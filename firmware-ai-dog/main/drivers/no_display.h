#ifndef XIAOZHI_DRIVERS_NO_DISPLAY_H
#define XIAOZHI_DRIVERS_NO_DISPLAY_H

#include "hal/display.h"

namespace xiaozhi {

// 无屏占位实现：所有显示调用为空操作。
class NoDisplay : public IDisplay {
public:
    NoDisplay() = default;
    ~NoDisplay() override = default;

    void SetStatus(const char* s) override { (void)s; }
    void SetChatMessage(const char* role, const char* text) override {
        (void)role;
        (void)text;
    }
    void SetEmotion(const char* e) override { (void)e; }
    void ShowToast(const char* msg, int ms) override {
        (void)msg;
        (void)ms;
    }
    int width() const override { return 0; }
    int height() const override { return 0; }
};

}  // namespace xiaozhi

#endif  // XIAOZHI_DRIVERS_NO_DISPLAY_H
