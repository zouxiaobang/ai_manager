#include "drivers/ssd1306_oled.h"

namespace kyle {

Ssd1306Oled::Ssd1306Oled(int sda, int scl, int width, int height)
    : sda_(sda), scl_(scl), width_(width), height_(height) {}

void Ssd1306Oled::SetStatus(const char* s) {
    // TODO(driver): I2C 发送 + 本地字模渲染
    (void)s;
}

void Ssd1306Oled::SetChatMessage(const char* role, const char* text) {
    (void)role;
    (void)text;
    // TODO(driver)
}

void Ssd1306Oled::SetEmotion(const char* e) {
    (void)e;
    // TODO(driver)
}

void Ssd1306Oled::Stop() {
    // 下电：关显示。DisplaySleep 当前为空实现，SSD1306 关显示命令留待 TODO(driver)
    DisplaySleep();
}

void Ssd1306Oled::ShowToast(const char* msg, int ms) {
    (void)msg;
    (void)ms;
    // TODO(driver)
}

int Ssd1306Oled::width() const { return width_; }
int Ssd1306Oled::height() const { return height_; }

}  // namespace kyle
