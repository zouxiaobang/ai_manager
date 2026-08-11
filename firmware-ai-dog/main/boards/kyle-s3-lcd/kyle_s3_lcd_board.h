#ifndef KYLE_BOARDS_KYLE_S3_LCD_BOARD_H
#define KYLE_BOARDS_KYLE_S3_LCD_BOARD_H

#include <memory>
#include <vector>

#include "hal/board.h"

namespace kyle {

// kyle-s3-lcd 板组装：NoCodecI2s + St7789Lcd + GpioButton + GpioLed + NoPower
class KyleS3LcdBoard : public IBoard {
public:
    KyleS3LcdBoard() = default;

    const BoardInfo& info() const override;
    IAudioCodec* audio() override;
    IDisplay* display() override;
    ILed* led() override;
    IInput* input() override;
    IBacklight* backlight() override;
    IPower* power() override;
    INetwork* network() override;
    IProvisioningServer* provisioning() override;
    void Init() override;
    void RegisterDevice(IDevice* device) override;
    void EnterSleep() override;

private:
    std::unique_ptr<IAudioCodec> audio_;
    std::unique_ptr<IDisplay> display_;
    std::unique_ptr<ILed> led_;
    std::unique_ptr<IInput> input_;
    std::unique_ptr<IBacklight> backlight_;
    std::unique_ptr<IPower> power_;
    std::unique_ptr<INetwork> network_;
    std::unique_ptr<IProvisioningServer> provisioner_;  // K5.6 配网服务（esp_http_server）
    // 注册的设备指针（板拥有 unique_ptr，此处仅借用；注册顺序即下电关断顺序）
    std::vector<IDevice*> devices_;
};

}  // namespace kyle

#endif  // KYLE_BOARDS_KYLE_S3_LCD_BOARD_H
