#ifndef KYLE_DRIVERS_NO_NETWORK_H
#define KYLE_DRIVERS_NO_NETWORK_H

#include <functional>
#include <memory>
#include <string>

#include "hal/network.h"

namespace kyle {

// 占位网络：不创建任何连接，也不实际配网。supermini-c3 等未接入真实网络的板子使用。
class NoNetwork : public INetwork {
public:
    NoNetwork() = default;
    ~NoNetwork() override = default;

    std::unique_ptr<IWebSocket> CreateWebSocket(int id) override {
        (void)id;
        return nullptr;
    }
    std::unique_ptr<IHttp> CreateHttp(int id) override {
        (void)id;
        return nullptr;
    }
    std::string mac_address() const override { return ""; }
    void ConnectWifi(const std::string& ssid, const std::string& password) override {
        (void)ssid;
        (void)password;
    }
    void DisconnectWifi() override {}
    WifiState wifi_state() const override { return WifiState::kDisconnected; }
    void OnWifiState(std::function<void(WifiState)> cb) override { (void)cb; }
    // 占位网络无 SoftAP 能力
    bool StartAp(const std::string& ssid) override {
        (void)ssid;
        return false;
    }
    void StopAp() override {}
    std::string ap_ip() const override { return ""; }
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_NETWORK_H
