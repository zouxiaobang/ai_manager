#include "drivers/gpio_backlight.h"
#include "unity.h"

using namespace kyle;

// PercentToDuty 是 constexpr 头文件内联实现，host 直接测，无需链接 IDF 侧 .cc。

TEST_CASE("backlight 0% maps to duty 0", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(0, GpioBacklight::PercentToDuty(0));
}

TEST_CASE("backlight 100% maps to duty 1023 (10bit)", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(1023, GpioBacklight::PercentToDuty(100));
}

TEST_CASE("backlight 50% maps to duty 511", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(511, GpioBacklight::PercentToDuty(50));
}

TEST_CASE("backlight 25% maps to duty 255 (integer division)", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(255, GpioBacklight::PercentToDuty(25));
}

TEST_CASE("backlight negative percent clamps to 0", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(0, GpioBacklight::PercentToDuty(-10));
}

TEST_CASE("backlight over-100 percent clamps to max", "[backlight]") {
    TEST_ASSERT_EQUAL_INT(1023, GpioBacklight::PercentToDuty(150));
}
