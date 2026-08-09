#include "drivers/no_codec_i2s.h"

namespace kyle {

NoCodecI2s::NoCodecI2s(int mic_ws, int mic_sck, int mic_din, int spk_dout,
                       int input_rate, int output_rate)
    : mic_ws_(mic_ws), mic_sck_(mic_sck), mic_din_(mic_din), spk_dout_(spk_dout),
      input_rate_(input_rate), output_rate_(output_rate) {}

bool NoCodecI2s::Start() {
    started_ = true;
    // TODO(driver): i2s_channel_init / i2s_channel_enable
    return true;
}

void NoCodecI2s::Stop() { started_ = false; }

size_t NoCodecI2s::Read(int16_t* dst, size_t samples) {
    (void)dst;
    // TODO(driver): i2s_channel_read 读麦克风 PCM
    return 0;
}

size_t NoCodecI2s::Write(const int16_t* src, size_t samples) {
    (void)src;
    // TODO(driver): i2s_channel_write 写扬声器 PCM
    return 0;
}

void NoCodecI2s::SetOutputVolume(int v) {
    volume_ = v < 0 ? 0 : (v > 100 ? 100 : v);
    // TODO(driver): 无外部 codec 时由 PA 使能/增益控制实现
}

int NoCodecI2s::input_sample_rate() const { return input_rate_; }
int NoCodecI2s::output_sample_rate() const { return output_rate_; }

}  // namespace kyle
