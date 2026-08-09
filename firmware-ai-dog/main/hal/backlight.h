#ifndef KYLE_HAL_BACKLIGHT_H
#define KYLE_HAL_BACKLIGHT_H

namespace kyle {

// 屏幕背光控制（kyle-s3-lcd 有，supermini-c3 OLED 常亮可空）
class IBacklight {
public:
    virtual ~IBacklight() = default;
    virtual void SetBrightness(int percent) = 0;  // 0..100
    virtual int brightness() const = 0;
};

}  // namespace kyle

#endif  // KYLE_HAL_BACKLIGHT_H
