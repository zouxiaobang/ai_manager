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
    // 连接建立回调（TCP + TLS + WS upgrade 完成）。用于发送 hello 等连接后消息。
    virtual void OnConnected(std::function<void()> cb) = 0;
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

// WiFi 连接状态（NetworkEsp 维护，Application 订阅用于状态显示）
enum class WifiState {
    kDisconnected,  // 未连接 / 未配网
    kConnecting,    // STA 连接中
    kConnected,     // 已连接且拿到 IP
};

class INetwork {
public:
    virtual ~INetwork() = default;

    virtual std::unique_ptr<IWebSocket> CreateWebSocket(int id) = 0;
    virtual std::unique_ptr<IHttp> CreateHttp(int id) = 0;

    // 设备 MAC（小写冒号分隔，如 "aa:bb:cc:dd:ee:ff"）。
    // 用于 OTA check 上报与 WS 握手 Device-Id（后端按此识别/注册设备）。
    virtual std::string mac_address() const = 0;

    // 配网：注入凭据并启动连接。ssid 为空表示未配网，进入待配网状态。
    virtual void ConnectWifi(const std::string& ssid, const std::string& password) = 0;
    // 断开并停止连接（深睡前调用）。
    virtual void DisconnectWifi() = 0;
    // 当前连接状态。
    virtual WifiState wifi_state() const = 0;
    // 订阅连接状态变化（回调线程为网络事件上下文，实现内需自行保证线程安全）。
    virtual void OnWifiState(std::function<void(WifiState)> cb) = 0;

    // ---- K5.6 配网 SoftAP ----
    // 开启开放热点（无密码）。SSID 由 Application 用 BuildApSsid 生成。成功返回 true。
    virtual bool StartAp(const std::string& ssid) = 0;
    // 关闭热点（回到纯 STA 模式）。
    virtual void StopAp() = 0;
    // AP 网关 IP（配网页地址），如 "192.168.4.1"。
    virtual std::string ap_ip() const = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_NETWORK_H
