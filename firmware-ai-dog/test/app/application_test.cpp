// Application 装配测试：输入事件 → 会话状态机 + LED 联动 + 音量 + 深睡。
// 纯 host 测试，IBoard/IAudioCodec/ILed/IPower 全部用 mock_board.h 的内存实现。

#include "app/application.h"
#include "core/chat_session.h"
#include "hal/input.h"
#include "mocks/mock_board.h"
#include "mocks/mock_storage.h"
#include "unity.h"

using namespace kyle;

namespace {

// 局部构造并初始化一个 Application，返回其输入源以便触发事件
struct Harness {
    MockBoard board;
    MockInput input;
    MockStorage storage;
    Application app;
    Harness() : app(board, storage) {
        board.input_source = &input;
        // 复刻板级注册：按关断顺序注册（灭灯→停音频→背光→屏→电源深睡）
        board.RegisterDevice(&board.led_mock);
        board.RegisterDevice(&board.audio_mock);
        board.RegisterDevice(&board.backlight_mock);
        board.RegisterDevice(&board.display_mock);
        board.RegisterDevice(&board.power_mock);
        app.Initialize();
    }
    void Emit(const InputEvent& ev) { input.Emit(ev); }
};

}  // namespace

TEST_CASE("Init leaves session idle and LED idle", "[application]") {
    Harness h;
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("Boot click starts session and drives LED connecting", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedConnecting),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("Touch click also toggles session", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kTouchButtonId});
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
}

TEST_CASE("Click while active stops session and LED returns idle", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});   // 开始
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});   // 停止
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("UP click raises volume and shows toast", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});
    TEST_ASSERT_EQUAL_INT(80, h.board.audio_mock.last_volume);       // 70 + 10
    TEST_ASSERT_EQUAL_STRING("音量 80%", h.board.display_mock.last_toast.c_str());
}

TEST_CASE("DOWN click lowers volume", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolDown});
    TEST_ASSERT_EQUAL_INT(60, h.board.audio_mock.last_volume);       // 70 - 10
}

TEST_CASE("Volume clamps at 0 and 100", "[application]") {
    Harness h;
    for (int i = 0; i < 10; ++i) {
        h.Emit(InputEvent{InputEvent::kClick, kButtonVolDown});  // 70 → 0
    }
    TEST_ASSERT_EQUAL_INT(0, h.board.audio_mock.last_volume);
    for (int i = 0; i < 15; ++i) {
        h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});    // 0 → 100
    }
    TEST_ASSERT_EQUAL_INT(100, h.board.audio_mock.last_volume);
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});        // 100 不再涨
    TEST_ASSERT_EQUAL_INT(100, h.board.audio_mock.last_volume);
}

TEST_CASE("Boot long press shuts down peripherals then deep sleeps", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kButtonBoot});
    // 应用层只触发板级 EnterSleep()，关断序列委托给板；副作用由 mock 板复刻并逐项断言
    TEST_ASSERT_EQUAL_INT(1, h.board.enter_sleep_calls);
    TEST_ASSERT_EQUAL_INT(1, h.board.power_mock.deep_sleep_calls);
    TEST_ASSERT_EQUAL_INT(static_cast<int>(kyle::LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
    TEST_ASSERT_EQUAL_INT(1, h.board.audio_mock.stop_calls);
    TEST_ASSERT_EQUAL_INT(0, h.board.backlight_mock.last_brightness);
    TEST_ASSERT_EQUAL_INT(1, h.board.display_mock.display_sleep_calls);
}

TEST_CASE("Touch long press does not deep sleep", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kTouchButtonId});
    TEST_ASSERT_EQUAL_INT(0, h.board.power_mock.deep_sleep_calls);
}

TEST_CASE("Unmapped double click is ignored", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kDoubleClick, kButtonBoot});
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());       // 状态不变
    TEST_ASSERT_EQUAL_INT(0, h.board.power_mock.deep_sleep_calls);
}
