#include "core/ws_protocol.h"

#include <cctype>
#include <string>

namespace kyle {

std::string NormalizeDeviceId(const std::string& mac) {
    std::string out;
    out.reserve(mac.size());
    for (char c : mac) {
        if (c == ':' || c == '-' || c == ' ' || c == '\t') {
            continue;  // 分隔符与空白丢弃
        }
        out += static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    }
    return out;
}

std::vector<std::pair<std::string, std::string>> BuildWsHandshakeHeaders(
    const WsAuthInfo& auth) {
    std::vector<std::pair<std::string, std::string>> headers;
    headers.reserve(4);
    headers.emplace_back("Device-Id", NormalizeDeviceId(auth.device_id));
    headers.emplace_back("Client-Id", auth.client_id);
    headers.emplace_back("Protocol-Version", std::to_string(auth.protocol_version));
    headers.emplace_back("Authorization", "Bearer " + auth.ws_token);
    return headers;
}

}  // namespace kyle
