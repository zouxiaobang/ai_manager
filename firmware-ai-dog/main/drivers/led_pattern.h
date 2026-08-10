#ifndef KYLE_DRIVERS_LED_PATTERN_H
#define KYLE_DRIVERS_LED_PATTERN_H

#include <cstdint>

#include "hal/led.h"

namespace kyle {

// 指示灯图案（纯逻辑，host 可测）：设备状态 → (颜色, 图案)，再按单调时钟算电平。
// 单色 GPIO 灯只用图案的亮/灭；WS2812 RGB 灯珠（kyle-s3-lcd）叠加颜色层。
enum class LedPattern {
    kLedOff,          // 熄灭
    kLedSolidOn,      // 常亮
    kLedBlinkSlow,    // 慢闪（1s 周期，50% 占空）
    kLedBlinkFast,    // 快闪（200ms 周期，50% 占空）
    kLedDoubleBlink,  // 双闪（1.2s 周期：亮300/灭100/亮300/灭500）
    kLedBreathing,    // 呼吸（3s 慢闪近似，PWM 真渐变留 F4）
};

// RGB 像素（纯数据，不依赖真机驱动）
struct LedRgb {
    uint8_t r = 0;
    uint8_t g = 0;
    uint8_t b = 0;
};

// 设备状态 → 图案：空闲熄灭，会话各态差异化辨识。
LedPattern LedPatternForState(LedState s);

// 设备状态 → 颜色：对齐旧项目 kyle-s3-lcd 状态灯配色（Connecting 蓝 / Listening 红 / Speaking 绿）。
LedRgb ColorForState(LedState s);

// 给定图案与单调时钟(ms)，返回当前是否应点亮（纯函数，同一时刻结果确定）。
bool ComputeLedLevel(LedPattern p, int64_t now_ms);

}  // namespace kyle

#endif  // KYLE_DRIVERS_LED_PATTERN_H
