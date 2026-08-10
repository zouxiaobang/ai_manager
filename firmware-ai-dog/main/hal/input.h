#ifndef KYLE_HAL_INPUT_H
#define KYLE_HAL_INPUT_H

#include <functional>

namespace kyle {

// 统一 button_id 约定：物理按键 0/1/2，触摸固定 10（区分两类输入源）
constexpr int kButtonBoot = 0;
constexpr int kButtonVolUp = 1;
constexpr int kButtonVolDown = 2;
constexpr int kTouchButtonId = 10;

// 按键/触摸统一输入事件
struct InputEvent {
    enum { kClick, kDoubleClick, kLongPress } type;
    int button_id;  // 见上方 kButton* / kTouchButtonId 约定
};

class IInput {
public:
    virtual ~IInput() = default;
    // 注册事件回调；驱动在中断/轮询中触发
    virtual void OnEvent(std::function<void(const InputEvent&)> cb) = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_INPUT_H
