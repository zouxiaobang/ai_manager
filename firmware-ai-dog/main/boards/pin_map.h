#ifndef XIAOZHI_BOARDS_PIN_MAP_H
#define XIAOZHI_BOARDS_PIN_MAP_H

// 板级声明式引脚表：纯数据，不依赖 ESP-IDF，可在 host 上做冲突校验。

#include <cstddef>
#include <string>

namespace xiaozhi {

// 未连接（等价于原项目 GPIO_NUM_NC 语义），校验时跳过
constexpr int kPinNc = -1;

struct PinDef {
    const char* name;  // 功能名，如 "mic_ws"
    int pin;           // GPIO 编号；kPinNc 表示未连接
    int group;         // 总线/功能组：同一组内同号引脚视为合法共享（如 I2S 分时）
};

// 声明式校验：检测 ① 跨组引脚复用冲突 ② 超范围/负值误用（kPinNc 除外）。
// 返回 true 表示无冲突；error 非空时写入第一条冲突描述。
bool ValidatePinMap(const PinDef* pins, size_t count, std::string* error = nullptr);

}  // namespace xiaozhi

#endif  // XIAOZHI_BOARDS_PIN_MAP_H
