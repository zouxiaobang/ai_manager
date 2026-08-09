#ifndef KYLE_BOARDS_SUPERMINI_C3_BOARD_H
#define KYLE_BOARDS_SUPERMINI_C3_BOARD_H

#include <memory>

#include "hal/board.h"

namespace kyle {

// supermini-c3 板组装：NoCodecI2s + Ssd1306Oled + GpioButton + GpioLed + NoPower
class SuperminiC3Board : public IBoard {
public:
    SuperminiC3Board() = default;

    const BoardInfo& info() const override;
    IAudioCodec* audio() override;
    IDisplay* display() override;
    ILed* led() override;
    IInput* input() override;
    IBacklight* backlight() override;  // 无背光，返回 nullptr
    IPower* power() override;
    INetwork* network() override;
    void Init() override;

private:
    std::unique_ptr<IAudioCodec> audio_;
    std::unique_ptr<IDisplay> display_;
    std::unique_ptr<ILed> led_;
    std::unique_ptr<IInput> input_;
    std::unique_ptr<IPower> power_;
    std::unique_ptr<INetwork> network_;
};

}  // namespace kyle

#endif  // KYLE_BOARDS_SUPERMINI_C3_BOARD_H
