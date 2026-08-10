#ifndef KYLE_DRIVERS_GPIO_LED_H
#define KYLE_DRIVERS_GPIO_LED_H

#include <memory>

#include "drivers/led_pattern.h"
#include "hal/led.h"

namespace kyle {

// 单色 GPIO 状态灯（kyle-s3-lcd=GPIO48 / supermini-c3=GPIO12）。
// SetState 映射 LedState → LedPattern，内部定时任务按单调时钟切换 GPIO 电平。
class GpioLed : public ILed {
public:
    explicit GpioLed(int pin);
    ~GpioLed() override;

    void SetState(LedState s) override;
    void Stop() override;  // 下电：熄灭状态灯

private:
    void Tick();

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_GPIO_LED_H
