// OTA 升级计划决策：版本比较 + URL 校验。纯 host 测试。

#include <string>

#include "core/ota_plan.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("IsValidFirmwareUrl accepts http and https", "[ota_plan]") {
    TEST_ASSERT_TRUE(IsValidFirmwareUrl("http://192.168.0.114:8080/fw/kyle.bin"));
    TEST_ASSERT_TRUE(IsValidFirmwareUrl("https://example.com/kyle/firmware.bin"));
}

TEST_CASE("IsValidFirmwareUrl rejects non-http schemes and empty", "[ota_plan]") {
    TEST_ASSERT_FALSE(IsValidFirmwareUrl(""));
    TEST_ASSERT_FALSE(IsValidFirmwareUrl("ftp://host/fw.bin"));
    TEST_ASSERT_FALSE(IsValidFirmwareUrl("/firmware.bin"));
    TEST_ASSERT_FALSE(IsValidFirmwareUrl("kyle.bin"));
}

TEST_CASE("No firmware info means no upgrade", "[ota_plan]") {
    FirmwareInfo fw;  // present = false
    OtaPlan plan = PlanOtaUpgrade("2.2.1", fw);
    TEST_ASSERT_FALSE(plan.upgrade);
    TEST_ASSERT_TRUE(plan.url.empty());
}

TEST_CASE("Invalid download url suppresses upgrade", "[ota_plan]") {
    FirmwareInfo fw;
    fw.present = true;
    fw.version = "3.0.0";
    fw.url = "ftp://bad/url.bin";
    OtaPlan plan = PlanOtaUpgrade("2.2.1", fw);
    TEST_ASSERT_FALSE(plan.upgrade);
}

TEST_CASE("Newer version triggers upgrade with url and version", "[ota_plan]") {
    FirmwareInfo fw;
    fw.present = true;
    fw.version = "3.0.0";
    fw.url = "https://example.com/kyle/firmware.bin";
    OtaPlan plan = PlanOtaUpgrade("2.2.1", fw);
    TEST_ASSERT_TRUE(plan.upgrade);
    TEST_ASSERT_EQUAL_STRING("3.0.0", plan.version.c_str());
    TEST_ASSERT_EQUAL_STRING("https://example.com/kyle/firmware.bin", plan.url.c_str());
}

TEST_CASE("Force flag only forces when version differs", "[ota_plan]") {
    FirmwareInfo fw;
    fw.present = true;
    fw.version = "2.2.1";  // 与当前相同
    fw.url = "https://example.com/fw.bin";
    fw.force = true;
    // 同版本 + force → 不升：否则后端每次 check 都下发同份固件，设备重启→下载→重启循环
    OtaPlan plan = PlanOtaUpgrade("2.2.1", fw);
    TEST_ASSERT_FALSE(plan.upgrade);

    // 版本更旧 → force 强制升（force 核心价值：越过版本比较下推修复）
    fw.version = "2.2.1";
    OtaPlan plan_old = PlanOtaUpgrade("2.2.2", fw);
    TEST_ASSERT_TRUE(plan_old.upgrade);

    // 版本更新（设备已领先）→ force 仍强制刷回，允许后端下推降级修复版
    OtaPlan plan_new = PlanOtaUpgrade("2.2.0", fw);
    TEST_ASSERT_TRUE(plan_new.upgrade);
}

TEST_CASE("Same or older version without force means no upgrade", "[ota_plan]") {
    FirmwareInfo fw;
    fw.present = true;
    fw.version = "2.2.0";  // 较旧
    fw.url = "https://example.com/fw.bin";
    OtaPlan plan = PlanOtaUpgrade("2.2.1", fw);
    TEST_ASSERT_FALSE(plan.upgrade);
}
