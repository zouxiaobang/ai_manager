#include <cstdint>

#include "drivers/cst816s_parse.h"
#include "hal/input.h"
#include "unity.h"

using namespace kyle;

// ---------- ParseCst816sPoint ----------

TEST_CASE("Point parse golden vector in range", "[cst816s_parse]") {
    // 触点 1，x=0x123，y=0x456（屏幕足够大不越界）
    const uint8_t raw[5] = {0x01, 0x01, 0x23, 0x04, 0x56};
    const TouchPoint tp = ParseCst816sPoint(raw, 4096, 4096);
    TEST_ASSERT_TRUE(tp.touched);
    TEST_ASSERT_EQUAL_INT(0x123, tp.x);
    TEST_ASSERT_EQUAL_INT(0x456, tp.y);
}

TEST_CASE("Point parse masks high nibble of high byte", "[cst816s_parse]") {
    // 高字节高半字节被掩掉：0xF5 → 0x05，0x0F → 0x0F
    const uint8_t raw[5] = {0x01, 0xF5, 0x10, 0x0F, 0x2A};
    const TouchPoint tp = ParseCst816sPoint(raw, 4096, 4096);
    TEST_ASSERT_EQUAL_INT((0x05 << 8) | 0x10, tp.x);
    TEST_ASSERT_EQUAL_INT((0x0F << 8) | 0x2A, tp.y);
}

TEST_CASE("Point parse no touch when count is zero", "[cst816s_parse]") {
    const uint8_t raw[5] = {0x00, 0xFF, 0xFF, 0xFF, 0xFF};
    const TouchPoint tp = ParseCst816sPoint(raw, 240, 240);
    TEST_ASSERT_FALSE(tp.touched);
    TEST_ASSERT_EQUAL_INT(0, tp.x);
    TEST_ASSERT_EQUAL_INT(0, tp.y);
}

TEST_CASE("Point parse multi-touch count still touched", "[cst816s_parse]") {
    // 触点数为 5（低 4 位即触点数量），仅取低 4 位
    const uint8_t raw[5] = {0x05, 0x00, 0x00, 0x00, 0x00};
    TEST_ASSERT_TRUE(ParseCst816sPoint(raw, 240, 240).touched);
}

TEST_CASE("Point parse clamps out-of-range coordinates", "[cst816s_parse]") {
    // 240x240 屏幕，坐标 0x0510=1296 / 0x0F2A=3882 都越界 → 钳到 239
    const uint8_t raw[5] = {0x01, 0x05, 0x10, 0x0F, 0x2A};
    const TouchPoint tp = ParseCst816sPoint(raw, 240, 240);
    TEST_ASSERT_TRUE(tp.touched);
    TEST_ASSERT_EQUAL_INT(239, tp.x);
    TEST_ASSERT_EQUAL_INT(239, tp.y);
}

TEST_CASE("Point parse zero-size screen yields no touch", "[cst816s_parse]") {
    const uint8_t raw[5] = {0x01, 0x00, 0x10, 0x00, 0x20};
    const TouchPoint tp = ParseCst816sPoint(raw, 0, 0);
    TEST_ASSERT_FALSE(tp.touched);
    TEST_ASSERT_EQUAL_INT(0, tp.x);
    TEST_ASSERT_EQUAL_INT(0, tp.y);
}

// ---------- TouchGestureDetector ----------
// 默认阈值：single_click=200ms、long_press=700ms、double_window=400ms。
// 以下用例直接推进虚拟时钟，事件在双击窗口到期（抬手 + 窗口）后结算。

TEST_CASE("Detector single click settles after double window", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    TEST_ASSERT_FALSE(d.Update(true, 1000, &ev));    // 按下沿
    TEST_ASSERT_FALSE(d.Update(true, 1040, &ev));    // 按住中
    TEST_ASSERT_FALSE(d.Update(false, 1060, &ev));   // 抬手（held 60ms <= 200 → 单击候选）
    TEST_ASSERT_FALSE(d.Update(false, 1429, &ev));   // 窗口 400ms 未到
    TEST_ASSERT_TRUE(d.Update(false, 1460, &ev));    // 1060+400 → 结算
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);
    TEST_ASSERT_EQUAL_INT(kTouchButtonId, ev.button_id);
}

TEST_CASE("Detector two taps in window become double click", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);   // 第一次抬手
    d.Update(true, 1200, &ev);    // 窗口内再按下
    d.Update(false, 1240, &ev);   // 第二次抬手
    TEST_ASSERT_FALSE(d.Update(false, 1639, &ev));
    TEST_ASSERT_TRUE(d.Update(false, 1640, &ev));    // 1240+400 → 结算
    TEST_ASSERT_EQUAL_INT(InputEvent::kDoubleClick, ev.type);
    TEST_ASSERT_EQUAL_INT(kTouchButtonId, ev.button_id);
}

TEST_CASE("Detector long press fires once on hold edge", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    TEST_ASSERT_TRUE(d.Update(true, 1700, &ev));     // 1000+700 → 长按沿
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, ev.type);
    TEST_ASSERT_FALSE(d.Update(true, 1750, &ev));    // 按住中不再重复触发
    TEST_ASSERT_FALSE(d.Update(false, 1800, &ev));   // 抬手，长按后不计单击
    TEST_ASSERT_FALSE(d.Update(false, 2200, &ev));   // 无残留单击
}

TEST_CASE("Detector hold too long then release emits nothing", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1300, &ev);   // held 300ms：> 200 单击阈值、< 700 长按阈值 → 不计数
    TEST_ASSERT_FALSE(d.Update(false, 1700, &ev));
    TEST_ASSERT_FALSE(d.Update(false, 2100, &ev));
}

TEST_CASE("Detector resets after click settles, next tap is separate click", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);
    TEST_ASSERT_FALSE(d.Update(false, 1429, &ev));
    TEST_ASSERT_TRUE(d.Update(false, 1440, &ev));    // 第一次单击
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);
    d.Update(true, 1600, &ev);                        // 结算后再按：全新单击
    d.Update(false, 1640, &ev);
    TEST_ASSERT_TRUE(d.Update(false, 2040, &ev));
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);
}

TEST_CASE("Detector three taps collapse to one double click", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);
    d.Update(true, 1200, &ev);
    d.Update(false, 1240, &ev);
    d.Update(true, 1400, &ev);
    d.Update(false, 1440, &ev);   // 窗口内第三次抬手 → click_count 3
    TEST_ASSERT_TRUE(d.Update(false, 1840, &ev));    // 结算，>=2 → 双击
    TEST_ASSERT_EQUAL_INT(InputEvent::kDoubleClick, ev.type);
}

TEST_CASE("Detector tap followed by long press emits only long press", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1040, &ev);                       // 第一次轻触（候选 1）
    d.Update(true, 1100, &ev);
    TEST_ASSERT_TRUE(d.Update(true, 1800, &ev));      // 1100+700 → 长按
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, ev.type);
    d.Update(false, 1850, &ev);
    TEST_ASSERT_FALSE(d.Update(false, 2300, &ev));    // 长按抬手后候选清零，无残留单击
}

TEST_CASE("Detector accepts null output and still advances", "[cst816s_parse]") {
    TouchGestureDetector d;
    TEST_ASSERT_FALSE(d.Update(true, 1000, nullptr));   // 不允许空事件，但状态机推进
    TEST_ASSERT_FALSE(d.Update(false, 1040, nullptr));
    TEST_ASSERT_TRUE(d.Update(false, 1440, nullptr));   // 事件丢弃，结算照常
}

TEST_CASE("Detector honors custom single click threshold", "[cst816s_parse]") {
    TouchGestureDetector d(TouchGestureDetector::Config{/*single_click=*/100, 700, 400});
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    d.Update(false, 1050, &ev);   // held 50ms <= 100 → 单击候选
    TEST_ASSERT_TRUE(d.Update(false, 1450, &ev));
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, ev.type);

    d.Update(true, 2000, &ev);
    d.Update(false, 2130, &ev);   // held 130ms > 100 → 不计入
    TEST_ASSERT_FALSE(d.Update(false, 2530, &ev));
}

TEST_CASE("Detector long press boundary is inclusive", "[cst816s_parse]") {
    TouchGestureDetector d;
    InputEvent ev{};
    d.Update(true, 1000, &ev);
    TEST_ASSERT_TRUE(d.Update(true, 1700, &ev));      // 恰好 700ms 触发长按
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, ev.type);
}
