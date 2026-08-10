#include "drivers/composite_input.h"

namespace kyle {

void CompositeInput::Add(std::unique_ptr<IInput> input) {
    if (input == nullptr) {
        return;
    }
    // 先挂回调再入列：若回调已注册，新加入的子驱动立即可用（不丢事件）
    if (cb_) {
        input->OnEvent(cb_);
    }
    inputs_.push_back(std::move(input));
}

void CompositeInput::OnEvent(std::function<void(const InputEvent&)> cb) {
    cb_ = std::move(cb);
    for (auto& in : inputs_) {
        in->OnEvent(cb_);
    }
}

}  // namespace kyle
