#include "core/ota_check.h"

namespace kyle {

namespace {
// 最小 JSON 字符串转义：引号/反斜杠/控制字符。
// 中文等多字节 UTF-8 原样输出（合法 JSON，后端 Jackson 直接解码）。
std::string EscapeJson(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    // 其余控制字符用 \uXXXX
                    const char* hex = "0123456789abcdef";
                    out += "\\u00";
                    out += hex[(c >> 4) & 0xF];
                    out += hex[c & 0xF];
                } else {
                    out += c;
                }
        }
    }
    return out;
}
}  // namespace

std::string BuildOtaCheckBody(const DeviceInfo& info) {
    // 字段顺序固定，snake_case 对齐后端 OtaCheckRequest（黄金向量锁定格式）
    return std::string("{")
        + "\"uuid\":\"" + EscapeJson(info.uuid) + "\","
        + "\"client_id\":\"" + EscapeJson(info.client_id) + "\","
        + "\"mac\":\"" + EscapeJson(info.mac) + "\","
        + "\"model\":\"" + EscapeJson(info.model) + "\","
        + "\"chip\":\"" + EscapeJson(info.chip) + "\","
        + "\"board\":\"" + EscapeJson(info.board) + "\","
        + "\"language\":\"" + EscapeJson(info.language) + "\","
        + "\"firmware_version\":\"" + EscapeJson(info.firmware_version) + "\""
        + "}";
}

std::string BuildOtaCheckUrl(const std::string& ota_base) {
    std::string base = ota_base;
    // 去掉尾斜杠，统一拼 "/check"
    while (!base.empty() && base.back() == '/') {
        base.pop_back();
    }
    return base + "/check";
}

}  // namespace kyle
