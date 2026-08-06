#ifndef XIAOZHI_DRIVERS_ST7789_LCD_H
#define XIAOZHI_DRIVERS_ST7789_LCD_H

#include "hal/display.h"

namespace xiaozhi {

// ST7789 SPI LCD 240x240（kyle-s3-lcd 用）。
// 当前为声明式骨架：TODO(driver) 用 esp_lcd_panel + SPI 初始化，再接 LVGL 渲染。
class St7789Lcd : public IDisplay {
public:
    St7789Lcd(int mosi, int clk, int dc, int rst, int cs, int width, int height);
    ~St7789Lcd() override = default;

    void SetStatus(const char* s) override;
    void SetChatMessage(const char* role, const char* text) override;
    void SetEmotion(const char* e) override;
    void ShowToast(const char* msg, int ms) override;
    int width() const override;
    int height() const override;

private:
    int mosi_;
    int clk_;
    int dc_;
    int rst_;
    int cs_;
    int width_;
    int height_;
};

}  // namespace xiaozhi

#endif  // XIAOZHI_DRIVERS_ST7789_LCD_H
