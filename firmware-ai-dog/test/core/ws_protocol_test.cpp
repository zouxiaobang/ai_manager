// WS 握手鉴权协议：MAC 归一化 + 4 header 组装。纯 host 黄金向量测试。

#include <string>
#include <utility>
#include <vector>

#include "core/ws_protocol.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("NormalizeDeviceId strips separators and lowercases", "[ws_protocol]") {
    // 冒号分隔 MAC → 小写去冒号
    TEST_ASSERT_EQUAL_STRING("aabbccddeeff",
                             NormalizeDeviceId("AA:BB:CC:DD:EE:FF").c_str());
    // 横杠分隔
    TEST_ASSERT_EQUAL_STRING("aabbccddeeff",
                             NormalizeDeviceId("aa-bb-cc-dd-ee-ff").c_str());
    // 空白（后端 normalizeMac 的 trim）
    TEST_ASSERT_EQUAL_STRING("aabbccddeeff",
                             NormalizeDeviceId("  aa:bb:cc:dd:ee:ff ").c_str());
    // 已是归一化形式：原样
    TEST_ASSERT_EQUAL_STRING("aabbccddeeff",
                             NormalizeDeviceId("aabbccddeeff").c_str());
}

TEST_CASE("BuildWsHandshakeHeaders emits four headers in backend order", "[ws_protocol]") {
    WsAuthInfo auth;
    auth.device_id = "AA:BB:CC:DD:EE:FF";
    auth.client_id = "kyle-client";
    auth.protocol_version = 3;
    auth.ws_token = "tok_123";

    std::vector<std::pair<std::string, std::string>> headers = BuildWsHandshakeHeaders(auth);
    TEST_ASSERT_EQUAL_INT(4, static_cast<int>(headers.size()));

    // Device-Id：MAC 归一化
    TEST_ASSERT_EQUAL_STRING("Device-Id", headers[0].first.c_str());
    TEST_ASSERT_EQUAL_STRING("aabbccddeeff", headers[0].second.c_str());
    // Client-Id 原样
    TEST_ASSERT_EQUAL_STRING("Client-Id", headers[1].first.c_str());
    TEST_ASSERT_EQUAL_STRING("kyle-client", headers[1].second.c_str());
    // Protocol-Version：数字
    TEST_ASSERT_EQUAL_STRING("Protocol-Version", headers[2].first.c_str());
    TEST_ASSERT_EQUAL_STRING("3", headers[2].second.c_str());
    // Authorization：Bearer 前缀 + token
    TEST_ASSERT_EQUAL_STRING("Authorization", headers[3].first.c_str());
    TEST_ASSERT_EQUAL_STRING("Bearer tok_123", headers[3].second.c_str());
}

TEST_CASE("BuildWsHandshakeHeaders uses default version when unset", "[ws_protocol]") {
    WsAuthInfo auth;
    auth.device_id = "aabbccddeeff";
    auth.client_id = "c";
    auth.ws_token = "t";

    std::vector<std::pair<std::string, std::string>> headers = BuildWsHandshakeHeaders(auth);
    TEST_ASSERT_EQUAL_STRING("3", headers[2].second.c_str());
}

TEST_CASE("BuildWsHandshakeHeaders bearer prefix applies even for empty token", "[ws_protocol]") {
    WsAuthInfo auth;
    auth.device_id = "aabbccddeeff";
    auth.client_id = "c";
    auth.ws_token = "";

    std::vector<std::pair<std::string, std::string>> headers = BuildWsHandshakeHeaders(auth);
    // 与后端 WsHandshakeInterceptor 一致：token 为 "Bearer " 会被 trim 后判空拒绝；
    // 固件侧由上层保证 ws_token 非空，这里只锁定格式
    TEST_ASSERT_EQUAL_STRING("Bearer ", headers[3].second.c_str());
}
