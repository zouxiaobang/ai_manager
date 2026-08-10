#ifndef KYLE_DRIVERS_CST816S_PARSE_H
#define KYLE_DRIVERS_CST816S_PARSE_H

#include <cstdint>

#include "hal/input.h"

namespace kyle {

// CST816S 触摸解析与手势识别：纯逻辑，零 ESP-IDF 依赖，host 可单测。
// 真机侧由 Cst816sTouch 读取原始字节、提供单调时钟后调用本文件函数。

// 寄存器地址
constexpr uint8_t kCst816sRegTouchData = 0x02;  // 触点状态 + 坐标（0x02 起 5 字节）
constexpr uint8_t kCst816sI2cAddr = 0x38;
// 触摸 button_id 约定见 hal/input.h（kTouchButtonId）

// 触点解析结果
struct TouchPoint {
    bool touched;  // (raw[0] & 0x0F) > 0
    int x;         // 已钳位到 [0, width-1]
    int y;         // 已钳位到 [0, height-1]
};

// 解析寄存器 0x02 起 5 字节触点数据；坐标 12bit，越界按屏幕尺寸钳位。
TouchPoint ParseCst816sPoint(const uint8_t raw[5], int width, int height);

// 软件手势识别：用按压/抬手时序推导单击/双击/长按。
// 为什么不用硬件手势寄存器：CST816S 坐标模式下 0x01 手势寄存器恒为 0
//（真机实测），旧项目 xingzhi 板同样走软件时序。
// 物理按键（GpioButton）复用本类：仅传不同 button_id，时序逻辑完全一致。
class TouchGestureDetector {
public:
    struct Config {
        int single_click_thresh_ms = 200;  // 按压 <= 此值记为单击候选
        int long_press_thresh_ms = 700;    // 按压 >= 此值触发长按（按下沿只发一次）
        int double_click_window_ms = 400;  // 两次单击之间的最大间隔
    };

    TouchGestureDetector();  // 默认阈值 + 触摸 button_id（Config{} 不能在类内作默认实参，见 .cc 委托构造）
    explicit TouchGestureDetector(const Config& cfg);
    // 指定手势事件的 button_id（触摸默认 kTouchButtonId，按键传各自 id）
    TouchGestureDetector(const Config& cfg, int button_id);

    // 每次轮询喂入触点状态与单调时钟(ms)。命中单击/双击/长按时返回 true 并填 out
    //（out 为空仍推进状态机）。同一手势只输出一次（沿触发）。
    bool Update(bool touched, int64_t now_ms, InputEvent* out);

private:
    Config cfg_;
    int button_id_ = kTouchButtonId;
    bool prev_touched_ = false;
    bool long_press_fired_ = false;
    int64_t press_start_ms_ = 0;
    int64_t last_release_ms_ = 0;
    int click_count_ = 0;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_CST816S_PARSE_H
