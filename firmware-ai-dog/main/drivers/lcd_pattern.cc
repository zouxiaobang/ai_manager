#include "drivers/lcd_pattern.h"

namespace kyle {
namespace lcd_pattern {

void FillRect(uint16_t* buf, int buf_w, int buf_h, int x, int y, int w, int h,
              uint16_t color) {
    if (buf == nullptr || buf_w <= 0 || buf_h <= 0 || w <= 0 || h <= 0) {
        return;
    }
    // 裁剪到缓冲区内，负起点裁为 0
    const int x0 = x < 0 ? 0 : x;
    const int y0 = y < 0 ? 0 : y;
    int x1 = x + w;
    int y1 = y + h;
    if (x1 > buf_w) x1 = buf_w;
    if (y1 > buf_h) y1 = buf_h;
    if (x0 >= x1 || y0 >= y1) {
        return;
    }
    for (int yy = y0; yy < y1; ++yy) {
        for (int xx = x0; xx < x1; ++xx) {
            buf[yy * buf_w + xx] = color;
        }
    }
}

void RenderColorBars(uint16_t* buf, int w, int h) {
    if (buf == nullptr || w <= 0 || h <= 0) {
        return;
    }
    static const uint16_t kBars[] = {
        kColorRed, kColorOrange, kColorYellow, kColorGreen,
        kColorCyan, kColorBlue, kColorMagenta,
    };
    const int n = static_cast<int>(sizeof(kBars) / sizeof(kBars[0]));
    const int seg = w / n;  // 每段基础宽度
    if (seg <= 0) {
        return;
    }
    for (int i = 0; i < n; ++i) {
        const int x = i * seg;
        const int sw = (i == n - 1) ? (w - x) : seg;  // 余数并入最后一段
        FillRect(buf, w, h, x, 0, sw, h, kBars[i]);
    }
}

void RenderCheckerboard(uint16_t* buf, int w, int h, int cell) {
    if (buf == nullptr || w <= 0 || h <= 0 || cell <= 0) {
        return;
    }
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            const bool black = ((x / cell) + (y / cell)) % 2 == 0;
            buf[y * w + x] = black ? kColorBlack : kColorWhite;
        }
    }
}

void ByteSwap565(uint16_t* buf, size_t count) {
    if (buf == nullptr) {
        return;
    }
    for (size_t i = 0; i < count; ++i) {
        const uint16_t c = buf[i];
        buf[i] = static_cast<uint16_t>((c >> 8) | (c << 8));
    }
}

}  // namespace lcd_pattern
}  // namespace kyle
