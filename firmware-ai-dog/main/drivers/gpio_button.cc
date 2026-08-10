#include "drivers/gpio_button.h"

#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_timer.h"

#include "drivers/cst816s_parse.h"  // TouchGestureDetector（复用触摸的软件手势时序）

// 轮询任务依赖 FreeRTOS；host 单测不编译本文件（纯逻辑在 gpio_button.h 的 DebounceFilter）。
#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#define TAG "GpioButton"

namespace kyle {

namespace {
// 轮询周期 10ms：消抖 3 次采样（30ms）足够滤除机械抖动
constexpr int kPollPeriodMs = 10;
// 按键长按阈值：与触摸一致 700ms（长按 BOOT 触发深睡）
constexpr int kLongPressThreshMs = 700;
}  // namespace

struct GpioButton::Impl {
    std::vector<int> pins;
    std::function<void(const InputEvent&)> cb;
    std::vector<TouchGestureDetector> detectors;  // 每键一个，button_id 按 pins 顺序
    std::vector<DebounceFilter> debouncers;
#ifdef ESP_PLATFORM
    TaskHandle_t task = nullptr;
    bool task_started = false;
#endif
};

GpioButton::GpioButton(const std::vector<int>& pins)
    : impl_(std::make_unique<Impl>()) {
    impl_->pins = pins;
#ifdef ESP_PLATFORM
    // 低有效按键：输入 + 内部上拉，按下拉低；按键由轮询读取，无需 GPIO 中断。
    for (int pin : pins) {
        gpio_config_t cfg = {};
        cfg.pin_bit_mask = 1ULL << pin;
        cfg.mode = GPIO_MODE_INPUT;
        cfg.pull_up_en = GPIO_PULLUP_ENABLE;
        cfg.pull_down_en = GPIO_PULLDOWN_DISABLE;
        cfg.intr_type = GPIO_INTR_DISABLE;
        gpio_config(&cfg);
    }
#endif
    // 每个按键一个手势检测器；长按阈值对按键统一用 700ms
    TouchGestureDetector::Config dcfg;
    dcfg.long_press_thresh_ms = kLongPressThreshMs;
    for (size_t i = 0; i < pins.size(); ++i) {
        impl_->detectors.emplace_back(dcfg, static_cast<int>(i));
        impl_->debouncers.emplace_back();
    }
}

GpioButton::~GpioButton() = default;

void GpioButton::OnEvent(std::function<void(const InputEvent&)> cb) {
    impl_->cb = std::move(cb);
#ifdef ESP_PLATFORM
    // 回调注册后再起任务：保证第一次轮询前 cb_ 已就绪，不丢事件。
    if (!impl_->task_started) {
        impl_->task_started = true;
        BaseType_t ok = xTaskCreate(
            [](void* arg) {
                auto* self = static_cast<GpioButton*>(arg);
                while (true) {
                    self->PollOnce();
                    vTaskDelay(pdMS_TO_TICKS(kPollPeriodMs));
                }
            },
            "gpio_btn", 4096, this, 5, &impl_->task);
        if (ok != pdPASS) {
            impl_->task_started = false;
            ESP_LOGE(TAG, "按键轮询任务创建失败");
        }
    }
#endif
}

void GpioButton::PollOnce() {
    if (!impl_->cb || impl_->pins.empty()) {
        return;
    }
    const int64_t now_ms = static_cast<int64_t>(esp_timer_get_time() / 1000);
    for (size_t i = 0; i < impl_->pins.size(); ++i) {
        // 低有效：GPIO 低电平 = 按下
        const bool raw_pressed =
#ifdef ESP_PLATFORM
            gpio_get_level(static_cast<gpio_num_t>(impl_->pins[i])) == 0;
#else
            false;
#endif
        const bool pressed = impl_->debouncers[i].Update(raw_pressed);
        InputEvent ev{};
        if (impl_->detectors[i].Update(pressed, now_ms, &ev)) {
            const char* ev_name =
                (ev.type == InputEvent::kClick) ? "单击" :
                (ev.type == InputEvent::kDoubleClick) ? "双击" : "长按";
            ESP_LOGI(TAG, "按键事件: %s (button=%d)", ev_name, ev.button_id);
            if (impl_->cb) {
                impl_->cb(ev);
            }
        }
    }
}

}  // namespace kyle
