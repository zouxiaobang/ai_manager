#ifndef XIAOZHI_DRIVERS_GPIO_BUTTON_H
#define XIAOZHI_DRIVERS_GPIO_BUTTON_H

#include <functional>
#include <vector>

#include "hal/input.h"

namespace xiaozhi {

// GPIO 按键输入（BOOT / 音量 / UP/DOWN）。
// 当前为声明式骨架：TODO(driver) 接入 gpio 中断 + 消抖 + 单击/双击/长按识别。
class GpioButton : public IInput {
public:
    explicit GpioButton(const std::vector<int>& pins);
    ~GpioButton() override = default;

    void OnEvent(std::function<void(const InputEvent&)> cb) override;

private:
    std::vector<int> pins_;
    std::function<void(const InputEvent&)> cb_;
};

}  // namespace xiaozhi

#endif  // XIAOZHI_DRIVERS_GPIO_BUTTON_H
