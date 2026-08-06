#include <cstring>

#include "core/wire_format.h"
#include "unity.h"

using namespace xiaozhi;

TEST_CASE("v2 header big-endian golden bytes", "[wire]") {
    const uint8_t payload[] = {0x01, 0x02, 0x03};
    auto buf = WireEncodeV2(payload, sizeof(payload), 0x12345678u, 0);
    TEST_ASSERT_EQUAL_SIZE_T(16 + 3, buf.size());
    const uint8_t expect[] = {
        0x00, 0x02,              // version = 2
        0x00, 0x00,              // type = OPUS
        0x00, 0x00, 0x00, 0x00,  // reserved
        0x12, 0x34, 0x56, 0x78,  // timestamp
        0x00, 0x00, 0x00, 0x03,  // payload_size
        0x01, 0x02, 0x03,        // payload
    };
    TEST_ASSERT_BYTES_EQUAL(expect, buf.data(), sizeof(expect));
}

TEST_CASE("v3 simplified header golden bytes", "[wire]") {
    const uint8_t payload[] = {0xDE, 0xAD};
    auto buf = WireEncodeV3(payload, sizeof(payload), 0);
    const uint8_t expect[] = {0x00, 0x00, 0x00, 0x02, 0xDE, 0xAD};
    TEST_ASSERT_BYTES_EQUAL(expect, buf.data(), sizeof(expect));
}

TEST_CASE("v1 raw passthrough", "[wire]") {
    const uint8_t payload[] = {0xAB, 0xCD};
    auto buf = WireEncodeV1(payload, sizeof(payload));
    TEST_ASSERT_EQUAL_SIZE_T(2, buf.size());
    TEST_ASSERT_BYTES_EQUAL(payload, buf.data(), sizeof(payload));
}

TEST_CASE("v2 decode round trip", "[wire]") {
    const uint8_t payload[] = {0x10, 0x20};
    auto buf = WireEncodeV2(payload, sizeof(payload), 0x0001ABCDu, 0);
    WireHeaderV2 hdr;
    const uint8_t* out_payload = nullptr;
    size_t plen = WireDecodeV2(buf.data(), buf.size(), &hdr, &out_payload);
    TEST_ASSERT_EQUAL_SIZE_T(2, plen);
    TEST_ASSERT_EQUAL_UINT16(2, hdr.version);
    TEST_ASSERT_EQUAL_UINT32(0x0001ABCDu, hdr.timestamp);
    TEST_ASSERT_BYTES_EQUAL(payload, out_payload, plen);
}

TEST_CASE("v3 decode round trip", "[wire]") {
    const uint8_t payload[] = {0x99};
    auto buf = WireEncodeV3(payload, sizeof(payload), 1);  // type = JSON
    WireHeaderV3 hdr;
    const uint8_t* out_payload = nullptr;
    size_t plen = WireDecodeV3(buf.data(), buf.size(), &hdr, &out_payload);
    TEST_ASSERT_EQUAL_SIZE_T(1, plen);
    TEST_ASSERT_EQUAL_UINT8(1, hdr.type);
    TEST_ASSERT_EQUAL_UINT16(1, hdr.payload_size);
    TEST_ASSERT_BYTES_EQUAL(payload, out_payload, plen);
}

TEST_CASE("v2 decode rejects malformed frame", "[wire]") {
    const uint8_t short_frame[] = {0x00, 0x02, 0x00};  // < 16B 头
    WireHeaderV2 hdr;
    const uint8_t* p = nullptr;
    TEST_ASSERT_EQUAL_SIZE_T(0, WireDecodeV2(short_frame, sizeof(short_frame), &hdr, &p));

    // 声明 payload 长度超出实际剩余 → 拒绝
    const uint8_t bad_len[] = {
        0x00, 0x02, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0xFF, 0xFF, 0xFF, 0xFF,  // payload_size = 0xFFFFFFFF
        0xAA, 0xBB,
    };
    TEST_ASSERT_EQUAL_SIZE_T(0, WireDecodeV2(bad_len, sizeof(bad_len), &hdr, &p));
}

TEST_CASE("hello message json", "[wire]") {
    AudioParams ap{16000, 1, 60};
    auto msg = BuildHelloMessage(2, "dev-1", "2.2.1", ap);
    TEST_ASSERT_NOT_NULL(std::strstr(msg.c_str(), "\"type\":\"hello\""));
    TEST_ASSERT_NOT_NULL(std::strstr(msg.c_str(), "\"sample_rate\":16000"));
    TEST_ASSERT_NOT_NULL(std::strstr(msg.c_str(), "\"frame_duration\":60"));
    TEST_ASSERT_NOT_NULL(std::strstr(msg.c_str(), "\"device_id\":\"dev-1\""));
}

TEST_CASE("listen/abort/stt/tts/llm json", "[wire]") {
    auto listen = BuildListenMessage("s1", "start", "auto", nullptr);
    TEST_ASSERT_NOT_NULL(std::strstr(listen.c_str(), "\"type\":\"listen\""));
    TEST_ASSERT_NOT_NULL(std::strstr(listen.c_str(), "\"state\":\"start\""));
    TEST_ASSERT_NOT_NULL(std::strstr(listen.c_str(), "\"mode\":\"auto\""));

    auto stop = BuildListenMessage("s1", "stop");
    TEST_ASSERT_NOT_NULL(std::strstr(stop.c_str(), "\"state\":\"stop\""));

    auto abort = BuildAbortMessage("s1", kAbortWakeWordDetected);
    TEST_ASSERT_NOT_NULL(std::strstr(abort.c_str(), "\"type\":\"abort\""));
    TEST_ASSERT_NOT_NULL(std::strstr(abort.c_str(), "\"reason\":\"wake_word_detected\""));

    auto stt = BuildSttMessage("s1", "你好");
    TEST_ASSERT_NOT_NULL(std::strstr(stt.c_str(), "\"type\":\"stt\""));
    TEST_ASSERT_NOT_NULL(std::strstr(stt.c_str(), "\"text\":\"你好\""));

    auto tts = BuildTtsMessage("s1", "start", "你好");
    TEST_ASSERT_NOT_NULL(std::strstr(tts.c_str(), "\"type\":\"tts\""));
    TEST_ASSERT_NOT_NULL(std::strstr(tts.c_str(), "\"state\":\"start\""));

    auto llm = BuildLlmMessage("s1", "happy", "嗨");
    TEST_ASSERT_NOT_NULL(std::strstr(llm.c_str(), "\"type\":\"llm\""));
    TEST_ASSERT_NOT_NULL(std::strstr(llm.c_str(), "\"emotion\":\"happy\""));
}

TEST_CASE("parse server hello extracts session_id and audio_params", "[wire]") {
    const char* json =
        "{\"type\":\"hello\",\"transport\":\"websocket\",\"session_id\":\"sess-abc\","
        "\"audio_params\":{\"sample_rate\":24000,\"frame_duration\":60}}";
    auto info = ParseServerHello(json);
    TEST_ASSERT_TRUE(info.ok);
    TEST_ASSERT_EQUAL_STRING("sess-abc", info.session_id.c_str());
    TEST_ASSERT_EQUAL_STRING("websocket", info.transport.c_str());
    TEST_ASSERT_EQUAL_INT(24000, info.sample_rate);
    TEST_ASSERT_EQUAL_INT(60, info.frame_duration);
}

TEST_CASE("parse server hello rejects non-hello message", "[wire]") {
    auto info = ParseServerHello("{\"type\":\"llm\",\"text\":\"x\"}");
    TEST_ASSERT_FALSE(info.ok);
    TEST_ASSERT_TRUE(info.session_id.empty());
}
