#ifndef KYLE_CORE_WS_PROTOCOL_H
#define KYLE_CORE_WS_PROTOCOL_H

// WebSocket 握手鉴权协议：设备身份 → 4 个握手 header。
// 纯逻辑，零 ESP-IDF 依赖，可在 host 单测（黄金向量锁定格式）。
// 对齐后端 WsHandshakeInterceptor：Device-Id / Client-Id / Protocol-Version / Authorization。

#include <string>
#include <utility>
#include <vector>

namespace kyle {

// WS 握手所需的设备身份信息（后端按 Device-Id 查设备并按 ws_token 校验）
struct WsAuthInfo {
    std::string device_id;    // 设备 MAC（可含冒号/横杠，组装时归一化）→ Device-Id
    std::string client_id;    // 客户端 ID → Client-Id
    int protocol_version = 3; // 协议版本（数字）→ Protocol-Version
    std::string ws_token;     // OTA check 下发的 token → Authorization: Bearer
};

// 归一化设备标识：小写、去掉冒号/横杠/空白（与后端 normalizeMac 同语义）
std::string NormalizeDeviceId(const std::string& mac);

// 组装 4 个握手 header（name → value），按后端 WsHandshakeInterceptor 校验顺序
std::vector<std::pair<std::string, std::string>> BuildWsHandshakeHeaders(const WsAuthInfo& auth);

}  // namespace kyle

#endif  // KYLE_CORE_WS_PROTOCOL_H
