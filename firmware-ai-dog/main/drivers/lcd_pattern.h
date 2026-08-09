#ifndef KYLE_DRIVERS_LCD_PATTERN_H
#define KYLE_DRIVERS_LCD_PATTERN_H

// 纯像素逻辑：零 ESP-IDF 依赖，host 可单测。
// 真机侧由 St7789Lcd 拿到 RGB565 缓冲后经 esp_lcd_panel_draw_bitmap 上屏。

#include <cstddef>  // size_t
#include <cstdint>

namespace kyle {
namespace lcd_pattern {

// RGB565 常用色
constexpr uint16_t kColorRed = 0xF800;
constexpr uint16_t kColorOrange = 0xFC00;
constexpr uint16_t kColorYellow = 0xFFE0;
constexpr uint16_t kColorGreen = 0x07E0;
constexpr uint16_t kColorCyan = 0x07FF;
constexpr uint16_t kColorBlue = 0x001F;
constexpr uint16_t kColorMagenta = 0xF81F;
constexpr uint16_t kColorWhite = 0xFFFF;
constexpr uint16_t kColorBlack = 0x0000;

// 把 (x,y,w,h) 区域填充成 color；越界部分裁剪（buf 是 buf_w×buf_h 的 RGB565 数组）。
void FillRect(uint16_t* buf, int buf_w, int buf_h, int x, int y, int w, int h,
              uint16_t color);

// 7 段彩色条：红/橙/黄/绿/青/蓝/紫，每段占满全高、宽 w/7，余数并入最后一段。
void RenderColorBars(uint16_t* buf, int w, int h);

// 黑白棋盘格，格边长 cell 像素。
void RenderCheckerboard(uint16_t* buf, int w, int h, int cell);

// 逐个像素交换高低字节：ST7789 RAMCTRL 默认大端，主机小端 uint16 缓冲上屏前必须换序。
// 注意 0xFFFF/0x0000 交换后不变（对称），彩色值会变。
void ByteSwap565(uint16_t* buf, size_t count);

}  // namespace lcd_pattern
}  // namespace kyle

#endif  // KYLE_DRIVERS_LCD_PATTERN_H
