#include "drivers/network_esp.h"

#include <cstdio>
#include <cstring>

#include "drivers/http_esp.h"
#include "drivers/websocket_esp.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_mac.h"
#include "esp_netif.h"
#include "esp_timer.h"
#include "esp_wifi.h"
#include "freertos/FreeRTOS.h"

namespace kyle {

namespace {
constexpr const char* kTag = "NetworkEsp";

// 常见断连 reason 的可读化（esp_wifi 断连事件只给数字 code，逐字查很费劲）
const char* ReasonName(int reason) {
    switch (reason) {
        case WIFI_REASON_AUTH_FAIL:
            return "AUTH_FAIL(密码错误/认证失败)";
        case WIFI_REASON_NO_AP_FOUND:
            return "NO_AP_FOUND(找不到该 SSID)";
        case WIFI_REASON_ASSOC_FAIL:
            return "ASSOC_FAIL(关联失败)";
        case WIFI_REASON_HANDSHAKE_TIMEOUT:
            return "HANDSHAKE_TIMEOUT(握手超时)";
        case WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT:
            return "4WAY_HANDSHAKE_TIMEOUT";
        default:
            return "其他";
    }
}
}  // namespace

// pimpl：隐藏 esp_wifi/esp_netif 类型，头文件保持零 IDF 依赖。
// esp_event / esp_timer 回调作为 Impl 静态成员，注册时 user_ctx 传 Impl*（impl_.get()）。
struct NetworkEsp::Impl {
    esp_netif_t* sta_netif = nullptr;
    esp_netif_t* ap_netif = nullptr;  // K5.6 配网热点
    esp_event_handler_instance_t wifi_any = nullptr;
    esp_event_handler_instance_t got_ip = nullptr;
    esp_timer_handle_t reconnect_timer = nullptr;
    std::string ssid;
    std::string password;
    std::string mac;  // 小写冒号分隔，构造时读 base MAC
    std::string ap_ip = "192.168.4.1";  // ESP AP 默认网关 IP（配网页地址）
    WifiStateMachine machine;
    std::function<void(WifiState)> on_state;
    bool started = false;
    bool ap_started = false;

    WifiState MapState(WifiLinkState s) const {
        switch (s) {
            case WifiLinkState::kConnected:
                return WifiState::kConnected;
            case WifiLinkState::kConnecting:
                return WifiState::kConnecting;
            default:
                return WifiState::kDisconnected;
        }
    }

    void Notify() {
        if (on_state) {
            on_state(MapState(machine.state()));
        }
    }

    // 一次性初始化 netif / event loop / wifi 驱动 + 重连定时器 + 事件注册。
    // STA/AP 与 mode 由调用方（ConnectWifi/StartAp）各自设置，这里不指定模式，
    // 保证先 StartAp（APSTA）再 ConnectWifi（STA）切换时不会重复初始化。
    esp_err_t EnsureInit() {
        if (started) {
            return ESP_OK;
        }
        esp_err_t err = esp_netif_init();
        if (err != ESP_OK) return err;
        // event loop 可能已被其他组件建过，允许 INVALID_STATE
        err = esp_event_loop_create_default();
        if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) return err;
        sta_netif = esp_netif_create_default_wifi_sta();
        wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
        err = esp_wifi_init(&cfg);
        if (err != ESP_OK) return err;

        // 重连定时器：单发，到点由 HandleReconnectTimer 重新 connect
        esp_timer_create_args_t timer_args = {};
        timer_args.callback = &Impl::HandleReconnectTimer;
        timer_args.arg = this;
        timer_args.name = "wifi-reconnect";
        err = esp_timer_create(&timer_args, &reconnect_timer);
        if (err != ESP_OK) return err;

        // 事件注册：WiFi 通用事件 + IP 事件（user_ctx 传 this）
        err = esp_event_handler_instance_register(
            WIFI_EVENT, ESP_EVENT_ANY_ID, &Impl::HandleWifiEvent, this, &wifi_any);
        if (err != ESP_OK) return err;
        err = esp_event_handler_instance_register(
            IP_EVENT, IP_EVENT_STA_GOT_IP, &Impl::HandleIpEvent, this, &got_ip);
        if (err != ESP_OK) return err;

        started = true;
        ESP_LOGI(kTag, "WiFi 驱动已初始化");
        return ESP_OK;
    }

    // ---- esp_event / esp_timer 静态回调（arg 即 Impl*，由注册时 user_ctx 提供）----
    static void HandleReconnectTimer(void* arg) {
        Impl* self = static_cast<Impl*>(arg);
        // 重连定时器到点：重新发起连接（仅当网络已启动且在配网模式）
        if (self->started && self->machine.state() == WifiLinkState::kConnecting) {
            esp_wifi_connect();
        }
    }

    static void HandleWifiEvent(void* arg, esp_event_base_t base, int32_t id, void* data) {
        Impl* self = static_cast<Impl*>(arg);
        switch (id) {
            case WIFI_EVENT_STA_START:
                if (self->ssid.empty()) {
                    // 未配网：驱动就绪但不发起连接，避免空配置反复连接失败刷屏
                    ESP_LOGI(kTag, "STA 已启动，等待配网信息");
                } else {
                    // 关联已开始，发起连接
                    ESP_LOGI(kTag, "STA 已启动，开始连接");
                    esp_wifi_connect();
                    self->machine.StartConnect();
                }
                break;
            case WIFI_EVENT_STA_CONNECTED:
                ESP_LOGI(kTag, "已关联 AP，等待获取 IP");
                self->machine.OnAssociated();
                break;
            case WIFI_EVENT_STA_DISCONNECTED: {
                auto* ev = static_cast<wifi_event_sta_disconnected_t*>(data);
                // reason 是诊断配网失败的关键（密码错/找不到 AP/超时）
                ESP_LOGW(kTag, "与 AP 断开: reason=%d(%s)", static_cast<int>(ev->reason),
                         ReasonName(static_cast<int>(ev->reason)));
                self->machine.OnDisconnected();
                if (self->machine.state() == WifiLinkState::kConnecting) {
                    // 还有重试机会，定时重连（间隔由状态机退避计算）
                    if (self->reconnect_timer) {
                        esp_timer_stop(self->reconnect_timer);
                        esp_timer_start_once(self->reconnect_timer,
                                             self->machine.retry_delay_ms() * 1000);
                    }
                }
                break;
            }
            default:
                break;
        }
        self->Notify();
        (void)base;
    }

    static void HandleIpEvent(void* arg, esp_event_base_t base, int32_t id, void* data) {
        Impl* self = static_cast<Impl*>(arg);
        if (id == IP_EVENT_STA_GOT_IP) {
            ip_event_got_ip_t* ev = static_cast<ip_event_got_ip_t*>(data);
            ESP_LOGI(kTag, "got ip: " IPSTR, IP2STR(&ev->ip_info.ip));
            self->machine.OnGotIp();
            if (self->reconnect_timer) {
                esp_timer_stop(self->reconnect_timer);
            }
            self->Notify();
        }
        (void)base;
    }
};

NetworkEsp::NetworkEsp() : impl_(std::make_unique<Impl>()) {
    // 读 base MAC（不依赖 netif/wifi 初始化，直接读 eFuse），格式 "aa:bb:cc:dd:ee:ff"
    uint8_t raw[6] = {0};
    if (esp_read_mac(raw, ESP_MAC_WIFI_STA) == ESP_OK) {
        char buf[18];
        std::snprintf(buf, sizeof(buf), "%02x:%02x:%02x:%02x:%02x:%02x", raw[0], raw[1],
                      raw[2], raw[3], raw[4], raw[5]);
        impl_->mac = buf;
    }
}

NetworkEsp::~NetworkEsp() {
    if (impl_->reconnect_timer) {
        esp_timer_stop(impl_->reconnect_timer);
        esp_timer_delete(impl_->reconnect_timer);
    }
    if (impl_->wifi_any) {
        esp_event_handler_instance_unregister(WIFI_EVENT, ESP_EVENT_ANY_ID,
                                              impl_->wifi_any);
    }
    if (impl_->got_ip) {
        esp_event_handler_instance_unregister(IP_EVENT, IP_EVENT_STA_GOT_IP,
                                              impl_->got_ip);
    }
}

std::unique_ptr<IWebSocket> NetworkEsp::CreateWebSocket(int id) {
    // K5.3：esp_websocket_client 真实现；id 当前无路由语义，预留
    (void)id;
    return std::make_unique<WebSocketEsp>();
}

std::unique_ptr<IHttp> NetworkEsp::CreateHttp(int id) {
    // K5.2：esp_http_client 真实现；id 当前无路由语义，预留
    (void)id;
    return std::make_unique<HttpEsp>();
}

void NetworkEsp::ConnectWifi(const std::string& ssid, const std::string& password) {
    Impl& i = *impl_;

    // 对齐老代码 wifi_manager_init：无论是否配网，boot 即初始化并拉起 WiFi 驱动（STA）。
    // 好处：未配网时驱动也在运行、日志可见（不再「设备静默不联网」让人误以为固件坏了）；
    // 配网信息就绪后（NVS/配网 UI）直接 connect。
    ESP_ERROR_CHECK(i.EnsureInit());

    // 配网（APSTA）后切回纯 STA；设置相同 mode 是 no-op，不会重启驱动
    esp_wifi_set_mode(WIFI_MODE_STA);

    if (ssid.empty()) {
        // 未配网：驱动已就绪但无凭据不连接，等待配网（配网 UI 后续步骤接入）。
        ESP_LOGW(kTag, "未配置 WiFi 凭据（NVS 与 CONFIG_WIFI_SSID 均为空），等待配网。"
                       "请用 menuconfig 设置 Kyle Firmware → Default WiFi SSID / Password");
        esp_err_t err = esp_wifi_start();
        if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
            ESP_ERROR_CHECK(err);
        }
        i.machine.Reset();
        i.Notify();
        return;
    }
    i.ssid = ssid;
    i.password = password;
    // 只打 ssid，不打密码（密码属敏感信息）
    ESP_LOGI(kTag, "连接 WiFi '%s'", ssid.c_str());

    wifi_config_t wifi_cfg = {};
    // ssid/password 是 std::string，超长时截断并终止（避免写越界）
    std::snprintf(reinterpret_cast<char*>(wifi_cfg.sta.ssid),
                  sizeof(wifi_cfg.sta.ssid), "%s", ssid.c_str());
    std::snprintf(reinterpret_cast<char*>(wifi_cfg.sta.password),
                  sizeof(wifi_cfg.sta.password), "%s", password.c_str());
    wifi_cfg.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;
    wifi_cfg.sta.sae_pwe_h2e = WPA3_SAE_PWE_BOTH;

    // STA 可能正处于连接中（配网 APSTA 场景沿用旧配置在线，或开机 STA_START 事件已自动 connect），
    // 此时 set_config 返回 ESP_ERR_WIFI_STATE「is connecting」。先主动断开，保证能写入新配置。
    // 对「未连接 / 未启动」场景断开是 no-op，返回对应错误码，均容忍。
    esp_err_t err = esp_wifi_disconnect();
    if (err != ESP_OK && err != ESP_ERR_WIFI_NOT_CONNECT &&
        err != ESP_ERR_WIFI_NOT_STARTED && err != ESP_ERR_WIFI_STATE) {
        ESP_ERROR_CHECK(err);
    }
    // 先写配置再启动：STA_START 事件触发自动连接时已用的是新配置，
    // 避免原「start 后再 set_config」顺序下先用 NVS 旧配置连上、set_config 报 is connecting 崩溃。
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_cfg));
    err = esp_wifi_start();
    if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
        ESP_ERROR_CHECK(err);
    }
    // STA_START 自动连接可能已发起，重复 connect 返回 ESP_ERR_WIFI_STATE，属正常竞态，忽略。
    err = esp_wifi_connect();
    if (err != ESP_OK && err != ESP_ERR_WIFI_STATE) {
        ESP_ERROR_CHECK(err);
    }

    i.machine.StartConnect();
    i.Notify();
}

bool NetworkEsp::StartAp(const std::string& ssid) {
    Impl& i = *impl_;
    if (i.EnsureInit() != ESP_OK) {
        ESP_LOGE(kTag, "WiFi 驱动初始化失败，无法开启热点");
        return false;
    }
    // AP 需要独立 netif（与 STA 共存）；首次开启才创建
    if (i.ap_netif == nullptr) {
        i.ap_netif = esp_netif_create_default_wifi_ap();
    }
    // APSTA 共存：配网时 STA 部分保持之前的状态（已有凭据则继续在线，未配网则只开 AP）
    esp_wifi_set_mode(WIFI_MODE_APSTA);

    wifi_config_t ap_cfg = {};
    std::snprintf(reinterpret_cast<char*>(ap_cfg.ap.ssid),
                  sizeof(ap_cfg.ap.ssid), "%s", ssid.c_str());
    // ssid_len 必须匹配实际写入长度（BuildApSsid 生成的 kyle-xxxx 固定 9 字节）
    ap_cfg.ap.ssid_len =
        static_cast<uint8_t>(std::strlen(reinterpret_cast<const char*>(ap_cfg.ap.ssid)));
    ap_cfg.ap.channel = 6;
    ap_cfg.ap.max_connection = 4;       // 配网一般一台手机连接
    ap_cfg.ap.authmode = WIFI_AUTH_OPEN;  // 开放热点：配网流程最简（凭据在页面上输入）
    if (esp_wifi_set_config(WIFI_IF_AP, &ap_cfg) != ESP_OK) {
        return false;
    }
    // 拉起（幂等）
    esp_err_t err = esp_wifi_start();
    if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
        return false;
    }
    i.ap_started = true;
    ESP_LOGI(kTag, "配网热点 %s 已开启（开放网络），配网页 http://%s/", ssid.c_str(),
             i.ap_ip.c_str());
    return true;
}

void NetworkEsp::StopAp() {
    Impl& i = *impl_;
    if (i.ap_started) {
        // APSTA → STA 会重启驱动关闭 AP；配网成功后走 esp_restart，此接口主要兜底
        esp_wifi_set_mode(WIFI_MODE_STA);
        i.ap_started = false;
    }
}

std::string NetworkEsp::ap_ip() const {
    return impl_->ap_ip;
}

std::string NetworkEsp::mac_address() const {
    return impl_->mac;
}

void NetworkEsp::DisconnectWifi() {
    Impl& i = *impl_;
    if (i.started) {
        esp_wifi_stop();
        esp_wifi_disconnect();
    }
    i.machine.Reset();
    i.Notify();
}

WifiState NetworkEsp::wifi_state() const {
    return impl_->MapState(impl_->machine.state());
}

void NetworkEsp::OnWifiState(std::function<void(WifiState)> cb) {
    impl_->on_state = std::move(cb);
}

}  // namespace kyle
