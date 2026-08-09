#ifndef KYLE_DRIVERS_CST816S_TOUCH_H
#define KYLE_DRIVERS_CST816S_TOUCH_H

#include <functional>
#include <memory>

#include "hal/input.h"

namespace kyle {

// CST816S 触摸驱动（I2C 0x38，SCL/SDA 引脚来自 board_config）。
// 实现 IInput：FreeRTOS 任务按 50Hz 轮询触点；手势（单击/双击/长按）
// 由软件时序识别（坐标模式下硬件手势寄存器恒为 0），识别逻辑在 cst816s_parse.h
// 的 TouchGestureDetector（host 可测），本驱动负责读取原始字节 + 提供单调时钟。
// 头文件不引入 ESP-IDF 类型（pimpl），与 St7789Lcd 同风格。
class Cst816sTouch : public IInput {
public:
    Cst816sTouch(int sda, int scl, int width, int height);
    ~Cst816sTouch() override;

    void OnEvent(std::function<void(const InputEvent&)> cb) override;

private:
    void PollOnce();

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_CST816S_TOUCH_H
