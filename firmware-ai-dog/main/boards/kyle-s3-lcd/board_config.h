#ifndef KYLE_BOARDS_KYLE_S3_LCD_BOARD_CONFIG_H
#define KYLE_BOARDS_KYLE_S3_LCD_BOARD_CONFIG_H

// kyle-s3-lcd（ESP32-S3）静态引脚配置：纯数据，不依赖 ESP-IDF。
// 与旧 kyle-s3-lcd 板级 config.h 对齐。

#include <cstddef>

#include "boards/pin_map.h"

namespace kyle {
namespace kyle_s3_lcd {

// ---- 音频 I2S 直连（无外部 codec，单工）----
constexpr int kPinMicWs = 4;
constexpr int kPinMicSck = 5;
constexpr int kPinMicDin = 6;
constexpr int kPinSpkDout = 7;
constexpr int kPinSpkBclk = 15;
constexpr int kPinSpkLrck = 16;

// ---- 显示 ST7789 SPI LCD 240x240 ----
constexpr int kPinDisplayMosi = 47;
constexpr int kPinDisplayClk = 21;
constexpr int kPinDisplayDc = 40;
constexpr int kPinDisplayRst = 45;
constexpr int kPinDisplayCs = 41;
constexpr int kPinBacklight = 42;  // PWM 背光
constexpr int kDisplayWidth = 240;
constexpr int kDisplayHeight = 240;

// ---- 触摸 CST816S（I2C 0x38）----
constexpr int kPinTouchSda = 2;
constexpr int kPinTouchScl = 1;

// ---- 输入 / LED ----
constexpr int kPinBoot = 3;      // 单击=确认/菜单，双击=返回，长按=深睡
constexpr int kPinTouchUp = 9;   // 菜单导航/音量
constexpr int kPinTouchDown = 11;
constexpr int kPinLed = 48;      // 板载状态灯

// ---- 默认采样率 ----
constexpr int kDefaultInputRate = 16000;
constexpr int kDefaultOutputRate = 24000;

// 声明式引脚表（供 host 引脚冲突校验）
constexpr PinDef kBoardPins[] = {
    {"mic_ws", kPinMicWs, 1},
    {"mic_sck", kPinMicSck, 1},
    {"mic_din", kPinMicDin, 1},
    {"spk_dout", kPinSpkDout, 2},
    {"spk_bclk", kPinSpkBclk, 2},
    {"spk_lrck", kPinSpkLrck, 2},
    {"lcd_mosi", kPinDisplayMosi, 3},
    {"lcd_clk", kPinDisplayClk, 3},
    {"lcd_dc", kPinDisplayDc, 3},
    {"lcd_rst", kPinDisplayRst, 3},
    {"lcd_cs", kPinDisplayCs, 3},
    {"backlight", kPinBacklight, 4},
    {"touch_sda", kPinTouchSda, 5},
    {"touch_scl", kPinTouchScl, 5},
    {"boot", kPinBoot, 6},
    {"touch_up", kPinTouchUp, 6},
    {"touch_down", kPinTouchDown, 6},
    {"led", kPinLed, 7},
};
constexpr size_t kBoardPinCount = sizeof(kBoardPins) / sizeof(kBoardPins[0]);

}  // namespace kyle_s3_lcd
}  // namespace kyle

#endif  // KYLE_BOARDS_KYLE_S3_LCD_BOARD_CONFIG_H
