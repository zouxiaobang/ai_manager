#ifndef KYLE_DRIVERS_NO_POWER_H
#define KYLE_DRIVERS_NO_POWER_H

#include "hal/power.h"

namespace kyle {

// 无电池/无电源管理占位（supermini-c3 等）：电量未知、深睡/重启留 TODO。
class NoPower : public IPower {
public:
    NoPower() = default;
    ~NoPower() override = default;

    int battery_percent() const override { return -1; }
    bool is_charging() const override { return false; }
    void DeepSleep() override {
        // TODO(driver): esp_deep_sleep_enable_gpio_wakeup + esp_deep_sleep_start
    }
    void Reboot() override {
        // TODO(driver): esp_restart()
    }
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_POWER_H
