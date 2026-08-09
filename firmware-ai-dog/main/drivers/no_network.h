#ifndef KYLE_DRIVERS_NO_NETWORK_H
#define KYLE_DRIVERS_NO_NETWORK_H

#include <memory>

#include "hal/network.h"

namespace kyle {

// 占位网络：不创建任何连接。TODO(driver) 接入 WiFi + WebSocket/HTTP 真实实现。
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
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_NETWORK_H
