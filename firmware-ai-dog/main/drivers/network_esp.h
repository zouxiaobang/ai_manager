#ifndef KYLE_DRIVERS_NETWORK_ESP_H
#define KYLE_DRIVERS_NETWORK_ESP_H

#include <functional>
#include <memory>
#include <string>

#include "core/wifi_state.h"
#include "hal/network.h"

namespace kyle {

// 真网络实现：esp_wifi + esp_netif（STA 模式）。
// WiFi 连接流程归 WifiStateMachine 纯逻辑驱动（重连/退避可 host 单测），
// 本类只负责把 esp_wifi 事件接到状态机，并把状态变化回调给订阅者。
// WebSocket/HTTP 能力（K5.2/K5.3）暂返回空实现，本步聚焦配网 + 连接。
// 头文件零 ESP-IDF 依赖：esp_event/esp_timer 回调都在 Impl(.cc) 内，经 user_ctx 恢复实例。
class NetworkEsp : public INetwork {
public:
    NetworkEsp();
    ~NetworkEsp() override;

    std::unique_ptr<IWebSocket> CreateWebSocket(int id) override;
    std::unique_ptr<IHttp> CreateHttp(int id) override;

    std::string mac_address() const override;

    void ConnectWifi(const std::string& ssid, const std::string& password) override;
    void DisconnectWifi() override;
    WifiState wifi_state() const override;
    void OnWifiState(std::function<void(WifiState)> cb) override;

    bool StartAp(const std::string& ssid) override;
    void StopAp() override;
    std::string ap_ip() const override;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NETWORK_ESP_H
