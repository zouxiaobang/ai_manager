#ifndef KYLE_DRIVERS_COMPOSITE_INPUT_H
#define KYLE_DRIVERS_COMPOSITE_INPUT_H

#include <functional>
#include <memory>
#include <vector>

#include "hal/input.h"

namespace kyle {

// 聚合多个 IInput（触摸 + 按键）为一个输入源：事件统一转发给注册回调。
// 板级组装用，各子驱动（Cst816sTouch / GpioButton）各自独立轮询/触发，互不感知。
class CompositeInput : public IInput {
public:
    CompositeInput() = default;
    ~CompositeInput() override = default;

    // 追加子输入源；若回调已注册则立即转发（先 Add 后 OnEvent、或反之均可）
    void Add(std::unique_ptr<IInput> input);

    void OnEvent(std::function<void(const InputEvent&)> cb) override;

private:
    std::vector<std::unique_ptr<IInput>> inputs_;
    std::function<void(const InputEvent&)> cb_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_COMPOSITE_INPUT_H
