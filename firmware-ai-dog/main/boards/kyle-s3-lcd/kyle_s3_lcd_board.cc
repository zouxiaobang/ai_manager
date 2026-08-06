#include "boards/kyle-s3-lcd/kyle_s3_lcd_board.h"

#include <memory>

#include "boards/kyle-s3-lcd/board_config.h"
#include "drivers/gpio_button.h"
#include "drivers/gpio_led.h"
#include "drivers/no_codec_i2s.h"
#include "drivers/no_network.h"
#include "drivers/no_power.h"
#include "drivers/st7789_lcd.h"

namespace xiaozhi {

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
    // 板级只做组装；TODO(driver): 接入真实 I2S/SPI(I2C)/GPIO 驱动，背光用 LEDC PWM。
    audio_ = std::make_unique<NoCodecI2s>(
        kyle_s3_lcd::kPinMicWs, kyle_s3_lcd::kPinMicSck, kyle_s3_lcd::kPinMicDin,
        kyle_s3_lcd::kPinSpkDout, kyle_s3_lcd::kDefaultInputRate,
        kyle_s3_lcd::kDefaultOutputRate);
    display_ = std::make_unique<St7789Lcd>(
        kyle_s3_lcd::kPinDisplayMosi, kyle_s3_lcd::kPinDisplayClk,
        kyle_s3_lcd::kPinDisplayDc, kyle_s3_lcd::kPinDisplayRst,
        kyle_s3_lcd::kPinDisplayCs, kyle_s3_lcd::kDisplayWidth,
        kyle_s3_lcd::kDisplayHeight);
    led_ = std::make_unique<GpioLed>(kyle_s3_lcd::kPinLed);
    input_ = std::make_unique<GpioButton>(std::vector<int>{
        kyle_s3_lcd::kPinBoot, kyle_s3_lcd::kPinTouchUp, kyle_s3_lcd::kPinTouchDown});
    // TODO(driver): 背光 GpioBacklight(kPinBacklight)；触摸 Cst816s 并入 IInput
    backlight_ = nullptr;
    power_ = std::make_unique<NoPower>();
    network_ = std::make_unique<NoNetwork>();
}

}  // namespace xiaozhi
