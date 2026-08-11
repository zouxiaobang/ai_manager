#ifndef KYLE_DRIVERS_WEBSOCKET_ESP_H
#define KYLE_DRIVERS_WEBSOCKET_ESP_H

#include <cstddef>
#include <cstdint>
#include <functional>
#include <memory>
#include <string>

#include "hal/network.h"

namespace kyle {

// 真 WebSocket 客户端：esp_websocket_client 实现 IWebSocket。
// 握手 header 在 Connect() 前经 SetHeader 收集，start 前通过 append_header 灌入
// （set_headers 仅连接后有效，首次握手必须走 append_header 进 config）。
// 事件回调在 WS 客户端 task 上下文触发，使用方需自行保证线程安全。
// 头文件零 ESP-IDF 依赖（pimpl）：esp_websocket_client 句柄与事件回调都在 Impl(.cc) 内。
class WebSocketEsp : public IWebSocket {
public:
    WebSocketEsp();
    ~WebSocketEsp() override;

    bool Connect(const std::string& url) override;
    void Close() override;
    bool IsConnected() const override;

    bool SendText(const std::string& text) override;
    bool SendBinary(const uint8_t* data, size_t len) override;

    void SetHeader(const std::string& name, const std::string& value) override;

    void OnText(std::function<void(const std::string&)> cb) override;
    void OnBinary(std::function<void(const uint8_t*, size_t)> cb) override;
    void OnDisconnected(std::function<void()> cb) override;
    void OnConnected(std::function<void()> cb) override;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_WEBSOCKET_ESP_H
