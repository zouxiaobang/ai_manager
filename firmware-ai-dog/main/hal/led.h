#ifndef KYLE_HAL_LED_H
#define KYLE_HAL_LED_H

#include "hal/device.h"

namespace kyle {

// 设备状态 → 指示灯状态联动
enum class LedState {
    kLedIdle,
    kLedConnecting,
    kLedListening,
    kLedSpeaking,
    kLedError,
};

class ILed : public IDevice {
public:
    virtual ~ILed() = default;
    virtual void SetState(LedState s) = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_LED_H
