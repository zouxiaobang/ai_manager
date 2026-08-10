#ifndef KYLE_HAL_DISPLAY_H
#define KYLE_HAL_DISPLAY_H

#include "hal/device.h"

namespace kyle {

// 显示能力抽象：LVGL 屏 / OLED / 无屏统一走这套接口。
class IDisplay : public IDevice {
public:
    virtual ~IDisplay() = default;

    virtual void SetStatus(const char* s) = 0;
    virtual void SetChatMessage(const char* role, const char* text) = 0;
    virtual void SetEmotion(const char* e) = 0;
    virtual void ShowToast(const char* msg, int ms) = 0;
    virtual int width() const = 0;
    virtual int height() const = 0;

    // 进入深睡前置屏进入低功耗（如 ST7789 DISPOFF）。默认空实现；支持关屏的面板覆盖。
    virtual void DisplaySleep() {}
};

}  // namespace kyle

#endif  // KYLE_HAL_DISPLAY_H
