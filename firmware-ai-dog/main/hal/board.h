#ifndef KYLE_HAL_BOARD_H
#define KYLE_HAL_BOARD_H

#include "hal/audio_codec.h"
#include "hal/backlight.h"
#include "hal/device.h"
#include "hal/display.h"
#include "hal/input.h"
#include "hal/led.h"
#include "hal/network.h"
#include "hal/power.h"
#include "hal/provisioning.h"

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
    virtual IProvisioningServer* provisioning() = 0;  // 可空（无配网能力的板返回 nullptr）
    virtual void Init() = 0;              // 总线/GPIO/电源初始化
    // 注册设备（LED/功放/麦克风/屏/背光/电源等）进板级列表。注册顺序即整板下电时
    // 遍历调 Stop() 的顺序，板在 Init 里按关断顺序注册（外设先、电源最后）。
    virtual void RegisterDevice(IDevice* device) = 0;
    // 深睡前关断序列：遍历已注册设备调用 Stop()，各设备自己做关断。
    // 顺序敏感约束（先灭背光再关屏、深睡必须是最后动作）由板在注册顺序里保证。
    virtual void EnterSleep() = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_BOARD_H
