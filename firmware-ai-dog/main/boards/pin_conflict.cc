#include "boards/pin_map.h"

#include <cstdio>

namespace kyle {

namespace {

// ESP32 系列 GPIO 编号上限（含）。C3/S3 均为 0..48，其中部分编号被内部复用禁用。
constexpr int kMaxGpioPin = 48;

constexpr bool GpioValid(int pin) {
    if (pin < 0 || pin > kMaxGpioPin) return false;
    if (pin == 20 || pin == 24) return false;  // 部分芯片内部占用
    if (pin >= 28 && pin <= 31) return false;  // 连接 flash/PSRAM 等
    return true;
}

}  // namespace

bool ValidatePinMap(const PinDef* pins, size_t count, std::string* error) {
    if (pins == nullptr) return false;
    for (size_t i = 0; i < count; ++i) {
        if (pins[i].pin == kPinNc) continue;  // 未连接跳过
        if (!GpioValid(pins[i].pin)) {
            if (error) {
                char buf[128];
                std::snprintf(buf, sizeof(buf), "%s: 引脚 %d 超出合法 GPIO 范围",
                              pins[i].name, pins[i].pin);
                *error = buf;
            }
            return false;
        }
        for (size_t j = i + 1; j < count; ++j) {
            if (pins[j].pin == kPinNc) continue;
            if (pins[i].pin == pins[j].pin && pins[i].group != pins[j].group) {
                if (error) {
                    char buf[160];
                    std::snprintf(buf, sizeof(buf), "%s(引脚 %d) 与 %s 跨组冲突",
                                  pins[i].name, pins[i].pin, pins[j].name);
                    *error = buf;
                }
                return false;
            }
        }
    }
    return true;
}

}  // namespace kyle
