#include "drivers/gpio_button.h"

namespace xiaozhi {

GpioButton::GpioButton(const std::vector<int>& pins) : pins_(pins) {}

void GpioButton::OnEvent(std::function<void(const InputEvent&)> cb) {
    cb_ = std::move(cb);
    // TODO(driver): 为每个 pins_ 配置 GPIO 中断，消抖后以 button_id 触发 cb_
}

}  // namespace xiaozhi
