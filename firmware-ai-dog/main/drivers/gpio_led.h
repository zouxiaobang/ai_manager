#ifndef XIAOZHI_DRIVERS_GPIO_LED_H
#define XIAOZHI_DRIVERS_GPIO_LED_H

#include "hal/led.h"

namespace xiaozhi {

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

}  // namespace xiaozhi

#endif  // XIAOZHI_DRIVERS_GPIO_LED_H
