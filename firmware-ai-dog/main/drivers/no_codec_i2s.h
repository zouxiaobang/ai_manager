#ifndef KYLE_DRIVERS_NO_CODEC_I2S_H
#define KYLE_DRIVERS_NO_CODEC_I2S_H

#include "hal/audio_codec.h"

namespace kyle {

// 无外部 codec 的 I2S 直连音频（supermini-c3 / kyle-s3-lcd 均为此类）。
// 当前为声明式骨架：TODO(driver) 接入 esp_driver_i2s_std，Read/Write 对接 DMA 缓冲。
class NoCodecI2s : public IAudioCodec {
public:
    NoCodecI2s(int mic_ws, int mic_sck, int mic_din, int spk_dout,
               int input_rate, int output_rate);
    ~NoCodecI2s() override = default;

    bool Start() override;
    void Stop() override;
    size_t Read(int16_t* dst, size_t samples) override;
    size_t Write(const int16_t* src, size_t samples) override;
    void SetOutputVolume(int v) override;
    int input_sample_rate() const override;
    int output_sample_rate() const override;

private:
    int mic_ws_;
    int mic_sck_;
    int mic_din_;
    int spk_dout_;
    int input_rate_;
    int output_rate_;
    int volume_ = 70;
    bool started_ = false;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_CODEC_I2S_H
