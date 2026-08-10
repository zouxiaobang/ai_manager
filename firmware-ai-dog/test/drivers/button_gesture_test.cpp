// 物理按键手势：TouchGestureDetector 参数化 button_id 后按键/触摸可共用时序逻辑。
// 默认阈值：single_click=200ms、long_press=700ms、double_window=400ms。

#include "drivers/cst816s_parse.h"
#include "hal/input.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("Button detector click carries its own button id", "[button_gesture]") {
    TouchGestureDetector d(TouchGestureDetector::Config{}, kButtonBoot);
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);
    TEST_ASSERT_TRUE(d.Update(false, 1440, &ev));  // 双击窗口到期结算
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);
    TEST_ASSERT_EQUAL_INT(kButtonBoot, ev.button_id);
}

TEST_CASE("Button detector double click carries its own button id", "[button_gesture]") {
    TouchGestureDetector d(TouchGestureDetector::Config{}, kButtonVolUp);
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);
    d.Update(true, 1200, &ev);
    d.Update(false, 1240, &ev);
    TEST_ASSERT_TRUE(d.Update(false, 1640, &ev));
    TEST_ASSERT_EQUAL_INT(InputEvent::kDoubleClick, ev.type);
    TEST_ASSERT_EQUAL_INT(kButtonVolUp, ev.button_id);
}

TEST_CASE("Button detector long press carries its own button id", "[button_gesture]") {
    TouchGestureDetector d(TouchGestureDetector::Config{}, kButtonVolDown);
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    TEST_ASSERT_TRUE(d.Update(true, 1700, &ev));  // 1000+700 → 长按沿
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, ev.type);
    TEST_ASSERT_EQUAL_INT(kButtonVolDown, ev.button_id);
}

TEST_CASE("Touch detector keeps kTouchButtonId by default", "[button_gesture]") {
    TouchGestureDetector d;  // 默认 button_id = kTouchButtonId
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);
    TEST_ASSERT_TRUE(d.Update(false, 1440, &ev));
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);
    TEST_ASSERT_EQUAL_INT(kTouchButtonId, ev.button_id);
}

TEST_CASE("Separate detectors do not share state", "[button_gesture]") {
    // 两个按键各自独立状态机：一个按住长按，不影响另一个的空闲状态
    TouchGestureDetector boot(TouchGestureDetector::Config{}, kButtonBoot);
    TouchGestureDetector vol(TouchGestureDetector::Config{}, kButtonVolUp);
    InputEvent ev{};
    boot.Update(true, 1000, &ev);
    boot.Update(true, 1700, &ev);  // BOOT 长按
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, ev.type);
    // 音量键从未按下，无事件
    TEST_ASSERT_FALSE(vol.Update(false, 1000, &ev));
    TEST_ASSERT_FALSE(vol.Update(false, 1700, &ev));
    TEST_ASSERT_FALSE(vol.Update(false, 2200, &ev));
}
