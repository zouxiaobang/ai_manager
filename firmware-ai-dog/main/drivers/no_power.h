#ifndef KYLE_DRIVERS_NO_POWER_H
#define KYLE_DRIVERS_NO_POWER_H

#include "hal/power.h"

namespace kyle {

// 无电池/无电源管理占位：电量未知；深睡由长按 wake_gpio（BOOT，低有效）触发，
// 深睡前先等 BOOT 释放回高，深睡保持低功耗、需再按 BOOT（低电平）唤醒——
// 见 no_power.cc 说明；重启直接 esp_restart。真机实现在 no_power.cc（host 单测不编译）。
class NoPower : public IPower {
public:
    explicit NoPower(int wake_gpio);
    ~NoPower() override = default;

    int battery_percent() const override;
    bool is_charging() const override;
    void DeepSleep() override;
    void Reboot() override;
    void Stop() override;  // 下电终结动作：进入深睡（须由板注册为最后一个设备）

private:
    int wake_gpio_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_NO_POWER_H
