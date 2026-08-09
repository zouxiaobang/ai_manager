#ifndef KYLE_DRIVERS_ST7789_LCD_H
#define KYLE_DRIVERS_ST7789_LCD_H

#include <cstdint>
#include <memory>

#include "hal/display.h"

namespace kyle {

// ST7789 SPI LCD 240x240 面板配置（纯数据，对齐旧 kyle-s3-lcd 硬件参数）。
struct St7789Config {
    int mosi;
    int clk;
    int dc;
    int rst;
    int cs;
    int width;
    int height;
    int pclk_hz = 40 * 1000 * 1000;  // 40MHz
    bool invert = true;              // ST7789 240x240 需反转颜色
    bool swap_xy = false;
    bool mirror_x = false;
    bool mirror_y = false;
};

// ST7789 SPI LCD（kyle-s3-lcd 用）。K1：esp_lcd 初始化 + 测试图案上屏，无 LVGL。
// 头文件不引入 esp_lcd 类型（pimpl），面板句柄藏在 Impl。
class St7789Lcd : public IDisplay {
public:
    explicit St7789Lcd(const St7789Config& cfg);
    ~St7789Lcd() override;

    // 初始化 SPI 总线 + panel，成功后画测试图案并亮背光。
    void Init();

    // 测试图案：0=彩色条，1=棋盘格。
    void ShowPattern(int pattern);

    void SetStatus(const char* s) override;
    void SetChatMessage(const char* role, const char* text) override;
    void SetEmotion(const char* e) override;
    void ShowToast(const char* msg, int ms) override;
    int width() const override;
    int height() const override;

private:
    struct Impl;
    St7789Config cfg_;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_ST7789_LCD_H
