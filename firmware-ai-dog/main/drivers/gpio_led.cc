#include "drivers/gpio_led.h"

namespace xiaozhi {

GpioLed::GpioLed(int pin) : pin_(pin) {}

void GpioLed::SetState(LedState s) {
    state_ = s;
    // TODO(driver): gpio_set_level / 定时闪烁
}

}  // namespace xiaozhi
