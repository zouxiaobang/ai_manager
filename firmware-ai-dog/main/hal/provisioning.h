#ifndef KYLE_HAL_PROVISIONING_H
#define KYLE_HAL_PROVISIONING_H

// 配网 HTTP 服务能力抽象：提供配网页并接收 WiFi 凭据提交。
// 真机用 esp_http_server 实现（drivers/provisioning_esp）；mock 记录调用供 host 单测。
// 回调（on_saved）在 httpd task 上下文触发，实现内需自行保证线程安全（跨任务回主循环）。

#include <functional>
#include <string>

#include "core/provisioning.h"

namespace kyle {

class IProvisioningServer {
public:
    virtual ~IProvisioningServer() = default;

    // 启动配网服务（监听 AP 网关 IP:80）。ap_ssid 用于页面提示；
    // on_saved 在表单校验通过后回调（Application 写 NVS + 计划重启）。
    virtual bool Start(const std::string& ap_ssid,
                       std::function<void(const ProvisionResult&)> on_saved) = 0;
    virtual void Stop() = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_PROVISIONING_H
