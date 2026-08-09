#ifndef KYLE_HAL_INPUT_H
#define KYLE_HAL_INPUT_H

#include <functional>

namespace kyle {

// 按键/触摸统一输入事件
struct InputEvent {
    enum { kClick, kDoubleClick, kLongPress } type;
    int button_id;  // 0 = BOOT，1 = VOL+ / UP，2 = VOL- / DOWN ...
};

class IInput {
public:
    virtual ~IInput() = default;
    // 注册事件回调；驱动在中断/轮询中触发
    virtual void OnEvent(std::function<void(const InputEvent&)> cb) = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_INPUT_H
