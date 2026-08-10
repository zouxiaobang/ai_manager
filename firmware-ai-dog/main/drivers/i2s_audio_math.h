#ifndef KYLE_DRIVERS_I2S_AUDIO_MATH_H
#define KYLE_DRIVERS_I2S_AUDIO_MATH_H

#include <cstdint>

namespace kyle {

// I2S 直连音频的采样变换与音量换算：纯函数，零 ESP-IDF 依赖，host 可单测。
// 量化方式对齐旧 NoAudioCodec：
// - 扬声器：int16 样本 × 音量因子 → int32（32bit 槽位），音量因子平方压缩模拟响度感知；
// - 麦克风：int32 样本 >> 12 → int16（I2S 数据左对齐在 32bit 槽位）。

// 音量(0..100) → int32 放大因子：pow(v/100, 2) * 65536 * 4。
// 65536*4 = 2^18 把 int16 提升到 int32 高位；平方是对数响度近似。
int32_t OutputVolumeFactor(int volume_percent);

// int16 样本 × 音量因子 → int32；用 int64 中间量避免溢出，结果钳到 int32 范围。
int32_t ScaleTo32(int16_t sample, int32_t volume_factor);

// int32 样本 >> 12 → int16；负值依赖算术右移（GCC/xtensa 工具链均为算术右移）。
int16_t DownTo16(int32_t sample);

}  // namespace kyle

#endif  // KYLE_DRIVERS_I2S_AUDIO_MATH_H
