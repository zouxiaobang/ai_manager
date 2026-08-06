#ifndef XIAOZHI_CORE_OTA_VERSION_H
#define XIAOZHI_CORE_OTA_VERSION_H

// 固件版本比较 + OTA 配置下发解析。纯逻辑，零 ESP-IDF 依赖。

#include <string>
#include <vector>

namespace xiaozhi {

// "1.2.3" → {1,2,3}
std::vector<int> ParseVersion(const std::string& version);

// new 是否比 current 新：逐段比较，前缀相同时段数多者新
bool IsNewVersionAvailable(const std::string& current, const std::string& newer);

struct FirmwareInfo {
    bool present = false;
    std::string version;
    std::string url;
    bool force = false;  // force=1 时强制升级
};

struct ServerTimeInfo {
    bool present = false;
    double timestamp_ms = 0;
    int timezone_offset_min = 0;
};

struct WebsocketConfigInfo {
    bool present = false;
    std::string url;
    std::string token;
    int version = 0;
};

struct MqttConfigInfo {
    bool present = false;
    std::string endpoint;
    std::string client_id;
    std::string username;
    std::string password;
};

// /api/iot/ota/check 响应解析结果（仅提取关心的字段）
struct OtaConfigResponse {
    WebsocketConfigInfo websocket;
    MqttConfigInfo mqtt;
    ServerTimeInfo server_time;
    FirmwareInfo firmware;
};

// 解析 OTA 检查响应 JSON（subset）
OtaConfigResponse ParseOtaConfigResponse(const char* json);

// 综合判断是否升级：force 优先，否则新版本更新才升
bool ShouldUpgrade(const std::string& current_version, const FirmwareInfo& fw);

}  // namespace xiaozhi

#endif  // XIAOZHI_CORE_OTA_VERSION_H
