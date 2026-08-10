#ifndef KYLE_HAL_AUDIO_CODEC_H
#define KYLE_HAL_AUDIO_CODEC_H

#include <cstddef>
#include <cstdint>

#include "hal/device.h"

namespace kyle {

// 音频硬件能力抽象：只描述「读写 PCM / 音量 / 采样率」，不含编解码逻辑。
// 解码归 core/audio_pipeline（本骨架未实现），codec 只做裸 PCM 搬运。
// 继承 IDevice：功放/麦克风是设备，可注册进板级设备列表；Stop() 由各 codec 实现。
class IAudioCodec : public IDevice {
public:
    virtual ~IAudioCodec() = default;

    virtual bool Start() = 0;
    virtual void Stop() = 0;  // 重抽象为纯虚：具体 codec 必须实现（同时满足 IDevice::Stop）

    // 麦克风 PCM（16-bit 单声道），返回实际读到的样本数
    virtual size_t Read(int16_t* dst, size_t samples) = 0;
    // 扬声器 PCM，返回实际写入的样本数
    virtual size_t Write(const int16_t* src, size_t samples) = 0;

    virtual void SetOutputVolume(int v) = 0;  // 0..100
    virtual int input_sample_rate() const = 0;
    virtual int output_sample_rate() const = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_AUDIO_CODEC_H
