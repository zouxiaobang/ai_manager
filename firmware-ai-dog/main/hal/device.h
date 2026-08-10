#ifndef KYLE_HAL_DEVICE_H
#define KYLE_HAL_DEVICE_H

namespace kyle {

// 板载设备统一生命周期抽象：LED / 功放 / 麦克风 / 显示屏 / 电源等外设继承本接口，
// 由 IBoard::RegisterDevice 注册进板级设备列表。整板下电（如长按深睡）时，板遍历
// 列表调用各设备 Stop()，应用层不感知具体设备集合与关断顺序。
// 方法全部默认空实现：异构设备大多没有 pause/resume/restart 语义，只在有意义时覆盖
//（对齐 IDisplay::DisplaySleep 的空实现模式），避免设备被迫实现一堆空壳方法。
class IDevice {
public:
    virtual ~IDevice() = default;

    // 设备级初始化（板 Init 已做总线/GPIO 初始化，多数设备无需覆盖）
    virtual void Init() {}
    // 暂停：会话级挂起（当前无设备用到，预留）
    virtual void Pause() {}
    // 停止/下电：整板下电时被遍历调用，每台设备做自己的关断
    //（功放 Stop、屏 DISPOFF、背光 0、灯灭、电源深睡）
    virtual void Stop() {}
    // 继续：与 Pause 对应
    virtual void Resume() {}
    // 重启：停止后重新初始化；默认空实现，需要时设备覆盖
    virtual void Restart() {}
    // 销毁前清理：设备对象由板 unique_ptr 拥有，析构即销毁，一般无需覆盖
    virtual void Destroy() {}
};

}  // namespace kyle

#endif  // KYLE_HAL_DEVICE_H
