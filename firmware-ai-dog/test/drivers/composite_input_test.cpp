#include <memory>
#include <vector>

#include "drivers/composite_input.h"
#include "hal/input.h"
#include "unity.h"

using namespace kyle;

namespace {

// 测试用输入源：可手动触发事件
class TestInput : public IInput {
public:
    std::function<void(const InputEvent&)> cb;
    int callback_count = 0;  // OnEvent 被调用次数（驱动注册回调）
    void OnEvent(std::function<void(const InputEvent&)> c) override {
        cb = std::move(c);
        callback_count++;
    }
    void Emit(const InputEvent& ev) {
        if (cb) cb(ev);
    }
};

}  // namespace

TEST_CASE("Composite forwards events from all sources", "[composite_input]") {
    CompositeInput composite;
    auto a = std::make_unique<TestInput>();
    auto b = std::make_unique<TestInput>();
    auto* pa = a.get();
    auto* pb = b.get();
    composite.Add(std::move(a));
    composite.Add(std::move(b));

    std::vector<InputEvent> got;
    composite.OnEvent([&got](const InputEvent& ev) { got.push_back(ev); });

    pa->Emit(InputEvent{InputEvent::kClick, kButtonBoot});
    pb->Emit(InputEvent{InputEvent::kLongPress, kButtonVolUp});
    TEST_ASSERT_EQUAL_INT(2, got.size());
    TEST_ASSERT_EQUAL_INT(InputEvent::kClick, got[0].type);
    TEST_ASSERT_EQUAL_INT(kButtonBoot, got[0].button_id);
    TEST_ASSERT_EQUAL_INT(InputEvent::kLongPress, got[1].type);
    TEST_ASSERT_EQUAL_INT(kButtonVolUp, got[1].button_id);
}

TEST_CASE("Composite Add after OnEvent forwards immediately", "[composite_input]") {
    CompositeInput composite;
    std::vector<InputEvent> got;
    composite.OnEvent([&got](const InputEvent& ev) { got.push_back(ev); });

    auto late = std::make_unique<TestInput>();
    auto* p = late.get();
    composite.Add(std::move(late));  // 回调已注册，立即转发

    p->Emit(InputEvent{InputEvent::kClick, kTouchButtonId});
    TEST_ASSERT_EQUAL_INT(1, got.size());
    TEST_ASSERT_EQUAL_INT(kTouchButtonId, got[0].button_id);
}

TEST_CASE("Composite ignores null source without crash", "[composite_input]") {
    CompositeInput composite;
    composite.Add(nullptr);  // 不应崩溃
    composite.Add(nullptr);
    // 回调注册后仍可用，空源不产生事件
    int got = 0;
    composite.OnEvent([&got](const InputEvent&) { got++; });
    TEST_ASSERT_EQUAL_INT(0, got);
}

TEST_CASE("Composite forwards registration to every source", "[composite_input]") {
    CompositeInput composite;
    auto a = std::make_unique<TestInput>();
    auto b = std::make_unique<TestInput>();
    auto* pa = a.get();
    auto* pb = b.get();
    composite.Add(std::move(a));
    composite.Add(std::move(b));
    composite.OnEvent([](const InputEvent&) {});  // 注册回调
    TEST_ASSERT_EQUAL_INT(1, pa->callback_count);
    TEST_ASSERT_EQUAL_INT(1, pb->callback_count);
}
