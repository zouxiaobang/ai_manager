#ifndef KYLE_DRIVERS_NO_CODEC_I2S_H
#define KYLE_DRIVERS_NO_CODEC_I2S_H

#include <memory>

#include "hal/audio_codec.h"

namespace kyle {

// 无外部 codec 的 I2S 直连音频（supermini-c3 / kyle-s3-lcd 均为此类）。
// 单工双通道：I2S_NUM_0 TX 扬声器（输出采样率）、I2S_NUM_1 RX 麦克风（输入采样率），
// 均 32bit mono LEFT，DMA 6×240 帧，与旧 NoAudioCodecSimplex 对齐。
// 采样变换/音量换算纯函数在 i2s_audio_math.h（host 可测），本驱动只做 I2S 搬运。
// 头文件不引入 ESP-IDF 类型（pimpl），与 Cst816sTouch 同风格。
class NoCodecI2s : public IAudioCodec {
public:
    // 引脚顺序与旧 NoAudioCodecSimplex 一致：扬声器(Bclk, Lrck, Dout) + 麦克风(Sck, Ws, Din)
    NoCodecI2s(int spk_bclk, int spk_lrck, int spk_dout,
               int mic_sck, int mic_ws, int mic_din,
               int input_rate, int output_rate);
    ~NoCodecI2s() override;

    bool Start() override;
    void Stop() override;
    size_t Read(int16_t* dst, size_t samples) override;
    size_t Write(const int16_t* src, size_t samples) override;
    void SetOutputVolume(int v) override;
    int input_sample_rate() const override;
    int output_sample_rate() const override;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_CODEC_I2S_H
