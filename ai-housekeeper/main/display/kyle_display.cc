#include "kyle_display.h"
#include "assets/lang_config.h"
#include "gif/lvgl_gif.h"
#include "lvgl_theme.h"
#include "settings.h"

#include <esp_err.h>
#include <esp_log.h>
#include <esp_lvgl_port.h>
#include <esp_psram.h>
#include <font_awesome.h>
#include <algorithm>
#include <cstring>
#include <vector>

#include "application.h"
#include "driver/i2c_master.h"
#include "esp_heap_caps.h"
#include "ImageLoader.h"


#define TAG "KyleDisplay"
#define TAG_V1 "KyleDisplay-V1"
// 字体定义
LV_FONT_DECLARE(BUILTIN_TEXT_FONT);
LV_FONT_DECLARE(BUILTIN_ICON_FONT);
LV_FONT_DECLARE(font_awesome_30_4);

KyleLcdDisplay::KyleLcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel,
                               int width, int height)
    : panel_io_(panel_io), panel_(panel) {
    width_ = width;
    height_ = height;
    InitializeTheme();
    // 创建预览图片的timer
    esp_timer_create_args_t preview_timer_args = {
        .callback =
            [](void* arg) {
                KyleLcdDisplay* display = static_cast<KyleLcdDisplay*>(arg);
                display->SetPreviewImage(nullptr);
            },
        .arg = this,
        .dispatch_method = ESP_TIMER_TASK,
        .name = "preview_timer",
        .skip_unhandled_events = false,
    };
    esp_timer_create(&preview_timer_args, &preview_timer_);
}
// 析构
KyleLcdDisplay::~KyleLcdDisplay() {
    SetPreviewImage(nullptr);
    if (gif_controller_) {
        gif_controller_->Stop();
        gif_controller_.reset();
    }
    if (preview_timer_ != nullptr) {
        esp_timer_stop(preview_timer_);
        esp_timer_delete(preview_timer_);
    }

    if (preview_image_ != nullptr) {
        lv_obj_del(preview_image_);
    }
    if (chat_message_label_ != nullptr) {
        lv_obj_del(chat_message_label_);
    }
    if (emoji_label_ != nullptr) {
        lv_obj_del(emoji_label_);
    }
    if (emoji_image_ != nullptr) {
        lv_obj_del(emoji_image_);
    }
    if (emoji_box_ != nullptr) {
        lv_obj_del(emoji_box_);
    }
    if (content_ != nullptr) {
        lv_obj_del(content_);
    }
    if (bottom_bar_ != nullptr) {
        lv_obj_del(bottom_bar_);
    }
    if (status_bar_ != nullptr) {
        lv_obj_del(status_bar_);
    }
    if (top_bar_ != nullptr) {
        lv_obj_del(top_bar_);
    }
    if (container_ != nullptr) {
        lv_obj_del(container_);
    }
    if (display_ != nullptr) {
        lv_display_delete(display_);
    }

    if (panel_ != nullptr) {
        esp_lcd_panel_del(panel_);
    }
    if (panel_io_ != nullptr) {
        esp_lcd_panel_io_del(panel_io_);
    }
}
void KyleLcdDisplay::InitializeTheme() {
    auto text_font = std::make_shared<LvglBuiltInFont>(&BUILTIN_TEXT_FONT);
    auto icon_font = std::make_shared<LvglBuiltInFont>(&BUILTIN_ICON_FONT);
    auto large_icon_font = std::make_shared<LvglBuiltInFont>(&font_awesome_30_4);

    // 初始化Light主题
    auto light_theme = new LvglTheme("light");
    light_theme->set_background_color(lv_color_hex(0xFFFFFF));
    light_theme->set_text_color(lv_color_hex(0x000000));
    light_theme->set_chat_background_color(lv_color_hex(0xE0E0E0));
    light_theme->set_user_bubble_color(lv_color_hex(0x00FF00));
    light_theme->set_assistant_bubble_color(lv_color_hex(0xDDDDDD));
    light_theme->set_system_bubble_color(lv_color_hex(0xFFFFFF));
    light_theme->set_system_text_color(lv_color_hex(0x000000));
    light_theme->set_border_color(lv_color_hex(0x000000));
    light_theme->set_low_battery_color(lv_color_hex(0x000000));
    light_theme->set_text_font(text_font);
    light_theme->set_icon_font(icon_font);
    light_theme->set_large_icon_font(large_icon_font);

    // 初始化Dark主题
    auto dark_theme = new LvglTheme("dark");
    dark_theme->set_background_color(lv_color_hex(0x000000));
    dark_theme->set_text_color(lv_color_hex(0xFFFFFF));
    dark_theme->set_chat_background_color(lv_color_hex(0x1F1F1F));
    dark_theme->set_user_bubble_color(lv_color_hex(0x00FF00));
    dark_theme->set_assistant_bubble_color(lv_color_hex(0x222222));
    dark_theme->set_system_bubble_color(lv_color_hex(0x000000));
    dark_theme->set_system_text_color(lv_color_hex(0xFFFFFF));
    dark_theme->set_border_color(lv_color_hex(0xFFFFFF));
    dark_theme->set_low_battery_color(lv_color_hex(0xFF0000));
    dark_theme->set_text_font(text_font);
    dark_theme->set_icon_font(icon_font);
    dark_theme->set_large_icon_font(large_icon_font);

    // 注册主题
    auto& theme_manager = LvglThemeManager::GetInstance();
    theme_manager.RegisterTheme("light", light_theme);
    theme_manager.RegisterTheme("dark", dark_theme);

    // 主题读取
    // 没有设置时默认Light主题
    std::string theme_name = Settings("display", false).GetString("theme", "light");
    current_theme_ = theme_manager.GetTheme(theme_name);
}

bool KyleLcdDisplay::Lock(int timeout_ms) { return lvgl_port_lock(timeout_ms); }
void KyleLcdDisplay::Unlock() { lvgl_port_unlock(); }
static uint8_t* decoded_bg_buf = nullptr;
static lv_image_dsc_t bg_native_dsc;
void KyleLcdDisplay::SetupUI() {
    ESP_LOGI(TAG, "配置UI界面");
    DisplayLockGuard lock(this);

    LvglTheme* lvgl_theme = static_cast<LvglTheme*>(current_theme_);
    auto text_font = lvgl_theme->text_font()->font();
    auto icon_font = lvgl_theme->icon_font()->font();
    auto large_icon_font = lvgl_theme->large_icon_font()->font();

    auto screen = lv_screen_active();
    lv_obj_set_style_text_font(screen, text_font, 0);
    lv_obj_set_style_text_color(screen, lvgl_theme->text_color(), 0);
    lv_obj_set_style_bg_color(screen, lvgl_theme->background_color(), 0);

    // 配置背景框架 -- 全屏
    container_ = lv_obj_create(screen);
    lv_obj_set_size(container_, LV_HOR_RES, LV_VER_RES);
    lv_obj_set_style_radius(container_, 0, 0);
    lv_obj_set_style_pad_all(container_, 0, 0);
    lv_obj_set_style_border_width(container_, 0, 0);
    lv_obj_set_style_bg_color(container_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_border_color(container_, lvgl_theme->border_color(), 0);

    lv_obj_t* bg = lv_image_create(container_);
    lv_obj_set_size(bg, LV_HOR_RES, LV_VER_RES);

    auto bg_img = ImageLoader::GetImage("bg.png", 240, 240);
    if (bg_img) {
        lv_image_set_src(bg, bg_img);
    }

    // 配置表情区域 -- 中间区域
    emoji_box_ = lv_obj_create(screen);
    lv_obj_set_size(emoji_box_, LV_SIZE_CONTENT, LV_SIZE_CONTENT);
    lv_obj_set_style_bg_opa(emoji_box_, LV_OPA_TRANSP, 0);
    lv_obj_set_style_pad_all(emoji_box_, 0, 0);
    lv_obj_set_style_border_width(emoji_box_, 0, 0);
    lv_obj_align(emoji_box_, LV_ALIGN_CENTER, 0, 0);
    // 配置文字表情
    emoji_label_ = lv_label_create(emoji_box_);
    lv_obj_set_style_text_font(emoji_label_, large_icon_font, 0);
    lv_obj_set_style_text_color(emoji_label_, lvgl_theme->text_color(), 0);
    lv_label_set_text(emoji_label_, FONT_AWESOME_MICROCHIP_AI);
    // 配置图片表情
    emoji_image_ = lv_img_create(emoji_box_);
    lv_obj_center(emoji_image_);
    lv_obj_set_style_bg_opa(emoji_image_, LV_OPA_TRANSP, 0);
    lv_obj_add_flag(emoji_image_, LV_OBJ_FLAG_HIDDEN);

    // 配置预览图片区域 -- 居中
    preview_image_ = lv_img_create(screen);
    lv_obj_set_size(preview_image_, width_ / 2, height_ / 2);
    lv_obj_align(preview_image_, LV_ALIGN_CENTER, 0, 0);
    lv_obj_add_flag(preview_image_, LV_OBJ_FLAG_HIDDEN);

    // 配置顶部状态栏
    top_bar_ = lv_obj_create(screen);
    lv_obj_set_size(top_bar_, LV_HOR_RES, LV_SIZE_CONTENT);
    lv_obj_set_style_radius(top_bar_, 0, 0);
    lv_obj_set_style_bg_opa(top_bar_, LV_OPA_50, 0);
    lv_obj_set_style_bg_color(top_bar_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_border_width(top_bar_, 0, 0);
    lv_obj_set_style_pad_all(top_bar_, 0, 0);
    lv_obj_set_style_pad_top(top_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_style_pad_bottom(top_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_style_pad_left(top_bar_, lvgl_theme->spacing(4), 0);
    lv_obj_set_style_pad_right(top_bar_, lvgl_theme->spacing(4), 0);
    lv_obj_set_flex_flow(top_bar_, LV_FLEX_FLOW_ROW);
    lv_obj_set_flex_align(top_bar_, LV_FLEX_ALIGN_SPACE_BETWEEN, LV_FLEX_ALIGN_CENTER,
                          LV_FLEX_ALIGN_CENTER);
    lv_obj_set_scrollbar_mode(top_bar_, LV_SCROLLBAR_MODE_OFF);
    lv_obj_align(top_bar_, LV_ALIGN_TOP_MID, 0, 0);
    // 左侧WIFI图标
    network_label_ = lv_label_create(top_bar_);
    lv_label_set_text(network_label_, "");
    lv_obj_set_style_text_font(network_label_, text_font, 0);
    lv_obj_set_style_text_color(network_label_, lvgl_theme->text_color(), 0);
    // 中间状态栏
    status_label_ = lv_label_create(top_bar_);
    lv_obj_set_size(status_label_, LV_SIZE_CONTENT, LV_SIZE_CONTENT);
    lv_label_set_text(status_label_, "");
    lv_obj_align(status_label_, LV_ALIGN_CENTER, 0, 0);
    // 右侧图标区域
    lv_obj_t* right_icons = lv_obj_create(top_bar_);
    lv_obj_set_size(right_icons, LV_SIZE_CONTENT, LV_SIZE_CONTENT);
    lv_obj_set_style_bg_opa(right_icons, LV_OPA_TRANSP, 0);
    lv_obj_set_style_pad_all(right_icons, 0, 0);
    lv_obj_set_style_border_width(right_icons, 0, 0);
    lv_obj_set_flex_flow(right_icons, LV_FLEX_FLOW_ROW);
    lv_obj_set_flex_align(right_icons, LV_FLEX_ALIGN_END, LV_FLEX_ALIGN_CENTER,
                          LV_FLEX_ALIGN_CENTER);
    // 静音图标
    mute_label_ = lv_label_create(right_icons);
    lv_label_set_text(mute_label_, "");
    lv_obj_set_style_text_font(mute_label_, icon_font, 0);
    lv_obj_set_style_text_color(mute_label_, lvgl_theme->text_color(), 0);
    // 电量图标
    battery_label_ = lv_label_create(right_icons);
    lv_label_set_text(battery_label_, "");
    lv_obj_set_style_text_font(battery_label_, icon_font, 0);
    lv_obj_set_style_text_color(battery_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_margin_left(battery_label_, lvgl_theme->spacing(2), 0);

    // 状态提示区域 -- 直接重叠在所有层面之上
    status_bar_ = lv_obj_create(screen);
    lv_obj_set_size(status_bar_, LV_HOR_RES, LV_SIZE_CONTENT);
    lv_obj_set_style_radius(status_bar_, 0, 0);
    // lv_obj_set_style_bg_opa(status_bar_, LV_OPA_TRANSP, 0);
    lv_obj_set_style_border_width(status_bar_, 0, 0);
    lv_obj_set_style_pad_all(status_bar_, 0, 0);
    lv_obj_set_style_pad_top(status_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_style_pad_bottom(status_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_scrollbar_mode(status_bar_, LV_SCROLLBAR_MODE_OFF);
    lv_obj_set_style_layout(status_bar_, LV_LAYOUT_NONE, 0);
    lv_obj_align(status_bar_, LV_ALIGN_BOTTOM_MID, 0, 0);
    lv_obj_set_style_opa(status_bar_, LV_OPA_TRANSP, 0);
    // lv_obj_set_style_bg_color(status_bar_, lv_palette_main(LV_PALETTE_RED), 0);
    lv_obj_set_height(status_bar_, 40);
    // 通知信息
    notification_label_ = lv_label_create(status_bar_);
    lv_obj_set_width(notification_label_, LV_HOR_RES * 0.75);
    lv_obj_set_style_text_align(notification_label_, LV_TEXT_ALIGN_CENTER, 0);
    lv_obj_set_style_text_color(notification_label_, lvgl_theme->text_color(), 0);
    lv_obj_align(notification_label_, LV_ALIGN_CENTER, 0, 0);
    lv_obj_add_flag(notification_label_, LV_OBJ_FLAG_HIDDEN);
    lv_label_set_text(notification_label_, "");

    // 底部区域
    bottom_bar_ = lv_obj_create(screen);
    lv_obj_set_width(bottom_bar_, LV_HOR_RES);
    lv_obj_set_height(bottom_bar_, LV_SIZE_CONTENT);
    lv_obj_set_style_min_height(bottom_bar_, 40, 0);
    lv_obj_set_style_radius(bottom_bar_, 0, 0);
    lv_obj_set_style_border_width(bottom_bar_, 0, 0);
    // lv_obj_set_style_bg_color(bottom_bar_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_bg_opa(bottom_bar_, LV_OPA_TRANSP, 0);
    lv_obj_set_style_text_color(bottom_bar_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_pad_top(bottom_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_style_pad_bottom(bottom_bar_, lvgl_theme->spacing(2), 0);
    lv_obj_set_style_pad_left(bottom_bar_, lvgl_theme->spacing(4), 0);
    lv_obj_set_style_pad_right(bottom_bar_, lvgl_theme->spacing(4), 0);
    lv_obj_align(bottom_bar_, LV_ALIGN_BOTTOM_MID, 0, 0);
    // 聊天内容
    chat_message_label_ = lv_label_create(bottom_bar_);
    lv_label_set_text(chat_message_label_, "");
    lv_obj_set_width(chat_message_label_, LV_HOR_RES - lvgl_theme->spacing(8));
    lv_label_set_long_mode(chat_message_label_, LV_LABEL_LONG_WRAP);
    lv_obj_set_style_bg_color(bottom_bar_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_text_align(chat_message_label_, LV_TEXT_ALIGN_CENTER, 0);
    lv_obj_set_style_text_color(chat_message_label_, lvgl_theme->text_color(), 0);
    lv_obj_align(chat_message_label_, LV_ALIGN_CENTER, 0, 0);
    // lv_obj_set_style_opa(chat_message_label_, LV_OPA_TRANSP, 0);

    // 低电量提醒弹出框
    low_battery_popup_ = lv_obj_create(screen);
    lv_obj_set_scrollbar_mode(low_battery_popup_, LV_SCROLLBAR_MODE_OFF);
    lv_obj_set_size(low_battery_popup_, LV_HOR_RES * 0.9, text_font->line_height * 2);
    lv_obj_align(low_battery_popup_, LV_ALIGN_BOTTOM_MID, 0, -lvgl_theme->spacing(4));
    lv_obj_set_style_bg_color(low_battery_popup_, lvgl_theme->low_battery_color(), 0);
    lv_obj_set_style_radius(low_battery_popup_, lvgl_theme->spacing(4), 0);
    // 低电量提醒文字
    low_battery_label_ = lv_label_create(low_battery_popup_);
    lv_label_set_text(low_battery_label_, Lang::Strings::BATTERY_NEED_CHARGE);
    lv_obj_set_style_text_color(low_battery_label_, lv_color_white(), 0);
    lv_obj_center(low_battery_label_);
    lv_obj_add_flag(low_battery_popup_, LV_OBJ_FLAG_HIDDEN);

    // 设置默认表情为neutral
    SetEmotion("neutral");
}

void KyleLcdDisplay::CustomEmoji() {
    ESP_LOGI(TAG, "初始化自定义表情");
    emotion_type_ = Settings("display", false).GetString("emotion_theme", "pig");

    ESP_LOGI(TAG, "自定义表情初始化完成");
}

void KyleLcdDisplay::SetPreviewImage(std::unique_ptr<LvglImage> image) {
    DisplayLockGuard lock(this);
    if (preview_image_ == nullptr) {
        ESP_LOGE(TAG, "预览图片区域尚未初始化");
        return;
    }

    if (image == nullptr) {
        esp_timer_stop(preview_timer_);
        lv_obj_remove_flag(emoji_box_, LV_OBJ_FLAG_HIDDEN);
        lv_obj_add_flag(preview_image_, LV_OBJ_FLAG_HIDDEN);
        preview_image_cache_.reset();
        if (gif_controller_) {
            gif_controller_->Start();
        }
        return;
    }

    // 图片缓存
    preview_image_cache_ = std::move(image);
    auto img_dsc = preview_image_cache_->image_dsc();
    lv_image_set_src(preview_image_, img_dsc);
    if (img_dsc->header.w > 0 && img_dsc->header.h > 0) {
        // 缩小--0.5
        lv_image_set_scale(preview_image_, 128 * width_ / img_dsc->header.w);
    }
    if (gif_controller_) {
        gif_controller_->Stop();
    }
    lv_obj_add_flag(emoji_box_, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(preview_image_, LV_OBJ_FLAG_HIDDEN);
    esp_timer_stop(preview_timer_);
    ESP_ERROR_CHECK(esp_timer_start_once(preview_timer_, PREVIEW_IMAGE_DURATION_MS * 1000));
}

void KyleLcdDisplay::SetChatMessage(const char* role, const char* content) {
    DisplayLockGuard lock(this);

    if (chat_message_label_ == nullptr) {
        ESP_LOGE(TAG, "聊天内容区域尚未初始化");
        return;
    }

    lv_label_set_text(chat_message_label_, content);
}

bool is_common_emotion(const char* emotion) {
    std::string emo = std::string(emotion);
    return emo == "microchip_ai";
}

void KyleLcdDisplay::SetEmotion(const char* emotion) {
    std::string file_name_str = is_common_emotion(emotion)
                                    ? std::string(emotion) + ".png"
                                    : std::string(emotion) + "_" + emotion_type_ + ".png";
    const char* file_name = file_name_str.c_str();

    // 先关闭GIF动画
    if (gif_controller_) {
        DisplayLockGuard lock(this);
        gif_controller_->Stop();
        gif_controller_.reset();
    }

    if (emoji_image_ == nullptr) {
        ESP_LOGE(TAG, "表情区域尚未初始化，当前欲配置表情：%s", emotion);
        return;
    }

    DisplayLockGuard lock(this);
    auto img = ImageLoader::GetImage(file_name, 124, 124);
    if (img) {
        lv_image_set_src(emoji_image_, img);
        lv_obj_add_flag(emoji_label_, LV_OBJ_FLAG_HIDDEN);
        lv_obj_remove_flag(emoji_image_, LV_OBJ_FLAG_HIDDEN);
    } else {
        // 如果该表情图片不存在，则从表情字符查找
        const char* emoji_utf8 = font_awesome_get_utf8(emotion);
        if (emoji_utf8 != nullptr && emoji_label_ != nullptr) {
            DisplayLockGuard lock(this);
            lv_label_set_text(emoji_label_, emoji_utf8);
            lv_obj_add_flag(emoji_image_, LV_OBJ_FLAG_HIDDEN);
            lv_obj_remove_flag(emoji_label_, LV_OBJ_FLAG_HIDDEN);
        }
        return;
    }
    lv_obj_add_flag(emoji_label_, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(emoji_image_, LV_OBJ_FLAG_HIDDEN);
}

void KyleLcdDisplay::SetTheme(Theme* theme) {
    DisplayLockGuard lock(this);

    auto lvgl_theme = static_cast<LvglTheme*>(theme);

    // 获取当前活跃的界面
    lv_obj_t* screen = lv_screen_active();
    // 重置字体
    auto text_font = lvgl_theme->text_font()->font();
    auto icon_font = lvgl_theme->icon_font()->font();
    auto large_icon_font = lvgl_theme->large_icon_font()->font();

    if (text_font->line_height > 40) {
        lv_obj_set_style_text_font(mute_label_, large_icon_font, 0);
        lv_obj_set_style_text_font(battery_label_, large_icon_font, 0);
        lv_obj_set_style_text_font(network_label_, large_icon_font, 0);
    } else {
        lv_obj_set_style_text_font(mute_label_, icon_font, 0);
        lv_obj_set_style_text_font(battery_label_, icon_font, 0);
        lv_obj_set_style_text_font(network_label_, icon_font, 0);
    }

    lv_obj_set_style_text_font(screen, text_font, 0);
    lv_obj_set_style_text_color(screen, lvgl_theme->text_color(), 0);
    if (lvgl_theme->background_image() != nullptr) {
        lv_obj_set_style_bg_image_src(container_, lvgl_theme->background_image()->image_dsc(), 0);
    } else {
        lv_obj_set_style_bg_image_src(container_, nullptr, 0);
        lv_obj_set_style_bg_color(container_, lvgl_theme->background_color(), 0);
    }

    if (top_bar_ != nullptr) {
        lv_obj_set_style_bg_opa(top_bar_, LV_OPA_50, 0);
        lv_obj_set_style_bg_color(top_bar_, lvgl_theme->background_color(), 0);
    }

    lv_obj_set_style_text_color(network_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(status_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(notification_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(mute_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(battery_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(emoji_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_color(chat_message_label_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_bg_color(chat_message_label_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_bg_color(bottom_bar_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_bg_color(low_battery_popup_, lvgl_theme->low_battery_color(), 0);

    Display::SetTheme(lvgl_theme);
}
void KyleLcdDisplay::toggleTheme(const char* theme_name) {
    auto manager = &LvglThemeManager::GetInstance();
    SetTheme(manager->GetTheme(theme_name));
}

void KyleLcdDisplay::SetEmotionType(const std::string& emotion_type) {
    emotion_type_ = emotion_type;
    SetEmotion("neutral");
}

void KyleLcdDisplay::SetHideSubtitle(bool hide) {
    DisplayLockGuard lock(this);
    hide_subtitle_ = hide;

    if (bottom_bar_ != nullptr) {
        if (hide) {
            lv_obj_add_flag(bottom_bar_, LV_OBJ_FLAG_HIDDEN);
        } else {
            lv_obj_remove_flag(bottom_bar_, LV_OBJ_FLAG_HIDDEN);
        }
    }
}

void KyleLcdDisplay::ShowToast(const char* message, ToastTime toast_time) {
    DisplayLockGuard lock(this);

    // 1. 创建容器（深色半透明背景）
    lv_obj_t* toast = lv_obj_create(lv_scr_act());
    lv_obj_set_size(toast, LV_SIZE_CONTENT, LV_SIZE_CONTENT);
    lv_obj_align(toast, LV_ALIGN_CENTER, 0, 0);
    lv_obj_set_style_bg_color(toast, lv_color_black(), 0);
    lv_obj_set_style_bg_opa(toast, LV_OPA_70, 0);
    lv_obj_set_style_border_width(toast, 0, 0);
    lv_obj_set_style_radius(toast, 20, 0);
    lv_obj_set_style_pad_all(toast, 10, 0);

    // 2. 创建文字
    lv_obj_t* label = lv_label_create(toast);
    lv_label_set_text(label, message);
    lv_obj_set_style_text_color(label, lv_color_white(), 0);
    lv_obj_center(label);

    // 3. 自动销毁定时器
    // 使用 LVGL 内置的清理函数，在指定毫秒后删除对象
    int duration_ms = 2000;
    if (toast_time == ToastTime::SHORT) {
        duration_ms = 1000;
    } else if (toast_time == ToastTime::LONG) {
        duration_ms = 5000;
    }

    lv_obj_delete_delayed(toast, duration_ms);
}

KyleV1Display::KyleV1Display(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel,
                             int width, int height)
    : KyleLcdDisplay(panel_io, panel, width, height) {
    // 绘制白色
    std::vector<uint16_t> buffer(width_, 0xFFFF);
    for (int y = 0; y < height_; y++) {
        esp_lcd_panel_draw_bitmap(panel_, 0, y, width_, y + 1, buffer.data());
    }

    {
        esp_err_t __err = esp_lcd_panel_disp_on_off(panel_, true);
        if (__err == ESP_ERR_NOT_SUPPORTED) {
            ESP_LOGW(TAG, "显示屏画布不支持显示");
        } else {
            ESP_ERROR_CHECK(__err);
        }
    }
    lv_init();

    lvgl_port_cfg_t port_cfg = ESP_LVGL_PORT_INIT_CONFIG();
    port_cfg.task_priority = 1;
    lvgl_port_init(&port_cfg);

    const lvgl_port_display_cfg_t display_cfg = {
        .io_handle = panel_io_,
        .panel_handle = panel_,
        .control_handle = nullptr,
        .buffer_size = static_cast<uint32_t>(width_ * 20),
        .double_buffer = false,
        .trans_size = 0,
        .hres = static_cast<uint32_t>(width_),
        .vres = static_cast<uint32_t>(height_),
        .monochrome = false,
        .rotation =
            {
                .swap_xy = false,
                .mirror_x = false,
                .mirror_y = false,
            },
        .color_format = LV_COLOR_FORMAT_RGB565,
        .flags =
            {
                .buff_dma = 1,
                .buff_spiram = 0,
                .sw_rotate = 0,
                .swap_bytes = 1,
                .full_refresh = 0,
                .direct_mode = 0,
            },
    };
    display_ = lvgl_port_add_disp(&display_cfg);
    if (display_ == nullptr) {
        ESP_LOGE(TAG, "加载LCD display失败");
        return;
    }
    // CustomEmoji();
    lv_async_call(
        [](void* arg) {
            auto display = static_cast<KyleV1Display*>(arg);
            display->CustomEmoji();
            display->SetupUI();
            display->resetHomeUI();
            display->initMenu();
        },
        this);
}

void KyleV1Display::initMenu() {
    DisplayLockGuard lock(this);
    if (menuUi_ == nullptr) {
        menuUi_ = new KyleTouchMenuUI(lv_scr_act());
    }
}

void KyleV1Display::showMenu(std::vector<MenuItem> menu) {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->setMenu(menu);
        menuUi_->show();
    }
}
void KyleV1Display::hideMenu() {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->hide();
    }
}

bool KyleV1Display::isMenuOpened() {
    if (menuUi_) {
        return menuUi_->isMenuOpened();
    }
    return false;
}

void KyleV1Display::onKeyNext() {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->next();
    }
}

void KyleV1Display::onKeyPrev() {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->prev();
    }
}

void KyleV1Display::onKeyEnter() {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->select();
    }
}

void KyleV1Display::onKeyBack() {
    if (menuUi_) {
        DisplayLockGuard lock(this);
        menuUi_->back();
    }
}

bool KyleV1Display::isHideHomePage() const { return home_type_ == "none"; }

void KyleV1Display::SetStatus(const char* status) {
    LvglDisplay::SetStatus(status);
    auto& app = Application::GetInstance();

    if (!isHideHomePage() && app.GetDeviceState() == kDeviceStateIdle) {
        // todo 配置延迟2s展示
        showHome();
    } else {
        hideHome();
    }
}


void KyleV1Display::setHomeType(const std::string& home_type) {
    if (home_type != home_type_) {
        home_type_ = home_type;
        resetHomeUI();
    }
}
void KyleV1Display::resetHomeUI() {
    if (homeUi_ != nullptr) {
        DisplayLockGuard lock(this);
        delete homeUi_;
        homeUi_ = nullptr;
    }
    if (home_type_ == "twt") {
        homeUi_ = new TwtHomeUI(lv_scr_act());
    }
}

void KyleV1Display::showHome() {
    if (isHideHomePage()) {
        return;
    }
    if (homeUi_) {
        DisplayLockGuard lock(this);
        homeUi_->Show();
    }
}

void KyleV1Display::hideHome() {
    if (homeUi_) {
        DisplayLockGuard lock(this);
        homeUi_->Hide();
    }
}

void KyleV1Display::toggleTheme(const char* theme_name) {
    KyleLcdDisplay::toggleTheme(theme_name);

    DisplayLockGuard lock(this);
    resetHomeUI();
}

KyleV2Display::KyleV2Display(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel,
                             int touchSda, int touchScl, int width, int height)
    : KyleV1Display(panel_io, panel, width, height),
      busHandle_(nullptr),
      devHandle_(nullptr),
      touchSda_(touchSda),
      touchScl_(touchScl) {
    registerTouchV9();
    initTouch();
}

void KyleV2Display::registerTouchV9() {
    if (lvgl_port_lock(portMAX_DELAY)) {
        lv_indev_t* indev = lv_indev_create();

        lv_indev_set_type(indev, LV_INDEV_TYPE_POINTER);
        lv_indev_set_display(indev, display_);

        lv_indev_set_user_data(indev, this);

        // 设置读取回调
        lv_indev_set_read_cb(indev, [](lv_indev_t* drv, lv_indev_data_t* data) {
            // 获取类实例
            auto* self = static_cast<KyleV2Display*>(lv_indev_get_user_data(drv));
            int x, y;
            // 装配点击事件
            if (self->getTouch(&x, &y)) {
                data->point.x = static_cast<lv_coord_t>(self->width_ - x);
                data->point.y = static_cast<lv_coord_t>(self->height_ - y);
                data->state = LV_INDEV_STATE_PRESSED;
            } else {
                data->state = LV_INDEV_STATE_RELEASED;
            }
        });
        lvgl_port_unlock();
    }
}

void KyleV2Display::initTouch() {
    i2c_master_bus_config_t i2c_bus_cfg = {
        .i2c_port = I2C_NUM_0,
        .sda_io_num = static_cast<gpio_num_t>(touchSda_),
        .scl_io_num = static_cast<gpio_num_t>(touchScl_),
        .clk_source = I2C_CLK_SRC_DEFAULT,
    };
    esp_err_t ret = i2c_new_master_bus(&i2c_bus_cfg, &busHandle_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2C Bus Init Failed: %s", esp_err_to_name(ret));
        return;  // 初始化失败直接返回，不继续 add_device
    }

    i2c_device_config_t dev_cfg = {
        .dev_addr_length = I2C_ADDR_BIT_LEN_7,
        .device_address = 0x38,
        .scl_speed_hz = 100000,
    };
    ret = i2c_master_bus_add_device(busHandle_, &dev_cfg, &devHandle_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2C Device Add Failed: %s", esp_err_to_name(ret));
        devHandle_ = nullptr;
    } else {
        ESP_LOGI(TAG, "I2C Touch Initialized Successfully on GPIO %d, %d", touchSda_, touchScl_);
    }
}

bool KyleV2Display::getTouch(int* x, int* y) {
    if (devHandle_ == nullptr) {
        return false;
    }

    uint8_t reg = 0x02;
    uint8_t data[5];
    esp_err_t ret = i2c_master_transmit_receive(devHandle_, &reg, 1, data, 5, -1);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "I2C 传输失败: %s", esp_err_to_name(ret));
        return false;
    }
    if ((data[0] & 0x0F) > 0) {
        *x = ((data[1] & 0x0F) << 8) | data[2];
        *y = ((data[3] & 0x0F) << 8) | data[4];
        return true;
    }
    return false;
}