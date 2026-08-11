// SoftAP 配网纯逻辑单测：热点命名、URL 解码、表单解析、配网页生成。
// 纯 host，零 ESP-IDF 依赖（provisioning.cc 只依赖 <string>）。

#include <cstring>
#include <string>

#include "core/provisioning.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("BuildApSsid derives hotspot name from mac tail", "[provisioning]") {
    TEST_ASSERT_EQUAL_STRING("kyle-eeff", BuildApSsid("aa:bb:cc:dd:ee:ff").c_str());
    // 无冒号的紧凑 MAC 同样取后 4 位
    TEST_ASSERT_EQUAL_STRING("kyle-eeff", BuildApSsid("aabbccddeeff").c_str());
    // MAC 过短：用全部；全空：回退固定后缀避免只有前缀
    TEST_ASSERT_EQUAL_STRING("kyle-aabb", BuildApSsid("aa:bb").c_str());
    TEST_ASSERT_EQUAL_STRING("kyle-dog", BuildApSsid("").c_str());
}

TEST_CASE("UrlDecode handles plus, hex and plain text", "[provisioning]") {
    // + → 空格
    TEST_ASSERT_EQUAL_STRING("a b c", UrlDecode("a+b%20c").c_str());
    // %XX → 原字节（UTF-8 中文 SSID 场景：铃 = U+94C3 = 0xE9 0x93 0x83）
    TEST_ASSERT_EQUAL_STRING("\xE9\x93\x83", UrlDecode("%E9%93%83").c_str());
    // 非法转义原样保留
    TEST_ASSERT_EQUAL_STRING("%zz", UrlDecode("%zz").c_str());
    TEST_ASSERT_EQUAL_STRING("hello", UrlDecode("hello").c_str());
    // 大写十六进制也支持
    TEST_ASSERT_EQUAL_STRING("a b", UrlDecode("a%20b").c_str());
}

TEST_CASE("ParseProvisionForm extracts ssid and password", "[provisioning]") {
    ProvisionResult r = ParseProvisionForm("ssid=home&password=pw%20123");
    TEST_ASSERT_TRUE(r.ok);
    TEST_ASSERT_EQUAL_STRING("home", r.ssid.c_str());
    TEST_ASSERT_EQUAL_STRING("pw 123", r.password.c_str());
}

TEST_CASE("ParseProvisionForm decodes encoded ssid", "[provisioning]") {
    // %2B → '+', %E9%93%83 → UTF-8 中文
    ProvisionResult r = ParseProvisionForm("ssid=my%2Bwifi%20%5F&password=");
    TEST_ASSERT_TRUE(r.ok);
    TEST_ASSERT_EQUAL_STRING("my+wifi _", r.ssid.c_str());
    TEST_ASSERT_TRUE(r.password.empty());
}

TEST_CASE("ParseProvisionForm rejects empty or missing ssid", "[provisioning]") {
    TEST_ASSERT_FALSE(ParseProvisionForm("").ok);
    TEST_ASSERT_FALSE(ParseProvisionForm("password=only").ok);
    TEST_ASSERT_FALSE(ParseProvisionForm("ssid=").ok);
}

TEST_CASE("BuildProvisionPage embeds form and hotspot name", "[provisioning]") {
    std::string page = BuildProvisionPage("kyle-eeff");
    // 关键元素：热点名、表单 action/method、两个输入字段
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "kyle-eeff"));
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "action=\"/save\""));
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "method=\"post\""));
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "name=\"ssid\""));
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "name=\"password\""));
    // 无外网资源：不得引用 CDN/外部域名（配网时设备无法联网）
    TEST_ASSERT_NULL(strstr(page.c_str(), "http://"));
    TEST_ASSERT_NULL(strstr(page.c_str(), "https://"));
}

TEST_CASE("BuildProvisionPage embeds error message when provided", "[provisioning]") {
    std::string page = BuildProvisionPage("kyle-eeff", "请填写 WiFi 名称");
    TEST_ASSERT_NOT_NULL(strstr(page.c_str(), "请填写 WiFi 名称"));
}
