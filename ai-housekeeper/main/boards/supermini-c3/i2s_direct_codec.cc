#include "i2s_direct_codec.h"
#include <esp_check.h>

const char* I2sDirectCodec::TAG = "I2sDirectCodec";

I2sDirectCodec::I2sDirectCodec(int input_sample_rate, int output_sample_rate) 
    : bclk_pin_(GPIO_NUM_6),  // 默认引脚，匹配你的接线
      ws_pin_(GPIO_NUM_5),
      din_pin_(GPIO_NUM_4),
      dout_pin_(GPIO_NUM_7),
      mclk_pin_(GPIO_NUM_0),
      started_(false) {
    
    ESP_LOGI(TAG, "I2sDirectCodec constructor");
    
    // 设置基类参数
    duplex_ = true;                    // 支持双工（同时输入输出）
    input_reference_ = false;          // 无参考输入
    input_enabled_ = true;             // 启用输入
    output_enabled_ = true;            // 启用输出
    input_sample_rate_ = input_sample_rate;
    output_sample_rate_ = output_sample_rate;
    input_channels_ = 1;               // 单声道输入
    output_channels_ = 1;              // 单声道输出
    output_volume_ = 40;               // 默认音量40%
    input_gain_ = 0.0f;                // 默认增益0dB
    
    ESP_LOGI(TAG, "Codec configured: in=%dHz, out=%dHz, duplex=%d", 
             input_sample_rate_, output_sample_rate_, duplex_);
}

I2sDirectCodec::~I2sDirectCodec() {
    ESP_LOGI(TAG, "I2sDirectCodec destructor");
    
    if (started_) {
        // 停止I2S通道
        if (tx_handle_) {
            i2s_channel_disable(tx_handle_);
        }
        if (rx_handle_) {
            i2s_channel_disable(rx_handle_);
        }
        
        // 删除通道
        if (tx_handle_) {
            i2s_del_channel(tx_handle_);
        }
        if (rx_handle_) {
            i2s_del_channel(rx_handle_);
        }
        
        started_ = false;
    }
}

void I2sDirectCodec::SetPins(gpio_num_t bclk, gpio_num_t ws, 
                             gpio_num_t din, gpio_num_t dout, 
                             gpio_num_t mclk) {
    bclk_pin_ = bclk;
    ws_pin_ = ws;
    din_pin_ = din;
    dout_pin_ = dout;
    mclk_pin_ = mclk;
    
    ESP_LOGI(TAG, "Pins set: BCLK=%d, WS=%d, DIN=%d, DOUT=%d, MCLK=%d",
             bclk_pin_, ws_pin_, din_pin_, dout_pin_, mclk_pin_);
}

void I2sDirectCodec::Start() {
    ESP_LOGI(TAG, "Starting I2S direct codec...");
    
    if (started_) {
        ESP_LOGW(TAG, "Codec already started");
        return;
    }
    
    // 初始化I2S硬件
    InitializeI2S();
    
    started_ = true;
    ESP_LOGI(TAG, "I2S direct codec started");
}

void I2sDirectCodec::InitializeI2S() {
    ESP_LOGI(TAG, "Initializing I2S hardware...");
    
    // I2S通道配置
    i2s_chan_config_t chan_cfg = I2S_CHANNEL_DEFAULT_CONFIG(I2S_NUM_0, I2S_ROLE_MASTER);
    chan_cfg.auto_clear = true;
    
    // 创建通道
    ESP_ERROR_CHECK(i2s_new_channel(&chan_cfg, &tx_handle_, &rx_handle_));
    
    // 使用ESP-IDF提供的宏来配置，避免结构体成员不匹配的问题
    i2s_std_config_t std_cfg = {
        .clk_cfg = I2S_STD_CLK_DEFAULT_CONFIG(static_cast<uint32_t>(input_sample_rate_)),
        .slot_cfg = I2S_STD_MSB_SLOT_DEFAULT_CONFIG(I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO),
        .gpio_cfg = {
            .mclk = mclk_pin_,
            .bclk = bclk_pin_,
            .ws = ws_pin_,
            .dout = dout_pin_,
            .din = din_pin_,
            .invert_flags = {
                .mclk_inv = false,
                .bclk_inv = false,
                .ws_inv = false,
            },
        },
    };
    
    // 初始化TX通道（输出到功放）
    if (tx_handle_) {
        ESP_ERROR_CHECK(i2s_channel_init_std_mode(tx_handle_, &std_cfg));
        ESP_ERROR_CHECK(i2s_channel_enable(tx_handle_));
        ESP_LOGI(TAG, "TX channel initialized and enabled");
    }
    
    // 初始化RX通道（输入从麦克风）
    if (rx_handle_) {
        ESP_ERROR_CHECK(i2s_channel_init_std_mode(rx_handle_, &std_cfg));
        ESP_ERROR_CHECK(i2s_channel_enable(rx_handle_));
        ESP_LOGI(TAG, "RX channel initialized and enabled");
    }
    
    ESP_LOGI(TAG, "I2S hardware initialized successfully");
}

int I2sDirectCodec::Read(int16_t* dest, int samples) {
    ESP_LOGI(TAG, "Read: %d, %d", dest, samples);
    if (!started_ || !rx_handle_ || !input_enabled_) {
        ESP_LOGD(TAG, "Read failed: started=%d, rx_handle=%p, input_enabled=%d", 
                started_, rx_handle_, input_enabled_);
        return 0;
    }
    
    size_t bytes_read = 0;
    size_t bytes_to_read = samples * sizeof(int16_t);
    
    esp_err_t ret = i2s_channel_read(rx_handle_, dest, bytes_to_read, 
                                     &bytes_read, 100 / portTICK_PERIOD_MS);
    
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2S read failed: %s", esp_err_to_name(ret));
        return 0;
    }
    
    int samples_read = bytes_read / sizeof(int16_t);
    
    if (samples_read > 0) {
        ESP_LOGD(TAG, "Read %d samples from microphone", samples_read);
    }
    ESP_LOGI(TAG, "Read success: %d", samples_read);
    
    return samples_read;
}

int I2sDirectCodec::Write(const int16_t* data, int samples) {
    if (!started_ || !tx_handle_ || !output_enabled_) {
        ESP_LOGD(TAG, "Write failed: started=%d, tx_handle=%p, output_enabled=%d", 
                started_, tx_handle_, output_enabled_);
        return 0;
    }
    
    size_t bytes_written = 0;
    size_t bytes_to_write = samples * sizeof(int16_t);
    
    esp_err_t ret = i2s_channel_write(tx_handle_, data, bytes_to_write, 
                                      &bytes_written, portMAX_DELAY);
    
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2S write failed: %s", esp_err_to_name(ret));
        return 0;
    }
    
    int samples_written = bytes_written / sizeof(int16_t);
    return samples_written;
}

void I2sDirectCodec::EnableInput(bool enable) {
    input_enabled_ = enable;
    ESP_LOGI(TAG, "Input %s", enable ? "enabled" : "disabled");
}

void I2sDirectCodec::EnableOutput(bool enable) {
    output_enabled_ = enable;
    ESP_LOGI(TAG, "Output %s", enable ? "enabled" : "disabled");
}

void I2sDirectCodec::SetOutputVolume(int volume) {
    if (volume < 0) volume = 0;
    if (volume > 100) volume = 100;
    
    output_volume_ = volume;
    ESP_LOGI(TAG, "Output volume set to %d%%", volume);
}

void I2sDirectCodec::SetInputGain(float gain) {
    input_gain_ = gain;
    ESP_LOGI(TAG, "Input gain set to %.1f dB", gain);
}