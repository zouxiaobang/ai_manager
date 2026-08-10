#ifndef KYLE_DRIVERS_GPIO_BUTTON_H
#define KYLE_DRIVERS_GPIO_BUTTON_H

#include <functional>
#include <memory>
#include <vector>

#include "hal/input.h"

namespace kyle {

// GPIO 消抖（纯逻辑，host 可测）：连续 stable_samples 次采样一致才切换稳定电平。
// 抖动（采样中途反跳）会取消本次确认，回到原稳定电平。
class DebounceFilter {
public:
    explicit DebounceFilter(int stable_samples = 3) : stable_samples_(stable_samples) {}

    // 喂入单次原始采样（true=按下），返回当前稳定电平；本函数状态与 GPIO 电平分离，
    // 采样周期固定（GpioButton 10ms），消抖时长 ≈ stable_samples × 周期。
    bool Update(bool raw) {
        if (!pending_ && raw == stable_) {
            return stable_;  // 与稳定电平一致，无变化
        }
        if (!pending_) {
            // 首次检测到与稳定电平不同：进入确认，但变化沿本身不计为一次一致采样
            pending_ = true;
            pending_level_ = raw;
            count_ = 0;
        } else if (raw == pending_level_) {
            if (++count_ >= stable_samples_) {
                stable_ = pending_level_;
                pending_ = false;
                count_ = 0;
            }
        } else {
            // 反方向抖动，取消确认
            pending_ = false;
            count_ = 0;
        }
        return stable_;
    }

    bool stable() const { return stable_; }

private:
    int stable_samples_;
    bool stable_ = false;       // 当前确认的稳定电平（true=按下）
    bool pending_ = false;      // 是否有待确认的电平变化
    bool pending_level_ = false;
    int count_ = 0;             // pending 电平连续一致次数
};

// GPIO 按键输入（BOOT / UP / DOWN）：轮询读电平 + 软件消抖 + 单击/双击/长按识别。
// 低有效（按下=0），内部上拉；pins 顺序即 button_id（0/1/2...，对齐 hal/input.h 约定）。
class GpioButton : public IInput {
public:
    explicit GpioButton(const std::vector<int>& pins);
    ~GpioButton() override;

    void OnEvent(std::function<void(const InputEvent&)> cb) override;

private:
    void PollOnce();

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_GPIO_BUTTON_H
