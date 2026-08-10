#include "drivers/gpio_led.h"

#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_timer.h"

// LED 刷新任务依赖 FreeRTOS；host 单测不编译本文件（纯逻辑在 led_pattern.h/.cc）。
#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#define TAG "GpioLed"

namespace kyle {

namespace {
// 电平刷新周期：50ms 足够表达慢闪/快闪/双闪（最小亮灭段 100ms）
constexpr int kTickPeriodMs = 50;
}  // namespace

struct GpioLed::Impl {
    int pin;
    LedPattern pattern = LedPattern::kLedOff;
#ifdef ESP_PLATFORM
    TaskHandle_t task = nullptr;
    bool task_started = false;
#endif
};

GpioLed::GpioLed(int pin) : impl_(std::make_unique<Impl>()) {
    impl_->pin = pin;
#ifdef ESP_PLATFORM
    gpio_config_t cfg = {};
    cfg.pin_bit_mask = 1ULL << pin;
    cfg.mode = GPIO_MODE_OUTPUT;
    cfg.pull_up_en = GPIO_PULLUP_DISABLE;
    cfg.pull_down_en = GPIO_PULLDOWN_DISABLE;
    cfg.intr_type = GPIO_INTR_DISABLE;
    gpio_config(&cfg);
    gpio_set_level(static_cast<gpio_num_t>(pin), 0);  // 初始熄灭
#endif
}

GpioLed::~GpioLed() = default;

void GpioLed::Stop() {
    // 下电：状态灯归空闲态（熄灭），电平由刷新任务下个周期置低
    SetState(LedState::kLedIdle);
}

void GpioLed::SetState(LedState s) {
    impl_->pattern = LedPatternForState(s);
#ifdef ESP_PLATFORM
    if (!impl_->task_started) {
        impl_->task_started = true;
        BaseType_t ok = xTaskCreate(
            [](void* arg) {
                auto* self = static_cast<GpioLed*>(arg);
                while (true) {
                    self->Tick();
                    vTaskDelay(pdMS_TO_TICKS(kTickPeriodMs));
                }
            },
            "led", 2048, this, 3, &impl_->task);
        if (ok != pdPASS) {
            impl_->task_started = false;
            ESP_LOGE(TAG, "LED 刷新任务创建失败");
        }
    }
    // 不在此直接 Tick()：与 LedStripLed 一致，避免主线程与任务并发刷新。
    // 状态切换由任务下个周期（≤50ms）刷新生效。
#endif
}

void GpioLed::Tick() {
#ifdef ESP_PLATFORM
    const int64_t now_ms = static_cast<int64_t>(esp_timer_get_time() / 1000);
    gpio_set_level(static_cast<gpio_num_t>(impl_->pin),
                   ComputeLedLevel(impl_->pattern, now_ms) ? 1 : 0);
#endif
}

}  // namespace kyle
