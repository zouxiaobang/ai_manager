#include "drivers/led_pattern.h"

namespace kyle {

LedPattern LedPatternForState(LedState s) {
    switch (s) {
        case LedState::kLedIdle:
            return LedPattern::kLedOff;  // 空闲熄灭（K4 决策）
        case LedState::kLedConnecting:
            return LedPattern::kLedBlinkSlow;
        case LedState::kLedListening:
            return LedPattern::kLedSolidOn;  // 监听中常亮
        case LedState::kLedSpeaking:
            return LedPattern::kLedBlinkFast;  // 说话中快闪
        case LedState::kLedError:
            return LedPattern::kLedDoubleBlink;
    }
    return LedPattern::kLedOff;
}

LedRgb ColorForState(LedState s) {
    // 亮度取低值（WS2812 满量程 255 太刺眼），对齐旧项目 DEFAULT_BRIGHTNESS=4
    constexpr uint8_t kDim = 4;
    switch (s) {
        case LedState::kLedIdle:
            return {0, 0, 0};          // 空闲熄灭
        case LedState::kLedConnecting:
            return {0, 0, kDim};       // 连接中：蓝
        case LedState::kLedListening:
            return {kDim, 0, 0};       // 监听中：红
        case LedState::kLedSpeaking:
            return {0, kDim, 0};       // 说话中：绿
        case LedState::kLedError:
            return {kDim, kDim, 0};    // 错误：黄（区别于监听的红）
    }
    return {0, 0, 0};
}

bool ComputeLedLevel(LedPattern p, int64_t now_ms) {
    switch (p) {
        case LedPattern::kLedOff:
            return false;
        case LedPattern::kLedSolidOn:
            return true;
        case LedPattern::kLedBlinkSlow:
            return (now_ms / 500) % 2 == 0;  // 500ms 亮 / 500ms 灭
        case LedPattern::kLedBlinkFast:
            return (now_ms / 100) % 2 == 0;  // 100ms 亮 / 100ms 灭
        case LedPattern::kLedDoubleBlink: {
            // 1.2s 周期：0-300 亮、300-400 灭、400-700 亮、700-1200 灭
            const int64_t t = now_ms % 1200;
            return t < 300 || (t >= 400 && t < 700);
        }
        case LedPattern::kLedBreathing:
            // 3s 周期慢闪近似呼吸（真 PWM 渐变留 F4 动画层）
            return (now_ms / 1500) % 2 == 0;
    }
    return false;
}

}  // namespace kyle
