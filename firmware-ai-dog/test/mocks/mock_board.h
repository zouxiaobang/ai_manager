#ifndef KYLE_TEST_MOCKS_MOCK_BOARD_H
#define KYLE_TEST_MOCKS_MOCK_BOARD_H

#include <functional>
#include <string>
#include <vector>

#include "hal/board.h"

// 内存版板级依赖：记录 audio/led/display/power 调用，供 application host 单测。
// 各 mock 的接口实现只做记录，不涉及真实硬件。mock 设备经能力接口继承 IDevice，
// 与真实驱动一致可注册进 MockBoard 的设备列表（EnterSleep 遍历 Stop() 驱动它们）。

class MockAudio : public kyle::IAudioCodec {
public:
    int last_volume = -1;
    int volume_set_count = 0;
    int stop_calls = 0;
    bool Start() override { return true; }
    void Stop() override { stop_calls++; }
    size_t Read(int16_t* dst, size_t s) override { (void)dst; (void)s; return 0; }
    size_t Write(const int16_t* src, size_t s) override { (void)src; (void)s; return 0; }
    void SetOutputVolume(int v) override {
        last_volume = v;
        volume_set_count++;
    }
    int input_sample_rate() const override { return 16000; }
    int output_sample_rate() const override { return 24000; }
};

class MockLed : public kyle::ILed {
public:
    kyle::LedState last_state = kyle::LedState::kLedIdle;
    int set_count = 0;
    void SetState(kyle::LedState s) override {
        last_state = s;
        set_count++;
    }
    void Stop() override { SetState(kyle::LedState::kLedIdle); }
};

class MockDisplay : public kyle::IDisplay {
public:
    std::string last_toast;
    int display_sleep_calls = 0;
    void SetStatus(const char* s) override { (void)s; }
    void SetChatMessage(const char* r, const char* t) override { (void)r; (void)t; }
    void SetEmotion(const char* e) override { (void)e; }
    void ShowToast(const char* msg, int ms) override {
        (void)ms;
        last_toast = msg ? msg : "";
    }
    int width() const override { return 240; }
    int height() const override { return 240; }
    void DisplaySleep() override { display_sleep_calls++; }
    void Stop() override { DisplaySleep(); }
};

class MockPower : public kyle::IPower {
public:
    int deep_sleep_calls = 0;
    int reboot_calls = 0;
    int battery_percent() const override { return -1; }
    bool is_charging() const override { return false; }
    void DeepSleep() override { deep_sleep_calls++; }
    void Reboot() override { reboot_calls++; }
    void Stop() override { DeepSleep(); }
};

class MockBacklight : public kyle::IBacklight {
public:
    int last_brightness = -1;
    void SetBrightness(int v) override { last_brightness = v; }
    int brightness() const override { return last_brightness; }
    void Stop() override { SetBrightness(0); }
};

class MockNetwork : public kyle::INetwork {
public:
    std::unique_ptr<kyle::IWebSocket> CreateWebSocket(int id) override {
        (void)id;
        return nullptr;
    }
    std::unique_ptr<kyle::IHttp> CreateHttp(int id) override {
        (void)id;
        return nullptr;
    }
};

// 可手动触发的输入源：Application 注册回调后由测试 Emit 事件
class MockInput : public kyle::IInput {
public:
    std::function<void(const kyle::InputEvent&)> cb;
    void OnEvent(std::function<void(const kyle::InputEvent&)> c) override {
        cb = std::move(c);
    }
    void Emit(const kyle::InputEvent& ev) {
        if (cb) cb(ev);
    }
};

class MockBoard : public kyle::IBoard {
public:
    // 成员名带 _mock 后缀：避免与同名接口方法（audio()/led()/...）冲突
    MockAudio audio_mock;
    MockLed led_mock;
    MockDisplay display_mock;
    MockBacklight backlight_mock;
    MockPower power_mock;
    MockNetwork network_mock;
    kyle::IInput* input_source = nullptr;

    const kyle::BoardInfo& info() const override {
        static const kyle::BoardInfo kInfo = {
            .name = "mock",
            .target = "host",
            .flash_size_mb = 0,
            .has_psram = false,
            .has_display = true,
            .has_touch = false,
            .has_battery = false,
            .has_backlight = false,
            .default_input_rate = 16000,
            .default_output_rate = 24000,
        };
        return kInfo;
    }
    kyle::IAudioCodec* audio() override { return &audio_mock; }
    kyle::IDisplay* display() override { return &display_mock; }
    kyle::ILed* led() override { return &led_mock; }
    kyle::IInput* input() override { return input_source; }
    kyle::IBacklight* backlight() override { return &backlight_mock; }
    kyle::IPower* power() override { return &power_mock; }
    kyle::INetwork* network() override { return &network_mock; }
    void Init() override {}
    void RegisterDevice(kyle::IDevice* d) override {
        if (d != nullptr) {
            devices_.push_back(d);
        }
    }
    // 与真实板一致：遍历设备列表调 Stop()，驱动各 mock 记录副作用
    void EnterSleep() override {
        enter_sleep_calls++;
        for (kyle::IDevice* d : devices_) {
            if (d != nullptr) {
                d->Stop();
            }
        }
    }
    int enter_sleep_calls = 0;
    std::vector<kyle::IDevice*> devices_;
};

#endif  // KYLE_TEST_MOCKS_MOCK_BOARD_H
