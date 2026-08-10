#include "boards/supermini-c3/supermini_c3_board.h"

#include <memory>

#include "boards/supermini-c3/board_config.h"
#include "drivers/gpio_button.h"
#include "drivers/gpio_led.h"
#include "drivers/no_codec_i2s.h"
#include "drivers/no_network.h"
#include "drivers/no_power.h"
#include "drivers/ssd1306_oled.h"

namespace kyle {

const BoardInfo& SuperminiC3Board::info() const {
    static const BoardInfo kInfo = {
        .name = "supermini-c3",
        .target = "esp32c3",
        .flash_size_mb = 4,
        .has_psram = false,
        .has_display = true,
        .has_touch = false,
        .has_battery = false,
        .has_backlight = false,
        .default_input_rate = supermini_c3::kDefaultInputRate,
        .default_output_rate = supermini_c3::kDefaultOutputRate,
    };
    return kInfo;
}

IAudioCodec* SuperminiC3Board::audio() { return audio_.get(); }
IDisplay* SuperminiC3Board::display() { return display_.get(); }
ILed* SuperminiC3Board::led() { return led_.get(); }
IInput* SuperminiC3Board::input() { return input_.get(); }
IBacklight* SuperminiC3Board::backlight() { return nullptr; }
IPower* SuperminiC3Board::power() { return power_.get(); }
INetwork* SuperminiC3Board::network() { return network_.get(); }

void SuperminiC3Board::RegisterDevice(IDevice* device) {
    if (device != nullptr) {
        devices_.push_back(device);
    }
}

void SuperminiC3Board::EnterSleep() {
    // 与 kyle-s3-lcd 相同的遍历关断语义；无背光，注册顺序为 灭灯→停音频→屏关→深睡。
    for (IDevice* d : devices_) {
        if (d != nullptr) {
            d->Stop();
        }
    }
}

void SuperminiC3Board::Init() {
    // 板级只做组装（依赖注入）；TODO(driver): 接入真实 I2S/I2C/GPIO 驱动并初始化总线。
    audio_ = std::make_unique<NoCodecI2s>(
        supermini_c3::kPinSpkBclk, supermini_c3::kPinSpkLrc, supermini_c3::kPinSpkDin,
        supermini_c3::kPinMicSck, supermini_c3::kPinMicWs, supermini_c3::kPinMicDin,
        supermini_c3::kDefaultInputRate, supermini_c3::kDefaultOutputRate);
    display_ = std::make_unique<Ssd1306Oled>(
        supermini_c3::kPinOledSda, supermini_c3::kPinOledScl,
        supermini_c3::kDisplayWidth, supermini_c3::kDisplayHeight);
    led_ = std::make_unique<GpioLed>(supermini_c3::kPinLed);
    input_ = std::make_unique<GpioButton>(
        std::vector<int>{supermini_c3::kPinBoot, supermini_c3::kPinVolumeUp,
                         supermini_c3::kPinVolumeDown});
    power_ = std::make_unique<NoPower>(supermini_c3::kPinBoot);  // 深睡由 BOOT 唤醒
    network_ = std::make_unique<NoNetwork>();

    // 注册进设备列表。注册顺序即下电遍历 Stop() 的顺序：灭灯→停音频→屏关→深睡。
    RegisterDevice(led_.get());
    RegisterDevice(audio_.get());
    RegisterDevice(display_.get());
    RegisterDevice(power_.get());
}

}  // namespace kyle
