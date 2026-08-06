#include <string>

#include "boards/kyle-s3-lcd/board_config.h"
#include "boards/pin_map.h"
#include "boards/supermini-c3/board_config.h"
#include "unity.h"

using namespace xiaozhi;

TEST_CASE("supermini-c3 pin map is conflict-free", "[board]") {
    std::string err;
    TEST_ASSERT_TRUE(
        ValidatePinMap(supermini_c3::kBoardPins, supermini_c3::kBoardPinCount, &err));
}

TEST_CASE("kyle-s3-lcd pin map is conflict-free", "[board]") {
    std::string err;
    TEST_ASSERT_TRUE(ValidatePinMap(kyle_s3_lcd::kBoardPins, kyle_s3_lcd::kBoardPinCount, &err));
}

TEST_CASE("cross-group pin reuse is detected", "[board]") {
    PinDef pins[] = {
        {"mic_sck", 6, 1},
        {"some_other", 6, 2},
    };
    std::string err;
    TEST_ASSERT_FALSE(ValidatePinMap(pins, 2, &err));
    TEST_ASSERT_FALSE(err.empty());
}

TEST_CASE("same-group pin sharing is legal (I2S 分时)", "[board]") {
    PinDef pins[] = {
        {"mic_sck", 6, 1},
        {"spk_bclk", 6, 1},
    };
    TEST_ASSERT_TRUE(ValidatePinMap(pins, 2, nullptr));
}

TEST_CASE("out-of-range pin is rejected", "[board]") {
    PinDef pins[] = {
        {"bad", 63, 1},
        {"good", 4, 2},
    };
    TEST_ASSERT_FALSE(ValidatePinMap(pins, 2, nullptr));
}

TEST_CASE("NC pin is skipped", "[board]") {
    PinDef pins[] = {
        {"unused", kPinNc, 1},
        {"boot", 2, 2},
    };
    TEST_ASSERT_TRUE(ValidatePinMap(pins, 2, nullptr));
}
