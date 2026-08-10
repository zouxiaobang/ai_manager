#include "drivers/led_pattern.h"
#include "unity.h"

using namespace kyle;

// ---------- LedPatternForState ----------

TEST_CASE("Idle maps to off, session states differentiate", "[led_pattern]") {
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedPattern::kLedOff),
                          static_cast<int>(LedPatternForState(LedState::kLedIdle)));
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedPattern::kLedBlinkSlow),
                          static_cast<int>(LedPatternForState(LedState::kLedConnecting)));
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedPattern::kLedSolidOn),
                          static_cast<int>(LedPatternForState(LedState::kLedListening)));
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedPattern::kLedBlinkFast),
                          static_cast<int>(LedPatternForState(LedState::kLedSpeaking)));
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedPattern::kLedDoubleBlink),
                          static_cast<int>(LedPatternForState(LedState::kLedError)));
}

// ---------- ColorForState ----------

TEST_CASE("Idle is off, session states get distinct colors", "[led_pattern]") {
    const LedRgb idle = ColorForState(LedState::kLedIdle);
    TEST_ASSERT_EQUAL_UINT8(0, idle.r);
    TEST_ASSERT_EQUAL_UINT8(0, idle.g);
    TEST_ASSERT_EQUAL_UINT8(0, idle.b);
    // 连接=蓝、监听=红、说话=绿、错误=黄：各状态主色通道组合互不相同
    const LedRgb connecting = ColorForState(LedState::kLedConnecting);
    const LedRgb listening = ColorForState(LedState::kLedListening);
    const LedRgb speaking = ColorForState(LedState::kLedSpeaking);
    const LedRgb error = ColorForState(LedState::kLedError);
    TEST_ASSERT_EQUAL_UINT8(0, connecting.r);   // 蓝：g/r 通道为零
    TEST_ASSERT_TRUE(connecting.b > 0);
    TEST_ASSERT_TRUE(listening.r > 0);           // 红：g 为零
    TEST_ASSERT_EQUAL_UINT8(0, listening.g);
    TEST_ASSERT_TRUE(speaking.g > 0);            // 绿：r 为零
    TEST_ASSERT_EQUAL_UINT8(0, speaking.r);
    TEST_ASSERT_TRUE(error.r > 0);               // 黄：r+g 双通道
    TEST_ASSERT_TRUE(error.g > 0);
}

// ---------- ComputeLedLevel ----------

TEST_CASE("Off and solid are level-invariant", "[led_pattern]") {
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedOff, 0));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedOff, 1234567));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedSolidOn, 0));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedSolidOn, 999999));
}

TEST_CASE("Slow blink toggles every 500ms", "[led_pattern]") {
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkSlow, 0));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkSlow, 499));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBlinkSlow, 500));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBlinkSlow, 999));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkSlow, 1000));
}

TEST_CASE("Fast blink toggles every 100ms", "[led_pattern]") {
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkFast, 0));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkFast, 99));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBlinkFast, 100));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBlinkFast, 199));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBlinkFast, 200));
}

TEST_CASE("Double blink: on-off-on-off in 1.2s period", "[led_pattern]") {
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedDoubleBlink, 100));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedDoubleBlink, 350));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedDoubleBlink, 500));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedDoubleBlink, 800));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedDoubleBlink, 1300));  // 周期回绕
}

TEST_CASE("Breathing approximates 3s slow cycle", "[led_pattern]") {
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBreathing, 0));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBreathing, 1499));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBreathing, 1500));
    TEST_ASSERT_FALSE(ComputeLedLevel(LedPattern::kLedBreathing, 2999));
    TEST_ASSERT_TRUE(ComputeLedLevel(LedPattern::kLedBreathing, 3000));
}
