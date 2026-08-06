#ifndef XIAOZHI_HAL_LED_H
#define XIAOZHI_HAL_LED_H

namespace xiaozhi {

// 设备状态 → 指示灯状态联动
enum class LedState {
    kLedIdle,
    kLedConnecting,
    kLedListening,
    kLedSpeaking,
    kLedError,
};

class ILed {
public:
    virtual ~ILed() = default;
    virtual void SetState(LedState s) = 0;
};

}  // namespace xiaozhi

#endif  // XIAOZHI_HAL_LED_H
