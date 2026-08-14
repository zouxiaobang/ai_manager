#ifndef I2S_DIRECT_CODEC_H
#define I2S_DIRECT_CODEC_H

#include "audio_codec.h"
#include "esp_log.h"
#include "driver/i2s_std.h"
#include "driver/gpio.h"

class I2sDirectCodec : public AudioCodec {
private:
    static const char* TAG;
    
    // I2S引脚配置
    gpio_num_t bclk_pin_;
    gpio_num_t ws_pin_;
    gpio_num_t din_pin_;
    gpio_num_t dout_pin_;
    gpio_num_t mclk_pin_;
    
    // 状态标志
    bool started_;
    
public:
    I2sDirectCodec(int input_sample_rate, int output_sample_rate);
    virtual ~I2sDirectCodec();
    
    // 设置引脚（在Start之前调用）
    void SetPins(gpio_num_t bclk, gpio_num_t ws, gpio_num_t din, 
                 gpio_num_t dout, gpio_num_t mclk = GPIO_NUM_0);
    
    // 必须实现的纯虚函数
    virtual int Read(int16_t* dest, int samples) override;
    virtual int Write(const int16_t* data, int samples) override;
    
    // 重写Start函数来初始化I2S硬件
    virtual void Start() override;
    
    // 重写其他函数
    virtual void EnableInput(bool enable) override;
    virtual void EnableOutput(bool enable) override;
    virtual void SetOutputVolume(int volume) override;
    virtual void SetInputGain(float gain) override;
    
private:
    void InitializeI2S();
};

#endif // I2S_DIRECT_CODEC_H