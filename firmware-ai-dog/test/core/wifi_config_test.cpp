// WiFi 配网配置读取：NVS(wifi/ssid+wifi/password) 优先，Kconfig 兜底。
// 纯 host 测试，用 MockStorage 模拟 NVS。

#include "core/wifi_config.h"
#include "mocks/mock_storage.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("LoadWifiConfig prefers NVS over Kconfig fallback", "[wifi_config]") {
    MockStorage storage;
    storage.SetString("wifi", "ssid", "home_wifi");
    storage.SetString("wifi", "password", "secret123");
    WifiConfig cfg = LoadWifiConfig(storage);
    TEST_ASSERT_TRUE(cfg.configured());
    TEST_ASSERT_EQUAL_STRING("home_wifi", cfg.ssid.c_str());
    TEST_ASSERT_EQUAL_STRING("secret123", cfg.password.c_str());
}

TEST_CASE("LoadWifiConfig uses NVS ssid with empty password", "[wifi_config]") {
    MockStorage storage;
    storage.SetString("wifi", "ssid", "open_net");
    // password 缺省
    WifiConfig cfg = LoadWifiConfig(storage);
    TEST_ASSERT_TRUE(cfg.configured());
    TEST_ASSERT_EQUAL_STRING("open_net", cfg.ssid.c_str());
    TEST_ASSERT_TRUE(cfg.password.empty());
}

TEST_CASE("LoadWifiConfig falls back to Kconfig when NVS empty", "[wifi_config]") {
    MockStorage storage;
    // NVS 无配置，Kconfig 兜底（host 构建时经 CONFIG_WIFI_SSID/PASSWORD 注入）
    WifiConfig cfg = LoadWifiConfig(storage);
#ifdef CONFIG_WIFI_SSID
    TEST_ASSERT_TRUE(cfg.configured());
    TEST_ASSERT_EQUAL_STRING(CONFIG_WIFI_SSID, cfg.ssid.c_str());
#else
    TEST_ASSERT_FALSE(cfg.configured());
#endif
}
