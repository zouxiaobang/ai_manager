#ifndef LCD_DISPLAY_H
#define LCD_DISPLAY_H

#include "gif/lvgl_gif.h"
#include "lvgl_display.h"

#include <esp_lcd_panel_io.h>
#include <esp_lcd_panel_ops.h>
#include <font_emoji.h>

#include <atomic>
#include <memory>

#define PREVIEW_IMAGE_DURATION_MS 5000
enum class MenuEvent {
    MENU_EVENT_DEEP_SLEEP,
    MENU_TYPE_VOLUME_UP,
    MENU_TYPE_VOLUME_DOWN,
    MENU_TYPE_MUTE,
    MENU_TYPE_WIFI_RECONNECT,
    MENU_TYPE_WIFI_RESET,
    MENU_TYPE_DARK,
    MENU_TYPE_LIGHT
};

enum class MenuLevel {
    ROOT,
    VOLUME,
    WIFI_SETTING,
    THEME
};

class LcdDisplay : public LvglDisplay {
protected:
    esp_lcd_panel_io_handle_t panel_io_ = nullptr;
    esp_lcd_panel_handle_t panel_ = nullptr;

    lv_draw_buf_t draw_buf_;
    lv_obj_t* top_bar_ = nullptr;
    lv_obj_t* status_bar_ = nullptr;
    lv_obj_t* content_ = nullptr;
    lv_obj_t* container_ = nullptr;
    lv_obj_t* side_bar_ = nullptr;
    lv_obj_t* bottom_bar_ = nullptr;
    lv_obj_t* preview_image_ = nullptr;
    lv_obj_t* emoji_label_ = nullptr;
    lv_obj_t* emoji_image_ = nullptr;
    std::unique_ptr<LvglGif> gif_controller_ = nullptr;
    lv_obj_t* emoji_box_ = nullptr;
    lv_obj_t* chat_message_label_ = nullptr;
    esp_timer_handle_t preview_timer_ = nullptr;
    std::unique_ptr<LvglImage> preview_image_cached_ = nullptr;
    bool hide_subtitle_ = false;  // Control whether to hide chat messages/subtitles

    void InitializeLcdThemes();
    void SetupUI();
    virtual bool Lock(int timeout_ms = 0) override;
    virtual void Unlock() override;

protected:
    // Add protected constructor
    LcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
               int height);

public:
    ~LcdDisplay();
    virtual void SetEmotion(const char* emotion) override;
    virtual void SetChatMessage(const char* role, const char* content) override;
    virtual void SetPreviewImage(std::unique_ptr<LvglImage> image) override;

    // Add theme switching function
    virtual void SetTheme(Theme* theme) override;

    // Set whether to hide chat messages/subtitles
    void SetHideSubtitle(bool hide);
};

// SPI LCD display
class SpiLcdDisplay : public LcdDisplay {
private:
    lv_obj_t* menu_container_ = nullptr;
    lv_obj_t* menu_list_ = nullptr;
    std::vector<std::string> menu_items_ = {"Back", "Deep Sleep", "Volume", "Wifi Settings", "Toggle Theme", };
    int last_menu_index_ = 0;
    int menu_index_ = 0;
    int menu_size_ = menu_items_.size();
    std::vector<std::string> volume_menu_items_ = {"Back", "Volume +", "Volume -", "Mute"};
    std::vector<std::string> wifi_menu_items_ = {"Back", "Reconnect", "Reset"};
    bool menu_open_ = false;
    std::function<void(MenuEvent)> event_cb_;
    MenuLevel menu_level_ = MenuLevel::ROOT;

public:
    SpiLcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
                  int height, int offset_x, int offset_y, bool mirror_x, bool mirror_y,
                  bool swap_xy);

    void SetEventCallback(std::function<void(MenuEvent)> cb);
    void OpenMenu();
    void CloseMenu();
    void CreateMenu();
    void UpdateMenuHighlight();
    void MenuPrev();
    void MenuNext();
    void MenuEnter();
    void MenuBack();
    bool IsMenuOpened();
    void ShowRootMenu();
    void ShowVolumeMenu();
    void ShowWifiMenu();
    void ToggleTheme();

};

// RGB LCD display
class RgbLcdDisplay : public LcdDisplay {
public:
    RgbLcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
                  int height, int offset_x, int offset_y, bool mirror_x, bool mirror_y,
                  bool swap_xy);
};

// MIPI LCD display
class MipiLcdDisplay : public LcdDisplay {
public:
    MipiLcdDisplay(esp_lcd_panel_io_handle_t panel_io, esp_lcd_panel_handle_t panel, int width,
                   int height, int offset_x, int offset_y, bool mirror_x, bool mirror_y,
                   bool swap_xy);
};

#endif  // LCD_DISPLAY_H
