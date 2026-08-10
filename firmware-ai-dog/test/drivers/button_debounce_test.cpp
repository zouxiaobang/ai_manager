#include "drivers/gpio_button.h"
#include "unity.h"

using namespace kyle;

// ---------- DebounceFilter ----------

TEST_CASE("Debounce stays released on initial polls", "[button_debounce]") {
    DebounceFilter f(3);
    TEST_ASSERT_FALSE(f.stable());
    TEST_ASSERT_FALSE(f.Update(false));  // 稳定松开，无变化
    TEST_ASSERT_FALSE(f.stable());
}

TEST_CASE("Debounce needs N consecutive samples to confirm press", "[button_debounce]") {
    DebounceFilter f(3);
    TEST_ASSERT_FALSE(f.Update(true));   // 变化沿：进入确认，仍算松开
    TEST_ASSERT_FALSE(f.Update(true));   // 1
    TEST_ASSERT_FALSE(f.Update(true));   // 2
    TEST_ASSERT_TRUE(f.Update(true));    // 3 次一致 → 稳定按下
    TEST_ASSERT_TRUE(f.stable());
}

TEST_CASE("Debounce keeps pressed while held", "[button_debounce]") {
    DebounceFilter f(3);
    f.Update(true);
    f.Update(true);
    f.Update(true);
    f.Update(true);  // 稳定按下
    TEST_ASSERT_TRUE(f.Update(true));  // 持续按下，稳定保持
    TEST_ASSERT_TRUE(f.stable());
}

TEST_CASE("Debounce needs N consecutive samples to confirm release", "[button_debounce]") {
    DebounceFilter f(3);
    f.Update(true);
    f.Update(true);
    f.Update(true);
    f.Update(true);  // 稳定按下
    TEST_ASSERT_TRUE(f.Update(false));  // 变化沿：仍算按下（未确认松开）
    TEST_ASSERT_TRUE(f.Update(false));  // 1
    TEST_ASSERT_TRUE(f.Update(false));  // 2
    TEST_ASSERT_FALSE(f.Update(false));  // 3 次一致 → 稳定松开
    TEST_ASSERT_FALSE(f.stable());
}

TEST_CASE("Debounce rejects single-sample glitch", "[button_debounce]") {
    DebounceFilter f(3);
    f.Update(true);   // 开始确认按下
    f.Update(false);  // 反跳 → 取消确认，回到松开
    TEST_ASSERT_FALSE(f.stable());
    // 稳定松开后重新按下，仍走完整确认（变化沿 + 3 次一致）
    f.Update(true);   // 变化沿
    f.Update(true);   // 1
    f.Update(true);   // 2
    f.Update(true);   // 3 → 稳定
    TEST_ASSERT_TRUE(f.stable());
}

TEST_CASE("Debounce custom sample count", "[button_debounce]") {
    DebounceFilter f(5);
    TEST_ASSERT_FALSE(f.Update(true));   // 变化沿
    TEST_ASSERT_FALSE(f.Update(true));   // 1
    TEST_ASSERT_FALSE(f.Update(true));   // 2
    TEST_ASSERT_FALSE(f.Update(true));   // 3
    TEST_ASSERT_FALSE(f.Update(true));   // 4 次仍未确认
    TEST_ASSERT_TRUE(f.Update(true));    // 第 5 次一致 → 确认
    TEST_ASSERT_TRUE(f.stable());
}
