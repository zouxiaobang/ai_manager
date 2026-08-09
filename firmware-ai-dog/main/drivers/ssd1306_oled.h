#ifndef KYLE_DRIVERS_SSD1306_OLED_H
#define KYLE_DRIVERS_SSD1306_OLED_H

#include "hal/display.h"

namespace kyle {

// SSD1306 OLED 128x64（I2C，supermini-c3 用）。
// 当前为声明式骨架：TODO(driver) 实现 I2C 命令/GRAM 上传与字符渲染。
class Ssd1306Oled : public IDisplay {
public:
    Ssd1306Oled(int sda, int scl, int width, int height);
    ~Ssd1306Oled() override = default;

    void SetStatus(const char* s) override;
    void SetChatMessage(const char* role, const char* text) override;
    void SetEmotion(const char* e) override;
    void ShowToast(const char* msg, int ms) override;
    int width() const override;
    int height() const override;

private:
    int sda_;
    int scl_;
    int width_;
    int height_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_SSD1306_OLED_H
