#ifndef XIAOZHI_HAL_DISPLAY_H
#define XIAOZHI_HAL_DISPLAY_H

namespace xiaozhi {

// 显示能力抽象：LVGL 屏 / OLED / 无屏统一走这套接口。
class IDisplay {
public:
    virtual ~IDisplay() = default;

    virtual void SetStatus(const char* s) = 0;
    virtual void SetChatMessage(const char* role, const char* text) = 0;
    virtual void SetEmotion(const char* e) = 0;
    virtual void ShowToast(const char* msg, int ms) = 0;
    virtual int width() const = 0;
    virtual int height() const = 0;
};

}  // namespace xiaozhi

#endif  // XIAOZHI_HAL_DISPLAY_H
