#include "drivers/i2s_audio_math.h"

#include <cmath>
#include <limits>

namespace kyle {

int32_t OutputVolumeFactor(int volume_percent) {
    if (volume_percent < 0) {
        volume_percent = 0;
    }
    if (volume_percent > 100) {
        volume_percent = 100;
    }
    // 与旧 NoAudioCodec 一致：平方压缩 + 65536*4 抬位。
    // 100% → 262144（2^18）；50% → 65536（2^16）；0% → 0。
    return static_cast<int32_t>(std::pow(volume_percent / 100.0, 2.0) * 65536.0 * 4.0);
}

int32_t ScaleTo32(int16_t sample, int32_t volume_factor) {
    // int16 × int32 可能溢出，先用 int64 求积再钳位
    const int64_t temp = static_cast<int64_t>(sample) * volume_factor;
    if (temp > std::numeric_limits<int32_t>::max()) {
        return std::numeric_limits<int32_t>::max();
    }
    if (temp < std::numeric_limits<int32_t>::min()) {
        return std::numeric_limits<int32_t>::min();
    }
    return static_cast<int32_t>(temp);
}

int16_t DownTo16(int32_t sample) {
    // I2S 数据在 32bit 槽位左对齐，右移 12 位回到 int16 量程
    const int32_t value = sample >> 12;
    if (value > std::numeric_limits<int16_t>::max()) {
        return std::numeric_limits<int16_t>::max();
    }
    if (value < std::numeric_limits<int16_t>::min()) {
        return std::numeric_limits<int16_t>::min();
    }
    return static_cast<int16_t>(value);
}

}  // namespace kyle
