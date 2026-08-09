#ifndef KYLE_HAL_BOARD_H
#define KYLE_HAL_BOARD_H

#include "hal/audio_codec.h"
#include "hal/backlight.h"
#include "hal/display.h"
#include "hal/input.h"
#include "hal/led.h"
#include "hal/network.h"
#include "hal/power.h"

namespace kyle {

// 板级只读信息（Kconfig 选择后，由具体板子提供）
struct BoardInfo {
    const char* name;       // "supermini-c3"
    const char* target;     // "esp32c3"
    int  flash_size_mb;
    bool has_psram;
    bool has_display;
    bool has_touch;
    bool has_battery;
    bool has_backlight;
    int  default_input_rate;   // 麦克风采样率
    int  default_output_rate;  // 扬声器采样率
};

// 板级组装结果：一块板 = 一份静态配置 + 依赖注入
class IBoard {
public:
    virtual ~IBoard() = default;

    virtual const BoardInfo& info() const = 0;
    virtual IAudioCodec* audio() = 0;
    virtual IDisplay* display() = 0;
    virtual ILed* led() = 0;
    virtual IInput* input() = 0;
    virtual IBacklight* backlight() = 0;  // 可空
    virtual IPower* power() = 0;          // 可空
    virtual INetwork* network() = 0;
    virtual void Init() = 0;              // 总线/GPIO/电源初始化
};

}  // namespace kyle

#endif  // KYLE_HAL_BOARD_H
