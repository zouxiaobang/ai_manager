#include "drivers/led_strip_led.h"

#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "led_strip.h"

// LED 刷新任务依赖 FreeRTOS；host 单测不编译本文件。
#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#define TAG "LedStripLed"

namespace kyle {

namespace {
// 电平刷新周期 50ms：足够表达慢闪/快闪/双闪（最小亮灭段 100ms）
constexpr int kTickPeriodMs = 50;
}  // namespace

struct LedStripLed::Impl {
    int gpio;
    led_strip_handle_t strip = nullptr;  // WS2812 单灯珠句柄
    LedRgb color{0, 0, 0};
    LedPattern pattern = LedPattern::kLedOff;
#ifdef ESP_PLATFORM
    TaskHandle_t task = nullptr;
    bool task_started = false;
#endif
};

LedStripLed::LedStripLed(int gpio) : impl_(std::make_unique<Impl>()) {
    impl_->gpio = gpio;
#ifdef ESP_PLATFORM
    // 与旧项目对齐：WS2812、GRB 顺序、RMT 10MHz、单颗灯珠。
    led_strip_config_t cfg = {};
    cfg.strip_gpio_num = static_cast<gpio_num_t>(gpio);
    cfg.max_leds = 1;
    cfg.color_component_format = LED_STRIP_COLOR_COMPONENT_FMT_GRB;
    cfg.led_model = LED_MODEL_WS2812;
    led_strip_rmt_config_t rmt = {};
    rmt.resolution_hz = 10 * 1000 * 1000;
    ESP_ERROR_CHECK(led_strip_new_rmt_device(&cfg, &rmt, &impl_->strip));
    led_strip_clear(impl_->strip);  // 初始熄灭
#endif
}

LedStripLed::~LedStripLed() {
#ifdef ESP_PLATFORM
    if (impl_->strip != nullptr) {
        led_strip_clear(impl_->strip);
        led_strip_del(impl_->strip);
    }
#endif
}

void LedStripLed::Stop() {
    // 下电：灯珠归空闲态（熄灭），状态由刷新任务下个周期生效
    SetState(LedState::kLedIdle);
}

void LedStripLed::SetState(LedState s) {
    impl_->color = ColorForState(s);
    impl_->pattern = LedPatternForState(s);
#ifdef ESP_PLATFORM
    if (!impl_->task_started) {
        impl_->task_started = true;
        BaseType_t ok = xTaskCreate(
            [](void* arg) {
                auto* self = static_cast<LedStripLed*>(arg);
                while (true) {
                    self->Tick();
                    vTaskDelay(pdMS_TO_TICKS(kTickPeriodMs));
                }
            },
            "led", 5120, this, 3, &impl_->task);
        if (ok != pdPASS) {
            impl_->task_started = false;
            ESP_LOGE(TAG, "LED 刷新任务创建失败");
        }
    }
    // 不在此直接 Tick()：RMT refresh 非线程安全，直接调用会与任务线程并发，破坏
    // rmt 通道状态机（曾实测导致 "channel not in init state"）。状态切换由任务下个
    // 周期（≤50ms）刷新生效，肉眼无感知。
#endif
}

void LedStripLed::Tick() {
#ifdef ESP_PLATFORM
    if (impl_->strip == nullptr) {
        return;
    }
    const int64_t now_ms = static_cast<int64_t>(esp_timer_get_time() / 1000);
    if (ComputeLedLevel(impl_->pattern, now_ms)) {
        led_strip_set_pixel(impl_->strip, 0, impl_->color.r, impl_->color.g, impl_->color.b);
        led_strip_refresh(impl_->strip);
    } else {
        led_strip_clear(impl_->strip);
    }
#endif
}

}  // namespace kyle
