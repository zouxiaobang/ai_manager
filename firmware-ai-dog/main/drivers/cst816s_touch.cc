#include "drivers/cst816s_touch.h"

#include "driver/i2c_master.h"
#include "esp_log.h"
#include "esp_timer.h"

#include "drivers/cst816s_parse.h"

// 轮询任务依赖 FreeRTOS；host 单测环境不定义 ESP_PLATFORM，保持 .cc 可纯 C++ 编译。
#ifdef ESP_PLATFORM
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#define TAG "Cst816sTouch"

namespace kyle {

namespace {
// 触摸轮询周期：50Hz 足够跟手，串口轨迹日志也不会刷屏
constexpr int kPollPeriodMs = 20;
// I2C 单次传输超时：100kHz 下 5 字节不到 1ms，50ms 远超传输时间。
// 有界超时避免设备真挂起时任务永久阻塞触发看门狗；
// 待机期读取失败是 CST816S 省电特性（见 PollOnce 注释），与超时无关。
constexpr int kI2cTimeoutMs = 50;
}  // namespace

struct Cst816sTouch::Impl {
    i2c_master_bus_handle_t bus = nullptr;
    i2c_master_dev_handle_t dev = nullptr;
    int width = 0;
    int height = 0;
    std::function<void(const InputEvent&)> cb;
    TouchGestureDetector detector;  // 软件时序手势识别（硬件手势寄存器恒为 0）
#ifdef ESP_PLATFORM
    TaskHandle_t task = nullptr;
    bool task_started = false;
#endif
    bool init_ok = false;
};

Cst816sTouch::Cst816sTouch(int sda, int scl, int width, int height)
    : impl_(std::make_unique<Impl>()) {
    impl_->width = width;
    impl_->height = height;

    i2c_master_bus_config_t bus_cfg = {};
    bus_cfg.i2c_port = I2C_NUM_0;
    bus_cfg.sda_io_num = static_cast<gpio_num_t>(sda);
    bus_cfg.scl_io_num = static_cast<gpio_num_t>(scl);
    bus_cfg.clk_source = I2C_CLK_SRC_DEFAULT;
    esp_err_t ret = i2c_new_master_bus(&bus_cfg, &impl_->bus);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2C 总线初始化失败: %s", esp_err_to_name(ret));
        return;
    }

    i2c_device_config_t dev_cfg = {};
    dev_cfg.dev_addr_length = I2C_ADDR_BIT_LEN_7;
    dev_cfg.device_address = kCst816sI2cAddr;
    dev_cfg.scl_speed_hz = 100000;
    ret = i2c_master_bus_add_device(impl_->bus, &dev_cfg, &impl_->dev);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "触摸设备 0x%02X 注册失败: %s", kCst816sI2cAddr, esp_err_to_name(ret));
        return;
    }
    impl_->init_ok = true;
    ESP_LOGI(TAG, "CST816S 触摸初始化完成（SDA=%d SCL=%d）", sda, scl);
}

Cst816sTouch::~Cst816sTouch() = default;

void Cst816sTouch::OnEvent(std::function<void(const InputEvent&)> cb) {
    impl_->cb = std::move(cb);
#ifdef ESP_PLATFORM
    // 回调注册后再起任务：保证第一次轮询前 cb_ 已就绪，不丢事件。
    if (!impl_->task_started) {
        impl_->task_started = true;
        BaseType_t ok = xTaskCreate(
            [](void* arg) {
                auto* self = static_cast<Cst816sTouch*>(arg);
                while (true) {
                    self->PollOnce();
                    vTaskDelay(pdMS_TO_TICKS(kPollPeriodMs));
                }
            },
            "cst816s", 4096, this, 5, &impl_->task);
        if (ok != pdPASS) {
            impl_->task_started = false;
            ESP_LOGE(TAG, "触摸轮询任务创建失败");
        }
    }
#endif
}

void Cst816sTouch::PollOnce() {
    if (!impl_->init_ok || impl_->dev == nullptr) {
        return;
    }

    uint8_t reg = kCst816sRegTouchData;
    uint8_t raw[5] = {};
    esp_err_t ret = i2c_master_transmit_receive(impl_->dev, &reg, 1, raw, sizeof(raw), kI2cTimeoutMs);
    if (ret != ESP_OK) {
        // CST816S 无触摸一段时间后自动进入待机省电，待机期 I2C 从机不响应（NACK）；
        // 属预期省电行为而非故障，触摸瞬间芯片唤醒即恢复。打 V 级避免刷屏。
        ESP_LOGV(TAG, "触点读取失败: %s", esp_err_to_name(ret));
        return;
    }

    const TouchPoint tp = ParseCst816sPoint(raw, impl_->width, impl_->height);

    // 软件时序识别单击/双击/长按；按 esp_timer 单调时钟喂入。
    // 手势事件打日志，便于真机确认；硬件手势寄存器在坐标模式下恒为 0。
    InputEvent ev{};
    const int64_t now_ms = static_cast<int64_t>(esp_timer_get_time() / 1000);
    if (impl_->detector.Update(tp.touched, now_ms, &ev)) {
        const char* ev_name =
            (ev.type == InputEvent::kClick) ? "单击" :
            (ev.type == InputEvent::kDoubleClick) ? "双击" : "长按";
        ESP_LOGI(TAG, "手势事件: %s (button=%d)", ev_name, ev.button_id);
        if (impl_->cb) {
            impl_->cb(ev);
        }
    }
}

}  // namespace kyle
