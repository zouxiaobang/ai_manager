#ifndef KYLE_BOARDS_KYLE_S3_LCD_BOARD_H
#define KYLE_BOARDS_KYLE_S3_LCD_BOARD_H

#include <memory>

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
    void Init() override;

private:
    std::unique_ptr<IAudioCodec> audio_;
    std::unique_ptr<IDisplay> display_;
    std::unique_ptr<ILed> led_;
    std::unique_ptr<IInput> input_;
    std::unique_ptr<IBacklight> backlight_;
    std::unique_ptr<IPower> power_;
    std::unique_ptr<INetwork> network_;
};

}  // namespace kyle

#endif  // KYLE_BOARDS_KYLE_S3_LCD_BOARD_H
