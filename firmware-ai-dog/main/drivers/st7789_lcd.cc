#include "drivers/st7789_lcd.h"

namespace xiaozhi {

St7789Lcd::St7789Lcd(int mosi, int clk, int dc, int rst, int cs, int width, int height)
    : mosi_(mosi), clk_(clk), dc_(dc), rst_(rst), cs_(cs),
      width_(width), height_(height) {}

void St7789Lcd::SetStatus(const char* s) {
    (void)s;
    // TODO(driver): esp_lcd 初始化 + LVGL 渲染
}

void St7789Lcd::SetChatMessage(const char* role, const char* text) {
    (void)role;
    (void)text;
    // TODO(driver)
}

void St7789Lcd::SetEmotion(const char* e) {
    (void)e;
    // TODO(driver)
}

void St7789Lcd::ShowToast(const char* msg, int ms) {
    (void)msg;
    (void)ms;
    // TODO(driver)
}

int St7789Lcd::width() const { return width_; }
int St7789Lcd::height() const { return height_; }

}  // namespace xiaozhi
