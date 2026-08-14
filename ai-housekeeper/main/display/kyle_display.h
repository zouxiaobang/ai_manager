#pragma once

#include "gif/lvgl_gif.h"
#include "lvgl_display.h"

#include <esp_lcd_panel_io.h>
#include <esp_lcd_panel_ops.h>
#include <font_emoji.h>
#include <atomic>
#include <memory>

#include "KyleTouchMenuUI.h"
#include "kyle_home_ui.h"
#include "kyle_menu_ui.h"

#define PREVIEW_IMAGE_DURATION_MS 5000
enum class ToastTime { SHORT, NORMAL, LONG };
struct EmotionItem {
    const char* name;
    const lv_img_dsc_t* img;
};

class KyleLcdDisplay : public LvglDisplay {
protected:
    std::string emotion_type_ = "";
    std::string home_type_ = "none";
    std::unordered_map<std::string, std::vector<EmotionItem>> all_emotions_type_;
    esp_lcd_panel_io_handle_t panel_io_ = nullptr;
    esp_lcd_panel_handle_t panel_ = nullptr;

    lv_draw_buf_t draw_buf_;
    lv_obj_t* container_ = nullptr;
    // 顶部信息栏模块--wifi、电量等信息
    lv_obj_t* top_bar_ = nullptr;
    // 通知信息栏模块--通知、状态等信息
    lv_obj_t* status_bar_ = nullptr;
    // 主内容区域栏模块--聊天、表情、图片等信息
    lv_obj_t* content_ = nullptr;
    // 底部信息栏模块--默认聊天信息
    lv_obj_t* bottom_bar_ = nullptr;
    // 图片预览
    lv_obj_t* preview_image_ = nullptr;
    // 表情符号（字符）
    lv_obj_t* emoji_label_ = nullptr;
    // 表情图片（图片资源）-- in emoji_collection or gif_controller_
    lv_obj_t* emoji_image_ = nullptr;
    // GIF动图控制器
    std::unique_ptr<LvglGif> gif_controller_ = nullptr;
    // 表情区域 -- 显示表情
    lv_obj_t* emoji_box_ = nullptr;
    // 聊天区域 -- 显示聊天信息
    lv_obj_t* chat_message_label_ = nullptr;
    // 图片预览超时器 -- 定时关闭预览图片
    esp_timer_handle_t preview_timer_ = nullptr;
    // 预览图片缓存
    std::unique_ptr<LvglImage> preview_image_cache_ = nullptr;
    // 是否隐藏聊天内容
    bool hide_subtitle_ = false;

    // 初始化主题 --  黑暗、亮模式
    void InitializeTheme();

    // 加锁 -- 继承父类
    virtual bool Lock(int timeout_ms = 0) override;
    // 解锁 -- 继承父类
    virtual void Unlock() override;

protected:
    KyleLcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
                   int height);

public:
    ~KyleLcdDisplay();

    // 重写父类方法
    void SetEmotion(const char* emotion) override;
    void SetChatMessage(const char* role, const char* content) override;
    void SetPreviewImage(std::unique_ptr<LvglImage> image) override;
    void SetTheme(Theme* theme) override;

    // 开关聊天内容显示
    void SetHideSubtitle(bool hide);
    void SetEmotionType(const std::string& emotion_type);
    // Toast显示通知信息
    void ShowToast(const char* message, ToastTime toast_time);
    virtual void toggleTheme(const char* theme_name);
    // 设置UI
    void SetupUI();
    // 自定义表情
    void CustomEmoji();
};

class KyleV1Display : public KyleLcdDisplay {
private:
    KyleTouchMenuUI* menuUi_ = nullptr;
    HomeUI* homeUi_ = nullptr;
    // 是否展示Home页面
    bool isHideHomePage() const;

public:
    KyleV1Display(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
                  int height);

    void SetStatus(const char* status) override;

    void initMenu();
    void hideMenu();
    // 输入接口（给按键/旋钮用）
    void onKeyNext();
    void onKeyPrev();
    void onKeyEnter();
    void onKeyBack();
    void showMenu(std::vector<MenuItem> menu);
    bool isMenuOpened();

    // 显示Home页面
    void setHomeType(const std::string& home_type);
    void resetHomeUI();
    void showHome();
    void hideHome();
    void toggleTheme(const char* theme_name) override;
};

class KyleV2Display : public KyleV1Display {
public:
    KyleV2Display(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int touchSda,
                  int touchScl, int width, int height);
    void registerTouchV9();
    void initTouch();
    bool getTouch(int* x, int* y);

private:
    i2c_master_bus_handle_t busHandle_;
    i2c_master_dev_handle_t devHandle_;
    int touchSda_;
    int touchScl_;
};