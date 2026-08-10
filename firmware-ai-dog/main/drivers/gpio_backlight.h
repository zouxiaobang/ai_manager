#ifndef KYLE_DRIVERS_GPIO_BACKLIGHT_H
#define KYLE_DRIVERS_GPIO_BACKLIGHT_H

#include "hal/backlight.h"

namespace kyle {

// LEDC PWM 屏幕背光（kyle-s3-lcd GPIO42）。
// 对齐旧 PwmBacklight：LOW_SPEED_MODE、10bit 分辨率、25kHz、高电平有效。
class GpioBacklight : public IBacklight {
public:
    // 百分比(0..100) → LEDC duty(0..1023)，纯换算，host 可单测
    static constexpr int PercentToDuty(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return (kDutyMax * percent) / 100;
    }

    explicit GpioBacklight(int pin);
    ~GpioBacklight() override = default;

    void SetBrightness(int percent) override;  // 0..100
    int brightness() const override;
    void Stop() override;  // 下电：背光亮度归零

private:
    static constexpr int kDutyMax = (1 << 10) - 1;  // 1023
    int pin_;
    int brightness_ = 0;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_GPIO_BACKLIGHT_H
