#include "drivers/no_codec_i2s.h"

#include <vector>

#include "esp_log.h"

// I2S 通道 API 只在真机构建可用；host 单测只编译 i2s_audio_math.cc，不编译本文件。
#ifdef ESP_PLATFORM
#include "driver/i2s_std.h"
#include "freertos/FreeRTOS.h"  // portMAX_DELAY
#endif

#include "drivers/i2s_audio_math.h"

#define TAG "NoCodecI2s"

namespace kyle {

namespace {
constexpr int kDmaDescNum = 6;    // 与旧 NoAudioCodec 的 AUDIO_CODEC_DMA_DESC_NUM 一致
constexpr int kDmaFrameNum = 240;  // 与旧 NoAudioCodec 的 AUDIO_CODEC_DMA_FRAME_NUM 一致
}  // namespace

struct NoCodecI2s::Impl {
    int input_rate = 0;
    int output_rate = 0;
    int volume = 70;  // 默认 70%，与旧 NoAudioCodec 默认一致
    bool started = false;
#ifdef ESP_PLATFORM
    i2s_chan_handle_t tx = nullptr;  // I2S_NUM_0：扬声器
    i2s_chan_handle_t rx = nullptr;  // I2S_NUM_1：麦克风
#endif
};

NoCodecI2s::NoCodecI2s(int spk_bclk, int spk_lrck, int spk_dout,
                       int mic_sck, int mic_ws, int mic_din,
                       int input_rate, int output_rate)
    : impl_(std::make_unique<Impl>()) {
    impl_->input_rate = input_rate;
    impl_->output_rate = output_rate;
#ifdef ESP_PLATFORM
    // 扬声器与麦克风共用同一套标准 I2S 时序配置（各自采样率/引脚不同），单工各占一路端口
    i2s_chan_config_t chan_cfg = {
        .id = I2S_NUM_0,
        .role = I2S_ROLE_MASTER,
        .dma_desc_num = kDmaDescNum,
        .dma_frame_num = kDmaFrameNum,
        .auto_clear_after_cb = true,
        .auto_clear_before_cb = false,
        .intr_priority = 0,
    };

    // TX（扬声器）：输出采样率，引脚 bclk/lrck/dout
    if (i2s_new_channel(&chan_cfg, &impl_->tx, nullptr) != ESP_OK) {
        ESP_LOGE(TAG, "扬声器 I2S 通道创建失败");
        return;
    }
    i2s_std_config_t tx_cfg = {
        .clk_cfg = {
            .sample_rate_hz = static_cast<uint32_t>(output_rate),
            .clk_src = I2S_CLK_SRC_DEFAULT,
            .ext_clk_freq_hz = 0,
            .mclk_multiple = I2S_MCLK_MULTIPLE_256,
            .bclk_div = 0,  // 仅 slave 角色生效，master 忽略
        },
        .slot_cfg = {
            .data_bit_width = I2S_DATA_BIT_WIDTH_32BIT,
            .slot_bit_width = I2S_SLOT_BIT_WIDTH_AUTO,
            .slot_mode = I2S_SLOT_MODE_MONO,
            .slot_mask = I2S_STD_SLOT_LEFT,
            .ws_width = I2S_DATA_BIT_WIDTH_32BIT,
            .ws_pol = false,
            .bit_shift = true,
            .left_align = true,
            .big_endian = false,
            .bit_order_lsb = false,
        },
        .gpio_cfg = {
            .mclk = I2S_GPIO_UNUSED,
            .bclk = static_cast<gpio_num_t>(spk_bclk),
            .ws = static_cast<gpio_num_t>(spk_lrck),
            .dout = static_cast<gpio_num_t>(spk_dout),
            .din = I2S_GPIO_UNUSED,
            .invert_flags = {.mclk_inv = false, .bclk_inv = false, .ws_inv = false},
        },
    };
    if (i2s_channel_init_std_mode(impl_->tx, &tx_cfg) != ESP_OK) {
        ESP_LOGE(TAG, "扬声器 I2S 标准模式初始化失败");
        return;
    }

    // RX（麦克风）：输入采样率，引脚 sck/ws/din
    chan_cfg.id = I2S_NUM_1;
    if (i2s_new_channel(&chan_cfg, nullptr, &impl_->rx) != ESP_OK) {
        ESP_LOGE(TAG, "麦克风 I2S 通道创建失败");
        return;
    }
    i2s_std_config_t rx_cfg = tx_cfg;
    rx_cfg.clk_cfg.sample_rate_hz = static_cast<uint32_t>(input_rate);
    rx_cfg.gpio_cfg.bclk = static_cast<gpio_num_t>(mic_sck);
    rx_cfg.gpio_cfg.ws = static_cast<gpio_num_t>(mic_ws);
    rx_cfg.gpio_cfg.dout = I2S_GPIO_UNUSED;
    rx_cfg.gpio_cfg.din = static_cast<gpio_num_t>(mic_din);
    if (i2s_channel_init_std_mode(impl_->rx, &rx_cfg) != ESP_OK) {
        ESP_LOGE(TAG, "麦克风 I2S 标准模式初始化失败");
        return;
    }
    ESP_LOGI(TAG, "I2S 直连音频初始化完成（mic %dHz / spk %dHz）", input_rate, output_rate);
#endif
}

NoCodecI2s::~NoCodecI2s() {
    Stop();
#ifdef ESP_PLATFORM
    if (impl_->tx) {
        i2s_channel_disable(impl_->tx);
        i2s_del_channel(impl_->tx);
    }
    if (impl_->rx) {
        i2s_channel_disable(impl_->rx);
        i2s_del_channel(impl_->rx);
    }
#endif
}

bool NoCodecI2s::Start() {
    if (impl_->started) {
        return true;
    }
#ifdef ESP_PLATFORM
    if (impl_->tx && i2s_channel_enable(impl_->tx) != ESP_OK) {
        ESP_LOGE(TAG, "扬声器通道使能失败");
        return false;
    }
    if (impl_->rx && i2s_channel_enable(impl_->rx) != ESP_OK) {
        ESP_LOGE(TAG, "麦克风通道使能失败");
        return false;
    }
#endif
    impl_->started = true;
    return true;
}

void NoCodecI2s::Stop() {
    if (!impl_->started) {
        return;
    }
#ifdef ESP_PLATFORM
    if (impl_->tx) {
        i2s_channel_disable(impl_->tx);
    }
    if (impl_->rx) {
        i2s_channel_disable(impl_->rx);
    }
#endif
    impl_->started = false;
}

size_t NoCodecI2s::Read(int16_t* dst, size_t samples) {
#ifdef ESP_PLATFORM
    if (impl_->rx == nullptr || !impl_->started) {
        return 0;
    }
    // 麦克风原始数据是 32bit 槽位，先按 int32 收，再降位到 int16
    std::vector<int32_t> buf(samples);
    size_t bytes_read = 0;
    if (i2s_channel_read(impl_->rx, buf.data(), samples * sizeof(int32_t), &bytes_read,
                         portMAX_DELAY) != ESP_OK) {
        ESP_LOGE(TAG, "麦克风读取失败");
        return 0;
    }
    const size_t got = bytes_read / sizeof(int32_t);
    for (size_t i = 0; i < got; ++i) {
        dst[i] = DownTo16(buf[i]);
    }
    return got;
#else
    (void)dst;
    (void)samples;
    return 0;
#endif
}

size_t NoCodecI2s::Write(const int16_t* src, size_t samples) {
#ifdef ESP_PLATFORM
    if (impl_->tx == nullptr || !impl_->started) {
        return 0;
    }
    // int16 按音量因子放大到 int32 槽位，匹配扬声器 32bit 数据格式
    const int32_t factor = OutputVolumeFactor(impl_->volume);
    std::vector<int32_t> buf(samples);
    for (size_t i = 0; i < samples; ++i) {
        buf[i] = ScaleTo32(src[i], factor);
    }
    size_t bytes_written = 0;
    if (i2s_channel_write(impl_->tx, buf.data(), samples * sizeof(int32_t), &bytes_written,
                          portMAX_DELAY) != ESP_OK) {
        ESP_LOGE(TAG, "扬声器写入失败");
        return 0;
    }
    return bytes_written / sizeof(int32_t);
#else
    (void)src;
    (void)samples;
    return 0;
#endif
}

void NoCodecI2s::SetOutputVolume(int v) {
    impl_->volume = v < 0 ? 0 : (v > 100 ? 100 : v);
}

int NoCodecI2s::input_sample_rate() const { return impl_->input_rate; }
int NoCodecI2s::output_sample_rate() const { return impl_->output_rate; }

}  // namespace kyle
