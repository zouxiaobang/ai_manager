#ifndef KYLE_CORE_NET_CONFIG_H
#define KYLE_CORE_NET_CONFIG_H

// NVS 读取抽象 + 服务器地址默认值回退策略。
// 通过注入的 IStorage 读写，core/ 本身不触碰 ESP-IDF NVS，可在 host 上单测。

#include <cstdint>
#include <string>

#include "core/ota_version.h"

namespace kyle {

// 可注入的键值存储（生产实现为 NVS，测试用内存 map）
class IStorage {
public:
    virtual ~IStorage() = default;

    virtual std::string GetString(const std::string& ns, const std::string& key,
                                  const std::string& def = "") = 0;
    virtual int GetInt(const std::string& ns, const std::string& key, int def = 0) = 0;
    virtual void SetString(const std::string& ns, const std::string& key,
                           const std::string& value) = 0;
    virtual void SetInt(const std::string& ns, const std::string& key, int value) = 0;
};

// 默认 OTA 基地址：编译期 CONFIG_OTA_URL 优先，否则内置kyle官方地址
extern const char* kDefaultOtaUrl;

struct WebsocketConfig {
    std::string url;
    std::string token;
    int version = 0;
};

class NetConfig {
public:
    explicit NetConfig(IStorage& storage, const std::string& default_ota_url = kDefaultOtaUrl);

    // wifi/ota_url 有值优先，否则回退默认
    std::string ota_url() const;

    // websocket/{url,token,version}
    WebsocketConfig websocket() const;

    // 把服务端 OTA 下发的配置写回存储（写穿透）
    void ApplyOtaResponse(const OtaConfigResponse& resp);

private:
    IStorage& storage_;
    std::string default_ota_url_;
};

}  // namespace kyle

#endif  // KYLE_CORE_NET_CONFIG_H
