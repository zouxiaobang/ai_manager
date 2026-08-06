#include "core/net_config.h"
#include "core/ota_version.h"
#include "mocks/mock_storage.h"
#include "unity.h"

using namespace xiaozhi;

TEST_CASE("ota url falls back to compile-time default when storage empty", "[net]") {
    MockStorage store;
    NetConfig cfg(store);
    TEST_ASSERT_EQUAL_STRING(kDefaultOtaUrl, cfg.ota_url().c_str());
    TEST_ASSERT_FALSE(cfg.ota_url().empty());
}

TEST_CASE("ota url from storage wins over default", "[net]") {
    MockStorage store;
    store.SetString("wifi", "ota_url", "https://self.example/ota/");
    NetConfig cfg(store, "https://default.example/ota/");
    TEST_ASSERT_EQUAL_STRING("https://self.example/ota/", cfg.ota_url().c_str());
}

TEST_CASE("custom default used when storage empty", "[net]") {
    MockStorage store;
    NetConfig cfg(store, "https://my.ota/");
    TEST_ASSERT_EQUAL_STRING("https://my.ota/", cfg.ota_url().c_str());
}

TEST_CASE("websocket config reads url/token/version", "[net]") {
    MockStorage store;
    store.SetString("websocket", "url", "wss://s/ws");
    store.SetString("websocket", "token", "abc");
    store.SetInt("websocket", "version", 3);
    NetConfig cfg(store);
    auto ws = cfg.websocket();
    TEST_ASSERT_EQUAL_STRING("wss://s/ws", ws.url.c_str());
    TEST_ASSERT_EQUAL_STRING("abc", ws.token.c_str());
    TEST_ASSERT_EQUAL_INT(3, ws.version);
}

TEST_CASE("empty websocket config returns empty defaults", "[net]") {
    MockStorage store;
    NetConfig cfg(store);
    auto ws = cfg.websocket();
    TEST_ASSERT_TRUE(ws.url.empty());
    TEST_ASSERT_EQUAL_INT(0, ws.version);
}

TEST_CASE("apply ota response writes websocket config into storage", "[net]") {
    MockStorage store;
    NetConfig cfg(store);

    OtaConfigResponse resp;
    resp.websocket.present = true;
    resp.websocket.url = "wss://new/ws";
    resp.websocket.token = "newtok";
    resp.websocket.version = 2;
    resp.mqtt.present = true;
    resp.mqtt.endpoint = "host:8883";
    cfg.ApplyOtaResponse(resp);

    auto ws = cfg.websocket();
    TEST_ASSERT_EQUAL_STRING("wss://new/ws", ws.url.c_str());
    TEST_ASSERT_EQUAL_STRING("newtok", ws.token.c_str());
    TEST_ASSERT_EQUAL_INT(2, ws.version);
    TEST_ASSERT_EQUAL_STRING("host:8883", store.GetString("mqtt", "endpoint").c_str());
}
