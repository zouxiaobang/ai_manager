#include "boards/kyle-s3-lcd/kyle_s3_lcd_board.h"

#include <memory>

#include "boards/kyle-s3-lcd/board_config.h"
#include "drivers/cst816s_touch.h"
#include "drivers/gpio_backlight.h"
#include "drivers/gpio_led.h"
#include "drivers/no_codec_i2s.h"
#include "drivers/no_network.h"
#include "drivers/no_power.h"
#include "drivers/st7789_lcd.h"

namespace kyle {

const BoardInfo& KyleS3LcdBoard::info() const {
    static const BoardInfo kInfo = {
        .name = "kyle-s3-lcd",
        .target = "esp32s3",
        .flash_size_mb = 16,
        .has_psram = true,
        .has_display = true,
        .has_touch = true,
        .has_battery = false,
        .has_backlight = true,
        .default_input_rate = kyle_s3_lcd::kDefaultInputRate,
        .default_output_rate = kyle_s3_lcd::kDefaultOutputRate,
    };
    return kInfo;
}

IAudioCodec* KyleS3LcdBoard::audio() { return audio_.get(); }
IDisplay* KyleS3LcdBoard::display() { return display_.get(); }
ILed* KyleS3LcdBoard::led() { return led_.get(); }
IInput* KyleS3LcdBoard::input() { return input_.get(); }
IBacklight* KyleS3LcdBoard::backlight() { return backlight_.get(); }
IPower* KyleS3LcdBoard::power() { return power_.get(); }
INetwork* KyleS3LcdBoard::network() { return network_.get(); }

void KyleS3LcdBoard::Init() {
    // 板级只做组装。K1：LCD(SPI) + 背光(LEDC) 已真实化；音频/输入/网络留后续阶段。
    audio_ = std::make_unique<NoCodecI2s>(
        kyle_s3_lcd::kPinMicWs, kyle_s3_lcd::kPinMicSck, kyle_s3_lcd::kPinMicDin,
        kyle_s3_lcd::kPinSpkDout, kyle_s3_lcd::kDefaultInputRate,
        kyle_s3_lcd::kDefaultOutputRate);

    St7789Config lcd_cfg;
    lcd_cfg.mosi = kyle_s3_lcd::kPinDisplayMosi;
    lcd_cfg.clk = kyle_s3_lcd::kPinDisplayClk;
    lcd_cfg.dc = kyle_s3_lcd::kPinDisplayDc;
    lcd_cfg.rst = kyle_s3_lcd::kPinDisplayRst;
    lcd_cfg.cs = kyle_s3_lcd::kPinDisplayCs;
    lcd_cfg.width = kyle_s3_lcd::kDisplayWidth;
    lcd_cfg.height = kyle_s3_lcd::kDisplayHeight;
    // 先以具体类型初始化（Init 是 St7789Lcd 特有方法），再提升为 IDisplay 接口
    auto lcd = std::make_unique<St7789Lcd>(lcd_cfg);
    lcd->Init();  // SPI 总线 + panel 初始化，画测试图案
    display_ = std::move(lcd);

    led_ = std::make_unique<GpioLed>(kyle_s3_lcd::kPinLed);
    // K2：CST816S 触摸作为 IInput；物理按键（BOOT/UP/DOWN）K4 再聚合进同一接口。
    input_ = std::make_unique<Cst816sTouch>(
        kyle_s3_lcd::kPinTouchSda, kyle_s3_lcd::kPinTouchScl,
        kyle_s3_lcd::kDisplayWidth, kyle_s3_lcd::kDisplayHeight);
    backlight_ = std::make_unique<GpioBacklight>(kyle_s3_lcd::kPinBacklight);
    backlight_->SetBrightness(75);  // 默认亮度，便于观察测试图案
    power_ = std::make_unique<NoPower>();
    network_ = std::make_unique<NoNetwork>();
}

}  // namespace kyle
