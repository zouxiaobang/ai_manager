#include "drivers/gpio_backlight.h"

#include "driver/ledc.h"
#include "esp_log.h"

#define TAG "GpioBacklight"

namespace kyle {

namespace {
constexpr ledc_mode_t kSpeedMode = LEDC_LOW_SPEED_MODE;
constexpr ledc_timer_t kTimer = LEDC_TIMER_0;
constexpr ledc_channel_t kChannel = LEDC_CHANNEL_0;
constexpr int kFreqHz = 25000;  // 高一点防止电感啸叫，对齐旧 PwmBacklight
}  // namespace

GpioBacklight::GpioBacklight(int pin) : pin_(pin) {
    const ledc_timer_config_t timer_cfg = {
        .speed_mode = kSpeedMode,
        .duty_resolution = LEDC_TIMER_10_BIT,
        .timer_num = kTimer,
        .freq_hz = kFreqHz,
        .clk_cfg = LEDC_AUTO_CLK,
        .deconfigure = false,
    };
    ESP_ERROR_CHECK(ledc_timer_config(&timer_cfg));

    // {} 全量清零再逐字段赋值，避免缺省字段（如 sleep_mode）告警。
    ledc_channel_config_t chan_cfg = {};
    chan_cfg.gpio_num = pin_;
    chan_cfg.speed_mode = kSpeedMode;
    chan_cfg.channel = kChannel;
    chan_cfg.intr_type = LEDC_INTR_DISABLE;
    chan_cfg.timer_sel = kTimer;
    chan_cfg.duty = 0;
    chan_cfg.hpoint = 0;
    chan_cfg.flags.output_invert = 0;  // kyle-s3-lcd 背光高电平有效
    ESP_ERROR_CHECK(ledc_channel_config(&chan_cfg));
}

void GpioBacklight::SetBrightness(int percent) {
    brightness_ = (percent < 0) ? 0 : (percent > 100) ? 100 : percent;
    ESP_ERROR_CHECK(ledc_set_duty(kSpeedMode, kChannel, PercentToDuty(brightness_)));
    ESP_ERROR_CHECK(ledc_update_duty(kSpeedMode, kChannel));
    ESP_LOGI(TAG, "背光亮度 %d%%", brightness_);
}

int GpioBacklight::brightness() const { return brightness_; }

}  // namespace kyle
