#ifndef KYLE_DRIVERS_GPIO_LED_H
#define KYLE_DRIVERS_GPIO_LED_H

#include "hal/led.h"

namespace kyle {

// 单色 GPIO 状态灯（supermini-c3=GPIO12，kyle-s3-lcd=GPIO48）。
// 当前为声明式骨架：TODO(driver) 映射 LedState → GPIO 电平/闪烁模式。
class GpioLed : public ILed {
public:
    explicit GpioLed(int pin);
    ~GpioLed() override = default;

    void SetState(LedState s) override;

private:
    int pin_;
    LedState state_ = LedState::kLedIdle;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_GPIO_LED_H
