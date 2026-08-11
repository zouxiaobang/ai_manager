#include "core/wire_format.h"

#include <cstdio>
#include <cstring>

#include "core/json_mini.h"

namespace kyle {

namespace {

inline void WriteU16BE(uint8_t* p, uint16_t v) {
    p[0] = static_cast<uint8_t>(v >> 8);
    p[1] = static_cast<uint8_t>(v & 0xFF);
}

inline void WriteU32BE(uint8_t* p, uint32_t v) {
    p[0] = static_cast<uint8_t>(v >> 24);
    p[1] = static_cast<uint8_t>(v >> 16);
    p[2] = static_cast<uint8_t>(v >> 8);
    p[3] = static_cast<uint8_t>(v & 0xFF);
}

inline uint16_t ReadU16BE(const uint8_t* p) {
    return static_cast<uint16_t>((static_cast<uint16_t>(p[0]) << 8) | p[1]);
}

inline uint32_t ReadU32BE(const uint8_t* p) {
    return (static_cast<uint32_t>(p[0]) << 24) | (static_cast<uint32_t>(p[1]) << 16) |
           (static_cast<uint32_t>(p[2]) << 8) | static_cast<uint32_t>(p[3]);
}

// JSON 字符串值编码：仅转义必须转义的引号/反斜杠/控制符
std::string JsonEscape(const char* s) {
    if (s == nullptr) return "";
    std::string out;
    for (; *s; ++s) {
        char c = *s;
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x",
                                  static_cast<unsigned>(static_cast<unsigned char>(c)));
                    out += buf;
                } else {
                    out += c;
                }
        }
    }
    return out;
}

}  // namespace

std::vector<uint8_t> WireEncodeV1(const uint8_t* payload, size_t payload_size) {
    std::vector<uint8_t> buf(payload ? payload_size : 0);
    if (payload && payload_size) std::memcpy(buf.data(), payload, payload_size);
    return buf;
}

std::vector<uint8_t> WireEncodeV2(const uint8_t* payload, size_t payload_size,
                                  uint32_t timestamp, uint16_t type) {
    std::vector<uint8_t> buf(WireHeaderV2::kHeaderSize + payload_size);
    uint8_t* p = buf.data();
    WriteU16BE(p, 2);                          // version
    WriteU16BE(p + 2, type);                   // type
    WriteU32BE(p + 4, 0);                      // reserved
    WriteU32BE(p + 8, timestamp);              // timestamp
    WriteU32BE(p + 12, static_cast<uint32_t>(payload_size));
    if (payload && payload_size) std::memcpy(p + WireHeaderV2::kHeaderSize, payload, payload_size);
    return buf;
}

std::vector<uint8_t> WireEncodeV3(const uint8_t* payload, size_t payload_size, uint8_t type) {
    std::vector<uint8_t> buf(WireHeaderV3::kHeaderSize + payload_size);
    uint8_t* p = buf.data();
    p[0] = type;
    p[1] = 0;  // reserved
    WriteU16BE(p + 2, static_cast<uint16_t>(payload_size));
    if (payload && payload_size) std::memcpy(p + WireHeaderV3::kHeaderSize, payload, payload_size);
    return buf;
}

size_t WireDecodeV2(const uint8_t* data, size_t len, WireHeaderV2* out,
                    const uint8_t** out_payload) {
    if (data == nullptr || out == nullptr || len < WireHeaderV2::kHeaderSize) return 0;
    out->version = ReadU16BE(data);
    out->type = ReadU16BE(data + 2);
    out->reserved = ReadU32BE(data + 4);
    out->timestamp = ReadU32BE(data + 8);
    out->payload_size = ReadU32BE(data + 12);
    if (out->payload_size > len - WireHeaderV2::kHeaderSize) return 0;  // 声明长度越界
    if (out_payload) *out_payload = data + WireHeaderV2::kHeaderSize;
    return static_cast<size_t>(out->payload_size);
}

size_t WireDecodeV3(const uint8_t* data, size_t len, WireHeaderV3* out,
                    const uint8_t** out_payload) {
    if (data == nullptr || out == nullptr || len < WireHeaderV3::kHeaderSize) return 0;
    out->type = data[0];
    out->reserved = data[1];
    out->payload_size = ReadU16BE(data + 2);
    if (out->payload_size > len - WireHeaderV3::kHeaderSize) return 0;
    if (out_payload) *out_payload = data + WireHeaderV3::kHeaderSize;
    return static_cast<size_t>(out->payload_size);
}

std::string BuildHelloMessage(int version, const char* device_id, const char* fw_version,
                              const AudioParams& ap) {
    std::string s = "{\"type\":\"hello\",\"version\":";
    s += std::to_string(version);
    s += ",\"transport\":\"websocket\"";
    s += ",\"device_id\":\"" + JsonEscape(device_id) + "\"";
    s += ",\"fw_version\":\"" + JsonEscape(fw_version) + "\"";
    s += ",\"features\":{\"mcp\":true,\"aec\":false}";
    s += ",\"audio_params\":{\"format\":\"opus\",\"sample_rate\":" + std::to_string(ap.sample_rate);
    s += ",\"channels\":" + std::to_string(ap.channels);
    s += ",\"frame_duration\":" + std::to_string(ap.frame_duration) + "}}";
    return s;
}

std::string BuildServerHelloMessage(const char* session_id, const char* transport,
                                    const AudioParams& ap) {
    std::string s = "{\"type\":\"hello\",\"transport\":\"";
    s += transport ? transport : "websocket";
    s += "\",\"session_id\":\"" + JsonEscape(session_id) + "\"";
    s += ",\"audio_params\":{\"sample_rate\":" + std::to_string(ap.sample_rate);
    s += ",\"frame_duration\":" + std::to_string(ap.frame_duration) + "}}";
    return s;
}

std::string BuildListenMessage(const char* session_id, const char* state, const char* mode,
                               const char* text) {
    std::string s = "{\"session_id\":\"" + JsonEscape(session_id) + "\",\"type\":\"listen\"";
    s += ",\"state\":\"" + JsonEscape(state) + "\"";
    if (mode != nullptr) s += ",\"mode\":\"" + JsonEscape(mode) + "\"";
    if (text != nullptr) s += ",\"text\":\"" + JsonEscape(text) + "\"";
    s += "}";
    return s;
}

std::string BuildAbortMessage(const char* session_id, AbortReason reason) {
    std::string s = "{\"session_id\":\"" + JsonEscape(session_id) + "\",\"type\":\"abort\"";
    if (reason == kAbortWakeWordDetected) s += ",\"reason\":\"wake_word_detected\"";
    s += "}";
    return s;
}

std::string BuildSttMessage(const char* session_id, const char* text) {
    return "{\"session_id\":\"" + JsonEscape(session_id) + "\",\"type\":\"stt\",\"text\":\"" +
           JsonEscape(text) + "\"}";
}

std::string BuildTtsMessage(const char* session_id, const char* state, const char* text) {
    std::string s = "{\"session_id\":\"" + JsonEscape(session_id) + "\",\"type\":\"tts\"";
    s += ",\"state\":\"" + JsonEscape(state) + "\"";
    if (text != nullptr) s += ",\"text\":\"" + JsonEscape(text) + "\"";
    s += "}";
    return s;
}

std::string BuildLlmMessage(const char* session_id, const char* emotion, const char* text) {
    return "{\"session_id\":\"" + JsonEscape(session_id) + "\",\"type\":\"llm\",\"emotion\":\"" +
           JsonEscape(emotion) + "\",\"text\":\"" + JsonEscape(text) + "\"}";
}

ServerHelloInfo ParseServerHello(const char* json) {
    ServerHelloInfo info;
    json::Value root;
    if (!json::Parse(json, &root) || !root.IsObject()) return info;

    const json::Value* type = root.Get("type");
    // 兼容后端实际下发 "server_hello"（与协议文档 "hello" 两种命名并存）
    if (type == nullptr || !type->IsString()) return info;
    const std::string t = type->AsString();
    if (t != "hello" && t != "server_hello") return info;

    const json::Value* sid = root.Get("session_id");
    if (sid && sid->IsString()) info.session_id = sid->AsString();
    const json::Value* transport = root.Get("transport");
    if (transport && transport->IsString()) info.transport = transport->AsString();

    const json::Value* ap = root.Get("audio_params");
    if (ap && ap->IsObject()) {
        const json::Value* sr = ap->Get("sample_rate");
        if (sr && sr->IsNumber()) info.sample_rate = static_cast<int>(sr->AsNumber());
        const json::Value* fd = ap->Get("frame_duration");
        if (fd && fd->IsNumber()) info.frame_duration = static_cast<int>(fd->AsNumber());
    }

    info.ok = !info.session_id.empty();
    return info;
}

}  // namespace kyle
