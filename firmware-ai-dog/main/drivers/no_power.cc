#include "drivers/no_power.h"

// 深睡/重启走 ESP-IDF；host 单测不编译本文件。
#ifdef ESP_PLATFORM
#include "driver/gpio.h"
#include "esp_log.h"
#include "esp_sleep.h"  // 含 esp_deep_sleep_start / esp_sleep_enable_ext0_wakeup
#include "esp_system.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#define TAG "NoPower"

namespace kyle {

NoPower::NoPower(int wake_gpio) : wake_gpio_(wake_gpio) {
#ifdef ESP_PLATFORM
    // 深睡唤醒后打印复位原因，便于真机确认确实从深睡返回（而非意外复位）
    if (esp_reset_reason() == ESP_RST_DEEPSLEEP) {
        ESP_LOGI(TAG, "上次为深睡唤醒（GPIO%d）", wake_gpio_);
    }
#endif
}

int NoPower::battery_percent() const { return -1; }
bool NoPower::is_charging() const { return false; }

void NoPower::DeepSleep() {
#ifdef ESP_PLATFORM
    // 对齐旧项目 kyle-s3-lcd enterDeepSleep 的成熟做法（源码注释原文「等待松开按钮，
    // 避免刚睡就唤醒」）：
    // 长按 BOOT（低有效，按下=低）触发深睡，触发瞬间 BOOT 必为低。ext0 是低电平
    // 唤醒：若深睡瞬间 BOOT 仍被按着（低），会被自身的低电平立即唤醒（长按即重启）。
    // 因此先【无条件】等 BOOT 释放回高，再以 ext0 低电平唤醒深睡——之后 BOOT 高不醒，
    // 保持深睡；再按 BOOT（拉低）才唤醒。正常长按后松手在此停留 <50ms，视觉上
    // Application 已在触发瞬间灭灯，感知为「长按立即进入深睡」。
    // 不能加"按久超时强睡"的兜底——BOOT 仍低时深睡会被自唤醒重启（此前实测根因）。
    ESP_LOGI(TAG, "进入深睡，稍后再按 GPIO%d 唤醒", wake_gpio_);
    while (gpio_get_level(static_cast<gpio_num_t>(wake_gpio_)) == 0) {
        vTaskDelay(pdMS_TO_TICKS(10));
    }
    // 内部上拉：deep sleep 期间保持 BOOT 高电平，确保 ext0 低电平唤醒只在再按时触发
    gpio_pullup_en(static_cast<gpio_num_t>(wake_gpio_));
    esp_sleep_enable_ext0_wakeup(static_cast<gpio_num_t>(wake_gpio_), 0);
    esp_deep_sleep_start();  // 不返回，复位后从 app_main 重启
#endif
}

void NoPower::Stop() {
    // 下电终结动作：进入深睡。须由板把本设备注册为列表最后一个，保证
    // 遍历 Stop() 时所有外设已关断（深睡前先等 BOOT 释放，见 DeepSleep）。
    DeepSleep();
}

void NoPower::Reboot() {
#ifdef ESP_PLATFORM
    esp_restart();
#endif
}

}  // namespace kyle
