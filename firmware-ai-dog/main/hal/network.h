#ifndef KYLE_HAL_NETWORK_H
#define KYLE_HAL_NETWORK_H

#include <cstddef>
#include <cstdint>
#include <functional>
#include <memory>
#include <string>

namespace kyle {

// WebSocket 能力抽象（协议层只依赖这个接口，便于注入 mock 单测）
class IWebSocket {
public:
    virtual ~IWebSocket() = default;

    virtual bool Connect(const std::string& url) = 0;
    virtual void Close() = 0;
    virtual bool IsConnected() const = 0;

    virtual bool SendText(const std::string& text) = 0;
    virtual bool SendBinary(const uint8_t* data, size_t len) = 0;

    virtual void SetHeader(const std::string& name, const std::string& value) = 0;

    virtual void OnText(std::function<void(const std::string&)> cb) = 0;
    virtual void OnBinary(std::function<void(const uint8_t*, size_t)> cb) = 0;
    virtual void OnDisconnected(std::function<void()> cb) = 0;
};

// HTTP 能力抽象（OTA 激活/下载用）
class IHttp {
public:
    virtual ~IHttp() = default;

    virtual bool Open(const std::string& method, const std::string& url) = 0;
    virtual int status_code() const = 0;
    virtual std::string ReadAll() = 0;
    virtual void SetContent(const std::string& body) = 0;
    virtual void SetHeader(const std::string& name, const std::string& value) = 0;
};

class INetwork {
public:
    virtual ~INetwork() = default;

    virtual std::unique_ptr<IWebSocket> CreateWebSocket(int id) = 0;
    virtual std::unique_ptr<IHttp> CreateHttp(int id) = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_NETWORK_H
