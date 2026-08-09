#ifndef KYLE_HAL_POWER_H
#define KYLE_HAL_POWER_H

namespace kyle {

// 电源/电池/睡眠能力（supermini-c3 无电池，可用 NoPower 空实现）
class IPower {
public:
    virtual ~IPower() = default;

    virtual int battery_percent() const = 0;  // -1 表示未知
    virtual bool is_charging() const = 0;
    virtual void DeepSleep() = 0;
    virtual void Reboot() = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_POWER_H
