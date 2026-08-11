#ifndef KYLE_APP_APPLICATION_H
#define KYLE_APP_APPLICATION_H

#include <cstddef>
#include <deque>
#include <functional>
#include <memory>
#include <mutex>

#include "core/chat_session.h"
#include "core/net_config.h"
#include "core/ota_check.h"
#include "core/ota_report.h"
#include "core/provisioning.h"
#include "hal/board.h"

namespace kyle {

// 业务编排骨架：输入事件 → ChatSession 状态机，消息/配置 → wire_format / NetConfig。
// 保持纯 C++，不依赖 ESP-IDF；真正的 NVS 存储由 app_main 注入 NvsStorage。
class Application {
public:
    Application(IBoard& board, IStorage& storage);
    ~Application() = default;

    void Initialize();
    void Run();
    // 主循环单次迭代：只派发待处理事件（host 测试用；真机走 Run() 的 while 循环）
    void RunOnce();

    // 跨任务/回调安全投递到主循环（入队）
    void Schedule(std::function<void()> fn);

    ChatSession& session() { return session_; }
    NetConfig& net_config() { return *net_config_; }

    // 上报 OTA 升级状态到后端 POST {ota_base}status（真机下载线程/终态调用；
    // host 测试直接调用验证契约）。state/progress 见 OtaReportState/0-100。
    void ReportOtaStatus(OtaReportState state, int progress);

private:
    void DispatchPendingEvents();
    void OnInputEvent(const InputEvent& ev);
    void ToggleSession();
    void AdjustVolume(int delta);
    void UpdateLedForSession();

    // ---- K5.5 网络引导：WiFi → OTA check → WS 建连 → hello 握手 ----
    void OnWifiStateChanged(WifiState state);
    void RunOtaCheck();
    void ConnectWebSocket();
    void OnWsConnected();                              // 通道建立 → 发 hello
    void OnWsText(const std::string& text);            // 服务端 hello → kChannelOpened
    DeviceInfo BuildDeviceInfo() const;

    // ---- OTA 升级状态上报（OtaUpdaterEsp 进度/终态回调）----
    void OnOtaDownloadProgress(std::size_t received, std::size_t total);
    void OnOtaDownloadDone(bool success, std::size_t received, std::size_t total);

    // ---- K5.6 SoftAP 配网 ----
    void EnterProvisioning();
    void OnProvisionSaved(const ProvisionResult& r);

    IBoard& board_;
    IStorage& storage_;
    std::unique_ptr<NetConfig> net_config_;
    ChatSession session_;
    std::deque<std::function<void()>> queue_;
    std::mutex queue_mutex_;  // Schedule 可能被 esp_event/WS task 调用，queue_ 需跨线程加锁
    std::unique_ptr<IWebSocket> ws_;  // 仅主循环访问（回调经 Schedule 回主循环）
    IProvisioningServer* provisioner_ = nullptr;  // 来自 board（无配网能力的板为 null）
    bool running_ = true;
    bool network_bootstrapped_ = false;  // 网络引导只做一次
    int volume_ = 70;  // 与 NoCodecI2s 默认音量一致
    OtaProgressThrottle ota_throttle_;  // 下载进度节流：每 10% 报一次后端
};

}  // namespace kyle

#endif  // KYLE_APP_APPLICATION_H
