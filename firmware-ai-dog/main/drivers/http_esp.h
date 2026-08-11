#ifndef KYLE_DRIVERS_HTTP_ESP_H
#define KYLE_DRIVERS_HTTP_ESP_H

#include <memory>
#include <string>

#include "hal/network.h"

namespace kyle {

// 真 HTTP 客户端：esp_http_client 实现 IHttp（OTA check / 激活用）。
// 惰性执行：Open/SetHeader/SetContent 只记录请求参数，首次 ReadAll() 才真正 perform。
// 头文件零 ESP-IDF 依赖（pimpl）：esp_http_client 句柄与事件回调都在 Impl(.cc) 内。
class HttpEsp : public IHttp {
public:
    HttpEsp();
    ~HttpEsp() override;

    bool Open(const std::string& method, const std::string& url) override;
    int status_code() const override;
    std::string ReadAll() override;
    void SetContent(const std::string& body) override;
    void SetHeader(const std::string& name, const std::string& value) override;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_HTTP_ESP_H
