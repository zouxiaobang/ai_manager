#ifndef XIAOZHI_CORE_WIRE_FORMAT_H
#define XIAOZHI_CORE_WIRE_FORMAT_H

// 二进制协议 v1/v2/v3 编解码 + 消息 JSON 组装辅助。
// 纯 C/C++，零 ESP-IDF 依赖，可在 PC host 上单测（黄金字节向量锁定格式）。

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace xiaozhi {

// 二进制帧 type 字段（v2 的 16bit / v3 的 8bit）
enum class WireType : uint8_t { kOpus = 0, kJson = 1 };

enum class ProtocolVersion : uint16_t { kV1 = 1, kV2 = 2, kV3 = 3 };

// v2 头：version(16) / type(16) / reserved(32) / timestamp(32) / payload_size(32)，全部大端
struct WireHeaderV2 {
    uint16_t version;
    uint16_t type;
    uint32_t reserved;
    uint32_t timestamp;  // 毫秒，用于服务端 AEC
    uint32_t payload_size;
    static constexpr size_t kHeaderSize = 16;
};

// v3 头：type(8) / reserved(8) / payload_size(16 大端)
struct WireHeaderV3 {
    uint8_t type;
    uint8_t reserved;
    uint16_t payload_size;
    static constexpr size_t kHeaderSize = 4;
};

// 会话打断原因（也是 abort 消息的 reason）
enum AbortReason {
    kAbortNone,
    kAbortWakeWordDetected,
    kAbortUser,
};

// ---- 编码 ----
std::vector<uint8_t> WireEncodeV1(const uint8_t* payload, size_t payload_size);
std::vector<uint8_t> WireEncodeV2(const uint8_t* payload, size_t payload_size,
                                  uint32_t timestamp, uint16_t type = 0);
std::vector<uint8_t> WireEncodeV3(const uint8_t* payload, size_t payload_size,
                                  uint8_t type = 0);

// ---- 解码 ----
// 返回实际 payload 长度；头不完整或声明长度越界返回 0。out_payload 指向 data 内的 payload 区。
size_t WireDecodeV2(const uint8_t* data, size_t len, WireHeaderV2* out,
                    const uint8_t** out_payload);
size_t WireDecodeV3(const uint8_t* data, size_t len, WireHeaderV3* out,
                    const uint8_t** out_payload);

// ---- 消息 JSON 组装 ----
struct AudioParams {
    int sample_rate;
    int channels;
    int frame_duration;
};

std::string BuildHelloMessage(int version, const char* device_id, const char* fw_version,
                              const AudioParams& ap);
std::string BuildServerHelloMessage(const char* session_id, const char* transport,
                                    const AudioParams& ap);
std::string BuildListenMessage(const char* session_id, const char* state,
                               const char* mode = nullptr, const char* text = nullptr);
std::string BuildAbortMessage(const char* session_id, AbortReason reason);
std::string BuildSttMessage(const char* session_id, const char* text);
std::string BuildTtsMessage(const char* session_id, const char* state, const char* text = nullptr);
std::string BuildLlmMessage(const char* session_id, const char* emotion, const char* text);

// ---- 服务端 hello 解析（提取 session_id / audio_params）----
struct ServerHelloInfo {
    bool ok = false;
    std::string session_id;
    std::string transport;
    int sample_rate = 0;
    int frame_duration = 0;
};
ServerHelloInfo ParseServerHello(const char* json);

}  // namespace xiaozhi

#endif  // XIAOZHI_CORE_WIRE_FORMAT_H
