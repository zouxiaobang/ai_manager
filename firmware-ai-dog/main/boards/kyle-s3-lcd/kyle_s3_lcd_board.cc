#include "boards/kyle-s3-lcd/kyle_s3_lcd_board.h"

#include <memory>

#include "boards/kyle-s3-lcd/board_config.h"
#include "drivers/composite_input.h"
#include "drivers/cst816s_touch.h"
#include "drivers/gpio_backlight.h"
#include "drivers/gpio_button.h"
#include "drivers/led_strip_led.h"
#include "drivers/network_esp.h"
#include "drivers/no_codec_i2s.h"
#include "drivers/no_power.h"
#include "drivers/provisioning_esp.h"
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
IProvisioningServer* KyleS3LcdBoard::provisioning() { return provisioner_.get(); }

void KyleS3LcdBoard::RegisterDevice(IDevice* device) {
    if (device != nullptr) {
        devices_.push_back(device);
    }
}

void KyleS3LcdBoard::EnterSleep() {
    // 灭灯 → 停音频 → 背光 0 → 屏 DISPOFF → 电源深睡（深睡必须是最后动作）。
    // 长按 BOOT 触发深睡：按注册顺序遍历设备列表调 Stop()，各设备自己做关断。
    // 注册顺序（Init 末尾）即关断顺序：灭灯 → 停音频 → 背光 0 → 屏 DISPOFF →
    // 电源深睡，保证深睡是最后动作（先灭背光再关屏，避免白屏闪黑帧）。
    for (IDevice* d : devices_) {
        if (d != nullptr) {
            d->Stop();
        }
    }
}

void KyleS3LcdBoard::Init() {
    // 板级只做组装。K1-K3：LCD(SPI)+背光、触摸、I2S 直连音频。K4：按键聚合进 IInput + LED。
    audio_ = std::make_unique<NoCodecI2s>(
        kyle_s3_lcd::kPinSpkBclk, kyle_s3_lcd::kPinSpkLrck, kyle_s3_lcd::kPinSpkDout,
        kyle_s3_lcd::kPinMicSck, kyle_s3_lcd::kPinMicWs, kyle_s3_lcd::kPinMicDin,
        kyle_s3_lcd::kDefaultInputRate, kyle_s3_lcd::kDefaultOutputRate);

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

    // GPIO48 是 WS2812 RGB 灯珠（非普通 GPIO 灯），须用 led_strip 驱动
    led_ = std::make_unique<LedStripLed>(kyle_s3_lcd::kPinLed);

    // K2 触摸 + K4 物理按键（BOOT/UP/DOWN）聚合为统一输入源，事件都进 Application
    auto composite = std::make_unique<CompositeInput>();
    composite->Add(std::make_unique<Cst816sTouch>(
        kyle_s3_lcd::kPinTouchSda, kyle_s3_lcd::kPinTouchScl,
        kyle_s3_lcd::kDisplayWidth, kyle_s3_lcd::kDisplayHeight));
    composite->Add(std::make_unique<GpioButton>(std::vector<int>{
        kyle_s3_lcd::kPinBoot, kyle_s3_lcd::kPinTouchUp, kyle_s3_lcd::kPinTouchDown}));
    input_ = std::move(composite);

    backlight_ = std::make_unique<GpioBacklight>(kyle_s3_lcd::kPinBacklight);
    backlight_->SetBrightness(75);  // 默认亮度，便于观察测试图案
    power_ = std::make_unique<NoPower>(kyle_s3_lcd::kPinBoot);  // 深睡由 BOOT 唤醒
    // K5.1：真实网络（esp_wifi STA）。配网信息由 Application 读 NVS/Kconfig 后注入。
    network_ = std::make_unique<NetworkEsp>();
    // K5.6：配网 HTTP 服务（esp_http_server）。Application 进配网模式时 Start + 开热点。
    provisioner_ = std::make_unique<ProvisioningEsp>();

    // 注册进设备列表。
    RegisterDevice(led_.get());
    RegisterDevice(audio_.get());
    RegisterDevice(backlight_.get());
    RegisterDevice(display_.get());
    RegisterDevice(power_.get());
}

}  // namespace kyle
