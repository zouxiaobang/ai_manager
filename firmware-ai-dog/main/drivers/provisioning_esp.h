#ifndef KYLE_DRIVERS_PROVISIONING_ESP_H
#define KYLE_DRIVERS_PROVISIONING_ESP_H

#include <functional>
#include <memory>
#include <string>

#include "hal/provisioning.h"

namespace kyle {

// esp_http_server 真实现：监听 AP 网关 IP:80，提供配网页 + 表单提交。
// GET / → 配网页；POST /save → ParseProvisionForm → 校验通过回调 on_saved（Application 写 NVS + 重启）。
// 头文件零 ESP-IDF 依赖（pimpl），esp_http_server 只在 .cc 内使用。
class ProvisioningEsp : public IProvisioningServer {
public:
    ProvisioningEsp();
    ~ProvisioningEsp() override;

    bool Start(const std::string& ap_ssid,
               std::function<void(const ProvisionResult&)> on_saved) override;
    void Stop() override;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_PROVISIONING_ESP_H
