#include "core/ota_version.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("parse version string", "[ota]") {
    auto v = ParseVersion("2.0.1");
    TEST_ASSERT_EQUAL_SIZE_T(3, v.size());
    TEST_ASSERT_EQUAL_INT(2, v[0]);
    TEST_ASSERT_EQUAL_INT(0, v[1]);
    TEST_ASSERT_EQUAL_INT(1, v[2]);
}

TEST_CASE("version compare basic", "[ota]") {
    TEST_ASSERT_TRUE(IsNewVersionAvailable("2.0.0", "2.0.1"));
    TEST_ASSERT_FALSE(IsNewVersionAvailable("2.0.1", "2.0.0"));
    TEST_ASSERT_FALSE(IsNewVersionAvailable("2.0.0", "2.0.0"));
}

TEST_CASE("version compare longer segment wins on equal prefix", "[ota]") {
    TEST_ASSERT_TRUE(IsNewVersionAvailable("1.9", "1.9.1"));
    TEST_ASSERT_FALSE(IsNewVersionAvailable("1.9.1", "1.9"));
    TEST_ASSERT_TRUE(IsNewVersionAvailable("1", "2"));
    // 主版本大直接新（current=1.9.9, newer=2.0）
    TEST_ASSERT_TRUE(IsNewVersionAvailable("1.9.9", "2.0"));
    // 反过来则不是新的
    TEST_ASSERT_FALSE(IsNewVersionAvailable("2.0", "1.9.9"));
}

TEST_CASE("force flag overrides version comparison", "[ota]") {
    FirmwareInfo fw;
    fw.present = true;
    fw.version = "2.0.0";
    fw.url = "http://host/1.bin";
    fw.force = true;
    TEST_ASSERT_TRUE(ShouldUpgrade("2.0.0", fw));  // 同版本但 force → 升

    fw.force = false;
    TEST_ASSERT_FALSE(ShouldUpgrade("2.0.0", fw));

    // 无 firmware 段（absent）→ 不升级
    TEST_ASSERT_FALSE(ShouldUpgrade("2.0.0", FirmwareInfo{}));
}

TEST_CASE("parse ota config response", "[ota]") {
    const char* json =
        "{"
        "\"activation\":{},"
        "\"websocket\":{\"url\":\"wss://host/ws/device\",\"token\":\"tok123\",\"version\":3},"
        "\"mqtt\":{\"endpoint\":\"host:8883\",\"client_id\":\"c1\",\"username\":\"u\",\"password\":\"p\"},"
        "\"server_time\":{\"timestamp\":1710000000000,\"timezone_offset\":480},"
        "\"firmware\":{\"version\":\"2.2.1\",\"url\":\"http://host/api/iot/ota/download/12\",\"force\":false}"
        "}";
    auto resp = ParseOtaConfigResponse(json);
    TEST_ASSERT_TRUE(resp.websocket.present);
    TEST_ASSERT_EQUAL_STRING("wss://host/ws/device", resp.websocket.url.c_str());
    TEST_ASSERT_EQUAL_STRING("tok123", resp.websocket.token.c_str());
    TEST_ASSERT_EQUAL_INT(3, resp.websocket.version);

    TEST_ASSERT_TRUE(resp.mqtt.present);
    TEST_ASSERT_EQUAL_STRING("host:8883", resp.mqtt.endpoint.c_str());

    TEST_ASSERT_TRUE(resp.server_time.present);
    TEST_ASSERT_EQUAL_INT(480, resp.server_time.timezone_offset_min);

    TEST_ASSERT_TRUE(resp.firmware.present);
    TEST_ASSERT_EQUAL_STRING("2.2.1", resp.firmware.version.c_str());
    TEST_ASSERT_FALSE(resp.firmware.force);
}

TEST_CASE("parse ota config with force=1 number", "[ota]") {
    const char* json =
        "{\"firmware\":{\"version\":\"3.0.0\",\"url\":\"http://h/3.bin\",\"force\":1}}";
    auto resp = ParseOtaConfigResponse(json);
    TEST_ASSERT_TRUE(resp.firmware.present);
    TEST_ASSERT_TRUE(resp.firmware.force);
    TEST_ASSERT_TRUE(ShouldUpgrade("9.9.9", resp.firmware));  // force 即使版本更旧也升
}
