#ifndef XIAOZHI_BOARDS_SUPERMINI_C3_BOARD_CONFIG_H
#define XIAOZHI_BOARDS_SUPERMINI_C3_BOARD_CONFIG_H

// supermini-c3（ESP32-C3）静态引脚配置：纯数据，不依赖 ESP-IDF。
// 与 G:\xiaozhi-esp32\main\boards\supermini-c3\config.h 对齐。

#include <cstddef>

#include "boards/pin_map.h"

namespace xiaozhi {
namespace supermini_c3 {

// ---- 音频 I2S 直连（无外部 codec）----
// 麦克风 INMP441：WS=5, SCK=6, DIN=4
// 功放 MAX98357A：DIN=7, BCLK=6, LRC=5（与 MIC 分时复用，同组共享合法）
constexpr int kPinMicWs = 5;
constexpr int kPinMicSck = 6;
constexpr int kPinMicDin = 4;
constexpr int kPinSpkDin = 7;
constexpr int kPinSpkBclk = 6;  // 与 kPinMicSck 共用（I2S 分时）
constexpr int kPinSpkLrc = 5;   // 与 kPinMicWs 共用（I2S 分时）

// ---- 显示 SSD1306 OLED 128x64（I2C 0x3C）----
constexpr int kPinOledSda = 8;
constexpr int kPinOledScl = 9;
constexpr int kDisplayWidth = 128;
constexpr int kDisplayHeight = 64;

// ---- 输入 / LED ----
constexpr int kPinBoot = 2;        // 单击切换对话，长按深睡
constexpr int kPinVolumeUp = 1;
constexpr int kPinVolumeDown = 3;
constexpr int kPinLed = 12;

// ---- 默认采样率 ----
constexpr int kDefaultInputRate = 16000;
constexpr int kDefaultOutputRate = 16000;

// 声明式引脚表（供 host 引脚冲突校验）；group 1 = I2S 音频总线
constexpr PinDef kBoardPins[] = {
    {"mic_ws", kPinMicWs, 1},
    {"mic_sck", kPinMicSck, 1},
    {"mic_din", kPinMicDin, 1},
    {"spk_din", kPinSpkDin, 1},
    {"spk_bclk", kPinSpkBclk, 1},
    {"spk_lrc", kPinSpkLrc, 1},
    {"oled_sda", kPinOledSda, 2},
    {"oled_scl", kPinOledScl, 2},
    {"boot", kPinBoot, 3},
    {"vol_up", kPinVolumeUp, 3},
    {"vol_down", kPinVolumeDown, 3},
    {"led", kPinLed, 4},
};
constexpr size_t kBoardPinCount = sizeof(kBoardPins) / sizeof(kBoardPins[0]);

}  // namespace supermini_c3
}  // namespace xiaozhi

#endif  // XIAOZHI_BOARDS_SUPERMINI_C3_BOARD_CONFIG_H
