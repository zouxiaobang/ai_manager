#ifndef KYLE_DRIVERS_LED_STRIP_LED_H
#define KYLE_DRIVERS_LED_STRIP_LED_H

#include <memory>

#include "drivers/led_pattern.h"
#include "hal/led.h"

namespace kyle {

// WS2812 单颗 RGB 灯珠驱动（kyle-s3-lcd GPIO48）：led_strip + RMT 发像素时序。
// 真机驱动，host 单测不编译本文件（纯逻辑在 led_pattern.h/.cc）。
class LedStripLed : public ILed {
public:
    explicit LedStripLed(int gpio);
    ~LedStripLed() override;

    void SetState(LedState s) override;
    void Stop() override;  // 下电：熄灭灯珠

private:
    void Tick();

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_LED_STRIP_LED_H
