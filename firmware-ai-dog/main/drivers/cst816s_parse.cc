#include "drivers/cst816s_parse.h"

namespace kyle {

TouchPoint ParseCst816sPoint(const uint8_t raw[5], int width, int height) {
    TouchPoint p{};
    if (width <= 0 || height <= 0) {
        return p;
    }
    // 触点数量在低 4 位；坐标高 4 位在偶数下标字节的高半字节，低 8 位在下一字节。
    p.touched = (raw[0] & 0x0F) > 0;
    if (p.touched) {
        const int x = ((raw[1] & 0x0F) << 8) | raw[2];
        const int y = ((raw[3] & 0x0F) << 8) | raw[4];
        // 坐标 12bit 上限 4095，屏幕可能更小，越界钳到 [0, width-1] / [0, height-1]。
        // 值由无符号字节算出恒非负，只需上界钳位。
        p.x = (x >= width) ? (width - 1) : x;
        p.y = (y >= height) ? (height - 1) : y;
    }
    return p;
}

TouchGestureDetector::TouchGestureDetector() : TouchGestureDetector(Config{}) {}

TouchGestureDetector::TouchGestureDetector(const Config& cfg) : cfg_(cfg) {}

bool TouchGestureDetector::Update(bool touched, int64_t now_ms, InputEvent* out) {
    InputEvent dummy{};
    if (out == nullptr) {
        out = &dummy;  // out 为空仍推进状态机，只是丢弃事件
    }

    // 按下沿：记录按压起点，重置长按沿标记
    if (touched && !prev_touched_) {
        press_start_ms_ = now_ms;
        long_press_fired_ = false;
    }
    // 按住中：按压 >= 长按阈值，按下沿触发一次
    if (touched && prev_touched_ && !long_press_fired_ &&
        now_ms - press_start_ms_ >= cfg_.long_press_thresh_ms) {
        long_press_fired_ = true;
        out->type = InputEvent::kLongPress;
        out->button_id = kTouchButtonId;
        prev_touched_ = touched;
        return true;
    }
    // 抬手沿：按压时长 <= 单击阈值记为单击候选；长按/按得太久则清零候选
    if (!touched && prev_touched_) {
        const int64_t held_ms = now_ms - press_start_ms_;
        if (!long_press_fired_ && held_ms <= cfg_.single_click_thresh_ms) {
            click_count_++;
        } else {
            click_count_ = 0;
        }
        last_release_ms_ = now_ms;
    }
    prev_touched_ = touched;

    // 抬起空闲：双击窗口到期后结算单击/双击（结算后 click_count_ 清零，只输出一次）
    if (!touched && click_count_ > 0 &&
        now_ms - last_release_ms_ >= cfg_.double_click_window_ms) {
        if (click_count_ >= 2) {
            out->type = InputEvent::kDoubleClick;
        } else {
            out->type = InputEvent::kClick;
        }
        out->button_id = kTouchButtonId;
        click_count_ = 0;
        return true;
    }
    return false;
}

}  // namespace kyle
