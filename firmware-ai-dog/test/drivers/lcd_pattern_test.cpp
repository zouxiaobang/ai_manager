#include <vector>

#include "drivers/lcd_pattern.h"
#include "unity.h"

using namespace kyle;

// ---------- FillRect ----------

TEST_CASE("FillRect fills exact region", "[lcd_pattern]") {
    std::vector<uint16_t> buf(8 * 8, 0);
    lcd_pattern::FillRect(buf.data(), 8, 8, 1, 2, 3, 4, lcd_pattern::kColorRed);
    for (int y = 0; y < 8; ++y) {
        for (int x = 0; x < 8; ++x) {
            const bool inside = (x >= 1 && x < 4) && (y >= 2 && y < 6);
            const uint16_t expect = inside ? lcd_pattern::kColorRed : 0;
            TEST_ASSERT_EQUAL_UINT16(expect, buf[y * 8 + x]);
        }
    }
}

TEST_CASE("FillRect clips out-of-range region", "[lcd_pattern]") {
    std::vector<uint16_t> buf(10 * 10, 0);
    // 越过右/下边界与负起点都要被裁剪
    lcd_pattern::FillRect(buf.data(), 10, 10, -2, -3, 100, 100, lcd_pattern::kColorWhite);
    for (int y = 0; y < 10; ++y) {
        for (int x = 0; x < 10; ++x) {
            TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorWhite, buf[y * 10 + x]);
        }
    }
}

TEST_CASE("FillRect zero-size is no-op", "[lcd_pattern]") {
    std::vector<uint16_t> buf(4 * 4, 0xABCD);
    lcd_pattern::FillRect(buf.data(), 4, 4, 0, 0, 0, 4, lcd_pattern::kColorRed);
    lcd_pattern::FillRect(buf.data(), 4, 4, 0, 0, 4, 0, lcd_pattern::kColorBlue);
    for (uint16_t v : buf) {
        TEST_ASSERT_EQUAL_UINT16(0xABCD, v);
    }
}

// ---------- RenderColorBars ----------

TEST_CASE("ColorBars renders rainbow segments", "[lcd_pattern]") {
    std::vector<uint16_t> buf(7 * 7, 0);
    lcd_pattern::RenderColorBars(buf.data(), 7, 7);
    // 7x7：每段宽 1，从红到紫精确匹配，且整列一致
    const uint16_t kRainbow[] = {
        lcd_pattern::kColorRed,    lcd_pattern::kColorOrange,
        lcd_pattern::kColorYellow, lcd_pattern::kColorGreen,
        lcd_pattern::kColorCyan,   lcd_pattern::kColorBlue,
        lcd_pattern::kColorMagenta,
    };
    for (int x = 0; x < 7; ++x) {
        for (int y = 0; y < 7; ++y) {
            TEST_ASSERT_EQUAL_UINT16(kRainbow[x], buf[y * 7 + x]);
        }
    }
}

TEST_CASE("ColorBars leftover pixels merge into last segment", "[lcd_pattern]") {
    // 10x1 宽不是 7 的倍数：前 6 段宽 1，末段宽 4，末段应全是紫
    std::vector<uint16_t> buf(10, 0);
    lcd_pattern::RenderColorBars(buf.data(), 10, 1);
    for (int x = 7; x < 10; ++x) {
        TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorMagenta, buf[x]);
    }
}

// ---------- RenderCheckerboard ----------

TEST_CASE("Checkerboard alternates black/white by cell", "[lcd_pattern]") {
    std::vector<uint16_t> buf(8 * 8, 0);
    lcd_pattern::RenderCheckerboard(buf.data(), 8, 8, 4);
    // (0,0) 黑，(4,0) 白，(0,4) 白，(4,4) 黑 —— 4px 格
    TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorBlack, buf[0]);
    TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorWhite, buf[4]);
    TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorWhite, buf[4 * 8]);
    TEST_ASSERT_EQUAL_UINT16(lcd_pattern::kColorBlack, buf[4 * 8 + 4]);
}

TEST_CASE("Checkerboard inside-cell is uniform", "[lcd_pattern]") {
    std::vector<uint16_t> buf(4 * 4, 0);
    lcd_pattern::RenderCheckerboard(buf.data(), 4, 4, 2);
    // 每个 2px 格内颜色一致
    for (int y = 0; y < 4; ++y) {
        for (int x = 0; x < 4; ++x) {
            const int cy = (y / 2) * 2;  // 所属格的左上角
            const int cx = (x / 2) * 2;
            const uint16_t ref = buf[cy * 4 + cx];
            TEST_ASSERT_EQUAL_UINT16(ref, buf[y * 4 + x]);
        }
    }
}

// ---------- ByteSwap565 ----------

TEST_CASE("ByteSwap565 swaps high/low bytes of each pixel", "[lcd_pattern]") {
    std::vector<uint16_t> buf = {0xF800, 0x001F, 0x07E0, 0xFFE0};
    const uint16_t kExpected[] = {0x00F8, 0x1F00, 0xE007, 0xE0FF};
    lcd_pattern::ByteSwap565(buf.data(), buf.size());
    for (size_t i = 0; i < buf.size(); ++i) {
        TEST_ASSERT_EQUAL_UINT16(kExpected[i], buf[i]);
    }
}

TEST_CASE("ByteSwap565 is an involution (self-inverse)", "[lcd_pattern]") {
    std::vector<uint16_t> original = {0xF800, 0x07FF, 0x1234, 0xABCD};
    std::vector<uint16_t> buf = original;
    lcd_pattern::ByteSwap565(buf.data(), buf.size());
    lcd_pattern::ByteSwap565(buf.data(), buf.size());
    for (size_t i = 0; i < buf.size(); ++i) {
        TEST_ASSERT_EQUAL_UINT16(original[i], buf[i]);
    }
}

TEST_CASE("ByteSwap565 leaves symmetric colors unchanged", "[lcd_pattern]") {
    // 仅当高字节 == 低字节时交换不变（如 0xFFFF/0x0000/0x3C3C/0xC3C3）
    std::vector<uint16_t> buf = {0xFFFF, 0x0000, 0x3C3C, 0xC3C3};
    const uint16_t kBefore[] = {0xFFFF, 0x0000, 0x3C3C, 0xC3C3};
    lcd_pattern::ByteSwap565(buf.data(), buf.size());
    for (size_t i = 0; i < buf.size(); ++i) {
        TEST_ASSERT_EQUAL_UINT16(kBefore[i], buf[i]);
    }
}
