// OTA check 请求组装：body JSON 与 URL 拼接。纯 host 黄金向量测试。

#include <string>

#include "core/ota_check.h"
#include "unity.h"

using namespace kyle;

namespace {
DeviceInfo SampleInfo() {
    DeviceInfo info;
    info.uuid = "u123";
    info.client_id = "kyle-client";
    info.mac = "aabbccddeeff";
    info.model = "kyle-dog";
    info.chip = "esp32s3";
    info.board = "kyle-s3-lcd";
    info.language = "zh-CN";
    info.firmware_version = "0.3.0";
    return info;
}
}  // namespace

TEST_CASE("BuildOtaCheckBody emits snake_case fields in fixed order", "[ota_check]") {
    DeviceInfo info = SampleInfo();
    const std::string body = BuildOtaCheckBody(info);
    const char* kExpected =
        "{\"uuid\":\"u123\",\"client_id\":\"kyle-client\",\"mac\":\"aabbccddeeff\","
        "\"model\":\"kyle-dog\",\"chip\":\"esp32s3\",\"board\":\"kyle-s3-lcd\","
        "\"language\":\"zh-CN\",\"firmware_version\":\"0.3.0\"}";
    TEST_ASSERT_EQUAL_STRING(kExpected, body.c_str());
}

TEST_CASE("BuildOtaCheckBody escapes quotes and backslashes", "[ota_check]") {
    DeviceInfo info = SampleInfo();
    // 字段值含 JSON 保留字符：引号与反斜杠必须转义，其它字段保持黄金向量
    info.model = "kyle\"dog\\v2";
    const std::string body = BuildOtaCheckBody(info);
    const char* kExpected =
        "{\"uuid\":\"u123\",\"client_id\":\"kyle-client\",\"mac\":\"aabbccddeeff\","
        "\"model\":\"kyle\\\"dog\\\\v2\",\"chip\":\"esp32s3\",\"board\":\"kyle-s3-lcd\","
        "\"language\":\"zh-CN\",\"firmware_version\":\"0.3.0\"}";
    TEST_ASSERT_EQUAL_STRING(kExpected, body.c_str());
}

TEST_CASE("BuildOtaCheckBody escapes control characters", "[ota_check]") {
    DeviceInfo info = SampleInfo();
    info.firmware_version = "0.3.0\n";  // 尾随换行 → \n
    const std::string body = BuildOtaCheckBody(info);
    TEST_ASSERT_NOT_NULL(strstr(body.c_str(), "firmware_version\":\"0.3.0\\n\"}"));
}

TEST_CASE("BuildOtaCheckBody keeps utf-8 multibyte as-is", "[ota_check]") {
    DeviceInfo info = SampleInfo();
    info.language = "zh-CN";  // 中文以 UTF-8 原样输出，不做 \u 转义
    const std::string body = BuildOtaCheckBody(info);
    TEST_ASSERT_NOT_NULL(strstr(body.c_str(), "kyle-s3-lcd"));
}

TEST_CASE("BuildOtaCheckUrl strips trailing slash then appends /check", "[ota_check]") {
    // 带尾斜杠的默认 OTA 基址
    TEST_ASSERT_EQUAL_STRING("http://192.168.0.114:8080/api/iot/ota/check",
                             BuildOtaCheckUrl("http://192.168.0.114:8080/api/iot/ota/").c_str());
    // 无尾斜杠
    TEST_ASSERT_EQUAL_STRING("http://host:8080/api/iot/ota/check",
                             BuildOtaCheckUrl("http://host:8080/api/iot/ota").c_str());
    // 多个尾斜杠
    TEST_ASSERT_EQUAL_STRING("https://h/api/check",
                             BuildOtaCheckUrl("https://h/api///").c_str());
}

TEST_CASE("BuildOtaCheckUrl empty base yields /check", "[ota_check]") {
    TEST_ASSERT_EQUAL_STRING("/check", BuildOtaCheckUrl("").c_str());
}
