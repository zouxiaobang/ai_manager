#include "app/application.h"

#include <cstdio>

#include "core/ota_plan.h"
#include "core/ws_protocol.h"
#include "core/wifi_config.h"
#include "core/wire_format.h"
#include "hal/input.h"

// Run() 需要让出主任务；host 单测环境不定义 ESP_PLATFORM，保持纯 C++。
#ifdef ESP_PLATFORM
#include "drivers/ota_esp.h"
#include "esp_log.h"
#include "esp_system.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

namespace kyle {

namespace {
// 编译期常量：语言 / 固件版本（Kconfig 提供；host 测试由 test/CMakeLists 定义同源宏）
const char* kLanguage() {
#ifdef CONFIG_LANGUAGE
    return CONFIG_LANGUAGE;
#else
    return "zh-CN";
#endif
}
const char* kFirmwareVersion() {
    // 版本号唯一来源 = IDF 官方 CONFIG_APP_PROJECT_VER（配 APP_PROJECT_VER_FROM_CONFIG=y），
    // 与 esp_app_desc.version（app_init 启动日志的 "App version"）同源——曾因
    // 自定义 CONFIG_FIRMWARE_VERSION 与 PROJECT_VER 两套版本号脱节，导致 OTA 上报版本
    // 永远落后于后端记录、无限重启循环。host 测试由 test/CMakeLists 定义同源宏。
#ifdef CONFIG_APP_PROJECT_VER
    return CONFIG_APP_PROJECT_VER;
#else
    return "";
#endif
}
}  // namespace

Application::Application(IBoard& board, IStorage& storage)
    : board_(board), storage_(storage) {}

void Application::Initialize() {
    net_config_ = std::make_unique<NetConfig>(storage_);

    // 启动即打印当前固件版本（与 app_init 的 "App version" 同源），
    // 便于真机核对 OTA check 上报的版本与后端记录是否一致
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "固件版本: %s", kFirmwareVersion());
#endif

    // 输入事件 → 会话状态机（触摸/按键统一走 OnInputEvent，按 button_id 分发）
    if (auto* input = board_.input()) {
        input->OnEvent([this](const InputEvent& ev) { OnInputEvent(ev); });
    }
    UpdateLedForSession();  // 初始空闲态

    // K5.5：订阅 WiFi 状态，首次连上后引导网络链路（OTA check → WS 建连）。
    // esp_event 回调在事件 task 上下文触发，跨任务经 Schedule 回主循环执行。
    if (auto* net = board_.network()) {
        net->OnWifiState([this](WifiState s) {
            Schedule([this, s]() { OnWifiStateChanged(s); });
        });
        // K5.1：配网信息（NVS wifi/ssid+password，Kconfig 兜底）注入网络层。
        // 已配网 → ConnectWifi（驱动 boot 拉起 + 凭据连接）；
        // 未配网 → K5.6 SoftAP 配网（StartAp 同样拉起驱动并开热点，不再静默空等）。
        WifiConfig wc = LoadWifiConfig(storage_);
        // 无条件持有配网服务：未配网时自动进配网，已配网时供长按 DOWN 重新配网
        provisioner_ = board_.provisioning();
        if (wc.configured()) {
            net->ConnectWifi(wc.ssid, wc.password);
        } else if (provisioner_ != nullptr) {
            EnterProvisioning();
        }
    }

    // TODO(firmware): 唤醒词触发 → session_.OnEvent(kWakeWordDetected) + 发 listen(detect)
    // TODO(firmware): 收到服务端 tts/llm → session_.OnEvent(kSpeakingStarted / kSpeakingStopped)
}

void Application::Schedule(std::function<void()> fn) {
    // 可从任意 task 调用（esp_event / WS 客户端 task），queue_ 需加锁
    std::lock_guard<std::mutex> lock(queue_mutex_);
    queue_.push_back(std::move(fn));
}

void Application::Run() {
    while (running_) {
        DispatchPendingEvents();
        // 让出主任务，避免 IDLE0 饿死触发任务看门狗（真机 task_wdt 已实测触发）。
        // K6 音频流水线就绪后改为事件驱动的阻塞等待，先定频轮询。
#ifdef ESP_PLATFORM
        vTaskDelay(pdMS_TO_TICKS(50));
#endif
    }
}

void Application::RunOnce() {
    DispatchPendingEvents();
}

void Application::DispatchPendingEvents() {
    // 加锁取出一个任务、出锁后执行：回调里再 Schedule 也不会死锁
    for (;;) {
        std::function<void()> fn;
        {
            std::lock_guard<std::mutex> lock(queue_mutex_);
            if (queue_.empty()) break;
            fn = std::move(queue_.front());
            queue_.pop_front();
        }
        if (fn) fn();
    }
}

void Application::OnInputEvent(const InputEvent& ev) {
    // 长按 BOOT：进入深睡（物理按键专属；触摸长按不深睡）。
    // 关断序列（LED→音频→背光→屏→深睡）归板级 EnterSleep() 单点实现，
    // 应用层不感知硬件顺序（顺序敏感，见各板实现注释）。
    if (ev.type == InputEvent::kLongPress && ev.button_id == kButtonBoot) {
        board_.EnterSleep();
        return;
    }
    if (ev.type == InputEvent::kLongPress && ev.button_id == kButtonVolDown) {
        // K5.6：长按 DOWN 重新进入配网（换 WiFi / 重新配网）
        EnterProvisioning();
        return;
    }
    if (ev.type != InputEvent::kClick) {
        return;  // 双击/其他长按：K4 暂不映射动作
    }
    switch (ev.button_id) {
        case kButtonBoot:     // BOOT 单击：对话开关
        case kTouchButtonId:  // 触摸单击：对话开关
            ToggleSession();
            break;
        case kButtonVolUp:    // UP：音量 +
            AdjustVolume(+10);
            break;
        case kButtonVolDown:  // DOWN：音量 -
            AdjustVolume(-10);
            break;
        default:
            break;
    }
}

void Application::ToggleSession() {
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "单击: 会话 %s", session_.state() == kIdle ? "开始" : "停止");
#endif
    if (session_.state() == kIdle) {
        session_.OnEvent(kStart);
        // TODO(firmware): 发 BuildListenMessage(session_id, "detect")，开启拾音
    } else {
        session_.OnEvent(kStopListening);
        // TODO(firmware): 发 BuildListenMessage(session_id, "stop")
    }
    UpdateLedForSession();
}

void Application::AdjustVolume(int delta) {
    int v = volume_ + delta;
    volume_ = (v < 0) ? 0 : (v > 100 ? 100 : v);
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "音量 %d%%", volume_);
#endif
    if (auto* a = board_.audio()) {
        a->SetOutputVolume(volume_);
    }
    // 音量变化给到屏显示（真机 St7789 的 ShowToast 当前为空实现，无副作用）
    if (auto* disp = board_.display()) {
        char buf[32];
        std::snprintf(buf, sizeof(buf), "音量 %d%%", volume_);
        disp->ShowToast(buf, 1500);
    }
}

void Application::UpdateLedForSession() {
    if (auto* led = board_.led()) {
        switch (session_.state()) {
            case kIdle:
                led->SetState(LedState::kLedIdle);
                break;
            case kConnecting:
                led->SetState(LedState::kLedConnecting);
                break;
            case kListening:
                led->SetState(LedState::kLedListening);
                break;
            case kSpeaking:
                led->SetState(LedState::kLedSpeaking);
                break;
        }
    }
}

// ---- K5.5 网络引导 ----

void Application::OnWifiStateChanged(WifiState state) {
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "WiFi 状态: %s", state == WifiState::kConnected
                                               ? "已连接"
                                               : state == WifiState::kConnecting ? "连接中"
                                                                                 : "未连接");
#endif
    // 首次拿到 IP 后引导一次；断线重连不重复引导（重连策略 K6 再引入）
    if (state == WifiState::kConnected && !network_bootstrapped_) {
        network_bootstrapped_ = true;
        RunOtaCheck();
    }
}

DeviceInfo Application::BuildDeviceInfo() const {
    DeviceInfo d;
    std::string mac = board_.network() ? board_.network()->mac_address() : "";
    // 后端按 MAC 识别/注册设备：client_id/mac 用归一化（小写去冒号）
    d.client_id = NormalizeDeviceId(mac);
    d.mac = d.client_id;
    d.model = board_.info().name ? board_.info().name : "";
    d.chip = board_.info().target ? board_.info().target : "";
    d.board = d.model;
    d.language = kLanguage();
    d.firmware_version = kFirmwareVersion();
    return d;
}

void Application::RunOtaCheck() {
    auto* net = board_.network();
    if (net == nullptr) return;

    auto http = net->CreateHttp(0);
    if (!http) return;

    // OTA check：上报设备信息，取回服务端配置（WS 地址/token/固件下发）
    std::string url = BuildOtaCheckUrl(net_config_->ota_url());
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "OTA check 开始: POST %s", url.c_str());
#endif
    http->SetHeader("Content-Type", "application/json");
    http->Open("POST", url);
    // 上报前打印设备 MAC + 固件版本：对照后端 OTA 记录里的版本号，
    // 若后端记录(如 2.2.3)与这里不一致，即版本循环的根因
#ifdef ESP_PLATFORM
    DeviceInfo di = BuildDeviceInfo();
    ESP_LOGI("Application", "OTA check 上报: client_id=%s firmware_version=%s", di.client_id.c_str(),
             di.firmware_version.c_str());
    http->SetContent(BuildOtaCheckBody(di));
#else
    http->SetContent(BuildOtaCheckBody(BuildDeviceInfo()));
#endif
    std::string body = http->ReadAll();
    const int status = http->status_code();
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "OTA check 响应: status=%d body=%zuB", status, body.size());
#endif
    if (status != 200) {
        // 后端不可达/未注册成功：本次跳过升级与建连（下次上电重试）
#ifdef ESP_PLATFORM
        ESP_LOGW("Application", "OTA check 未成功(status=%d)，跳过升级与建连，下次上电重试",
                 status);
#endif
        return;
    }

    OtaConfigResponse resp = ParseOtaConfigResponse(body.c_str());
    net_config_->ApplyOtaResponse(resp);  // WS 地址/token/版本写穿透

    OtaPlan plan = PlanOtaUpgrade(kFirmwareVersion(), resp.firmware);
    if (plan.upgrade) {
        // 需要升级：下载→校验→设启动分区→重启。OtaUpdaterEsp 依赖 esp_ota，仅真机可用；
        // host 测试只覆盖到"升级决策分支"（esp_restart 前调用不返回）。
        // 进度/终态经回调回主任务 → ReportOtaStatus 上报后端（下载线程即主任务，无跨线程竞态）。
#ifdef ESP_PLATFORM
        OtaUpdaterEsp updater;
        updater.UpgradeFromUrl(
            plan.url,
            [this](std::size_t received, std::size_t total) {
                OnOtaDownloadProgress(received, total);
            },
            [this](bool success, std::size_t received, std::size_t total) {
                OnOtaDownloadDone(success, received, total);
            });
#endif
        return;
    }
    ConnectWebSocket();
}

void Application::OnOtaDownloadProgress(std::size_t received, std::size_t total) {
    // OtaUpdaterEsp ON_DATA 回调：算百分比 + 节流，跨越里程碑（默认 10%）才 POST 后端，
    // 避免大固件高频上报打爆后端。进度上报失败不阻断下载（忽略响应）。
    int percent = ComputeOtaPercent(received, total);
    if (ota_throttle_.ShouldReport(percent, /*is_terminal=*/false)) {
        ReportOtaStatus(OtaReportState::kDownloading, percent);
    }
}

void Application::OnOtaDownloadDone(bool success, std::size_t received, std::size_t total) {
    // 终态：成功固定 100；失败上报实际下载到的百分比（便于后端区分"刚启动就挂"与"中途失败"）
    int percent = ComputeOtaPercent(received, total);
    ReportOtaStatus(success ? OtaReportState::kSuccess : OtaReportState::kFailed,
                    success ? 100 : percent);
}

void Application::ReportOtaStatus(OtaReportState state, int progress) {
    auto* net = board_.network();
    if (net == nullptr) return;
    auto http = net->CreateHttp(0);
    if (!http) return;

    // 契约：POST {ota_base}status，snake_case body，mac 归一化（与 OTA check/WS 握手同源）
    std::string url = BuildOtaStatusUrl(net_config_->ota_url());
    std::string mac = NormalizeDeviceId(net->mac_address());
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "OTA 上报: POST %s state=%d progress=%d", url.c_str(),
             static_cast<int>(state), progress);
#endif
    http->SetHeader("Content-Type", "application/json");
    http->Open("POST", url);
    http->SetContent(BuildOtaStatusBody(mac, state, progress));
    http->ReadAll();  // 响应仅留日志；上报失败不阻断升级主流程
}

void Application::ConnectWebSocket() {
    auto* net = board_.network();
    if (net == nullptr) return;

    WebsocketConfig cfg = net_config_->websocket();
    if (cfg.url.empty()) {
#ifdef ESP_PLATFORM
        ESP_LOGW("Application", "服务端未下发 WS 地址，跳过建连");
#endif
        return;
    }
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "WS 建连: %s", cfg.url.c_str());
#endif

    auto ws = net->CreateWebSocket(0);
    if (!ws) return;
    ws_ = std::move(ws);

    // 握手鉴权 4 header（对齐后端 WsHandshakeInterceptor：Device-Id/Client-Id/Protocol-Version/Authorization）
    WsAuthInfo auth;
    auth.device_id = net->mac_address();
    auth.client_id = NormalizeDeviceId(net->mac_address());
    auth.protocol_version = cfg.version > 0 ? cfg.version : 3;
    auth.ws_token = cfg.token;
    for (const auto& h : BuildWsHandshakeHeaders(auth)) {
        ws_->SetHeader(h.first, h.second);
    }

    // WS 事件在客户端 task 上下文触发，统一经 Schedule 回主循环执行
    ws_->OnConnected([this]() { Schedule([this]() { OnWsConnected(); }); });
    ws_->OnText([this](const std::string& t) { Schedule([this, t]() { OnWsText(t); }); });
    ws_->OnDisconnected([this]() {
        Schedule([this]() {
            session_.OnEvent(kDisconnected);  // 掉线 → 会话复位
            UpdateLedForSession();
        });
    });

    // 建连即进入会话（connecting），server hello 后通道就绪（listening）。
    // 注意：K5 里通道一开即自动进入监听，K6 唤醒词接入后 kStart 改由唤醒/按键驱动。
    session_.OnEvent(kStart);
    UpdateLedForSession();
    if (!ws_->Connect(cfg.url)) {
#ifdef ESP_PLATFORM
        ESP_LOGW("Application", "WS 建连失败: %s", cfg.url.c_str());
#endif
        session_.OnEvent(kError);  // 建连失败 → 复位
        UpdateLedForSession();
        ws_.reset();
    }
}

void Application::OnWsConnected() {
    // 通道建立（TCP+TLS+WS upgrade）：发送 hello，等待服务端 server hello（session_id+audio_params）
    AudioParams ap;
    ap.sample_rate = board_.info().default_input_rate;
    ap.channels = 1;
    ap.frame_duration = 16;  // 与后端 serverHello(16000, 1, 16) 对齐
    std::string device_id =
        NormalizeDeviceId(board_.network() ? board_.network()->mac_address() : "");
    std::string hello = BuildHelloMessage(3, device_id.c_str(), kFirmwareVersion(), ap);
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "WS 已连接，发送 hello (%zuB)", hello.size());
#endif
    bool sent = ws_->SendText(hello);
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "hello 发送%s", sent ? "成功" : "失败");
#endif
}

void Application::OnWsText(const std::string& text) {
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "收到服务端文本 %zuB", text.size());
#endif
    ServerHelloInfo info = ParseServerHello(text.c_str());
    if (info.ok) {
        session_.OnEvent(kChannelOpened);  // connecting → listening（通道就绪）
        UpdateLedForSession();
#ifdef ESP_PLATFORM
        ESP_LOGI("Application", "server hello: session=%s 通道就绪", info.session_id.c_str());
#endif
    }
    // 非 hello 文本（stt/llm/tts 下发）留到 K6 音频流水线接入时解析
}

// ---- K5.6 SoftAP 配网 ----

void Application::EnterProvisioning() {
    auto* net = board_.network();
    if (net == nullptr || provisioner_ == nullptr) {
        return;  // 无网络或无配网服务（如 supermini），跳过
    }
    // 热点 SSID 由 MAC 后 4 位派生：kyle-xxxx（开放网络，无密码）
    std::string ssid = BuildApSsid(net->mac_address());
    if (!net->StartAp(ssid)) {
#ifdef ESP_PLATFORM
        ESP_LOGE("Application", "热点 %s 开启失败，无法进入配网", ssid.c_str());
#endif
        return;
    }
    // on_saved 在 httpd task 上下文触发，经 Schedule 回主循环执行（与 WS 回调同一模式）
    provisioner_->Start(ssid, [this](const ProvisionResult& r) {
        Schedule([this, r]() { OnProvisionSaved(r); });
    });
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "进入配网模式: 连接热点 %s 后打开 http://%s/", ssid.c_str(),
             net->ap_ip().c_str());
#endif
    if (auto* disp = board_.display()) {
        std::string msg = "配网: 连 " + ssid;
        disp->ShowToast(msg.c_str(), 3000);
    }
}

void Application::OnProvisionSaved(const ProvisionResult& r) {
    if (!r.ok || r.ssid.empty()) {
        return;  // 表单无效（页面已提示错误），忽略
    }
    // 凭据写入 NVS（重启后 LoadWifiConfig 走 STA 连接，configured()==true 不再进配网）
    storage_.SetString("wifi", "ssid", r.ssid);
    storage_.SetString("wifi", "password", r.password);
#ifdef ESP_PLATFORM
    ESP_LOGI("Application", "配网成功: %s，2s 后重启连接", r.ssid.c_str());
    // 延迟重启：让 httpd 先把成功页响应发给手机，否则手机看不到「配置成功」提示
    esp_timer_create_args_t args = {};
    args.callback = [](void*) { esp_restart(); };
    args.name = "provision-reboot";
    esp_timer_handle_t timer = nullptr;
    if (esp_timer_create(&args, &timer) == ESP_OK) {
        esp_timer_start_once(timer, 2 * 1000 * 1000);
    } else {
        esp_restart();
    }
#endif
}

}  // namespace kyle
