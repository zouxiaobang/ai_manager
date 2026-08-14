#include "application.h"
#include "button.h"
#include "codecs/no_audio_codec.h"
#include "config.h"
#include "display/kyle_display.h"
#include "esp_sleep.h"
#include "lamp_controller.h"
#include "led/single_led.h"
#include "wifi_board.h"

#include <driver/spi_common.h>
#include <esp_lcd_panel_io.h>
#include <esp_lcd_panel_ops.h>
#include <esp_lcd_panel_vendor.h>
#include <esp_log.h>

#include "KyleBoard.h"
#include "display/menu/kyle_menu_ui.h"
#include "kyle_power_manager.h"
#include "power_save_timer.h"
#include "settings.h"

#include "assets/icons/battery.h"

#define TAG "KyleS3LCDBoard"

#define ICON_SETTING LV_SYMBOL_SETTINGS
#define ICON_VOICE LV_SYMBOL_AUDIO
#define ICON_WIFI LV_SYMBOL_WIFI
#define ICON_BATTERY LV_SYMBOL_BATTERY_FULL
#define SETTING_DISPLAY_NS "display"
#define SETTING_DISPLAY_BRIGHTNESS_KEY "brightness"
#define SETTING_DISPLAY_DELAY_TIME_KEY "delay_time"
#define SETTING_DISPLAY_HOME_TYPE_KEY "home_page_type"
#define SETTING_DISPLAY_EMOTION_TYPE_KEY "emotion_theme"
#define SETTING_DISPLAY_THEME_KEY "theme"
#define SETTING_AUDIO_NS "audio"
#define SETTING_AUDIO_VOLUME_KEY "output_volume"

enum class ButtonEvent {
    BootClick,
    BootDoubleClick,
    BootLongPress,
    UpClick,
    UpLongPress,
    DownClick,
    DownLongPress
};

class KyleS3LcdBroad : public WifiBoard, public KyleBoard {
private:
    Button boot_button_;
    Button up_button_;
    Button down_button_;
    // SpiLcdDisplay* display_;
    KyleV1Display* display_;
    QueueHandle_t button_queue_;
    PowerSaveTimer* power_save_timer_;
    PowerManager* power_manager_;
    // KyleMenuUI* menu_ui_;
    std::vector<MenuItem> menu_;
    esp_lcd_panel_handle_t panel_ = nullptr;

    void InitializeSpi() {
        spi_bus_config_t buscfg = {};
        buscfg.mosi_io_num = DISPLAY_MOSI_PIN;
        buscfg.miso_io_num = GPIO_NUM_NC;
        buscfg.sclk_io_num = DISPLAY_CLK_PIN;
        buscfg.quadwp_io_num = GPIO_NUM_NC;
        buscfg.quadhd_io_num = GPIO_NUM_NC;
        buscfg.max_transfer_sz = (240 * 120 * 2) + 1024;
        ;
        ESP_ERROR_CHECK(spi_bus_initialize(SPI3_HOST, &buscfg, SPI_DMA_CH_AUTO));
    }

    void InitializeLcdDisplay() {
        esp_lcd_panel_io_handle_t panel_io = nullptr;
        panel_ = nullptr;
        // 液晶屏控制IO初始化
        ESP_LOGI(TAG, "初始化屏幕");
        esp_lcd_panel_io_spi_config_t io_config = {};
        io_config.cs_gpio_num = DISPLAY_CS_PIN;
        io_config.dc_gpio_num = DISPLAY_DC_PIN;
        io_config.spi_mode = DISPLAY_SPI_MODE;
        io_config.pclk_hz = 40 * 1000 * 1000;
        io_config.trans_queue_depth = 10;
        io_config.lcd_cmd_bits = 8;
        io_config.lcd_param_bits = 8;
        ESP_ERROR_CHECK(esp_lcd_new_panel_io_spi(SPI3_HOST, &io_config, &panel_io));
        esp_lcd_panel_dev_config_t panel_config = {};
        panel_config.reset_gpio_num = DISPLAY_RST_PIN;
        panel_config.rgb_ele_order = DISPLAY_RGB_ORDER;
        panel_config.bits_per_pixel = 16;
        ESP_ERROR_CHECK(esp_lcd_new_panel_st7789(panel_io, &panel_config, &panel_));

        esp_lcd_panel_reset(panel_);

        esp_lcd_panel_init(panel_);
        esp_lcd_panel_invert_color(panel_, DISPLAY_INVERT_COLOR);
        esp_lcd_panel_swap_xy(panel_, DISPLAY_SWAP_XY);
        esp_lcd_panel_mirror(panel_, DISPLAY_MIRROR_X, DISPLAY_MIRROR_Y);

        if (!CAN_TOUCH) {
            display_ = new KyleV1Display(panel_io, panel_, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        } else {
            display_ = new KyleV2Display(panel_io, panel_, DISPLAY_TOUCH_SDA, DISPLAY_TOUCH_SCL,
                                         DISPLAY_WIDTH, DISPLAY_HEIGHT);
        }
        InitializeMenus();
    }

    void InitializeMenus() {
        std::vector batteryMenu = {
            MenuItem{.name = "睡眠",
                     .iconTitle = "menu_sleep",
                     .onConfirm = [this] { enterDeepSleep(); }},
            MenuItem{
                .name = "重启", .iconTitle = "menu_reboot", .onConfirm = [] { esp_restart(); }}};

        std::vector displayLightMenu = {
            MenuItem{.name = "至暗",
                     .iconTitle = "menu_per_1",
                     .onConfirm =
                         [this] {
                             GetBacklight()->SetBrightness(1);
                             display_->ShowToast("当前亮度设置为1", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, 1);
                         }},
            MenuItem{.name = "偏暗",
                     .iconTitle = "menu_per_25",
                     .onConfirm =
                         [this] {
                             GetBacklight()->SetBrightness(25);
                             display_->ShowToast("当前亮度设置为25", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, 25);
                         }},
            MenuItem{.name = "适中",
                     .iconTitle = "menu_per_50",
                     .onConfirm =
                         [this] {
                             GetBacklight()->SetBrightness(50);
                             display_->ShowToast("当前亮度设置为50", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, 50);
                         }},
            MenuItem{.name = "偏亮",
                     .iconTitle = "menu_per_75",
                     .onConfirm =
                         [this] {
                             GetBacklight()->SetBrightness(75);
                             display_->ShowToast("当前亮度设置为75", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, 75);
                         }},
            MenuItem{
                .name = "最亮", .iconTitle = "menu_per_100", .onConfirm = [this] {
                    GetBacklight()->SetBrightness(100);
                    display_->ShowToast("当前亮度设置为100", ToastTime::NORMAL);
                    Settings(SETTING_DISPLAY_NS, true).SetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, 100);
                }}};

        std::vector displayLightTimeMenu = {
            MenuItem{.name = "1分钟",
                     .iconTitle = "menu_min_1",
                     .onConfirm =
                         [this] {
                             power_save_timer_->SetTime(60, 120);
                             display_->ShowToast("等待1分钟后暗屏", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_DELAY_TIME_KEY, 60);
                         }},
            MenuItem{.name = "2分钟",
                     .iconTitle = "menu_min_2",
                     .onConfirm =
                         [this] {
                             power_save_timer_->SetTime(120, 180);
                             display_->ShowToast("等待2分钟后暗屏", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetInt(SETTING_DISPLAY_DELAY_TIME_KEY, 120);
                         }},
            MenuItem{
                .name = "5分钟", .iconTitle = "menu_min_5", .onConfirm = [this] {
                    power_save_timer_->SetTime(300, 360);
                    display_->ShowToast("等待5分钟后暗屏", ToastTime::NORMAL);
                    Settings(SETTING_DISPLAY_NS, true).SetInt(SETTING_DISPLAY_DELAY_TIME_KEY, 300);
                }}};

        std::vector displayHomeMenu = {
            MenuItem{.name = "默认",
                     .iconTitle = "menu_display_default",
                     .onConfirm =
                         [this] {
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetString(SETTING_DISPLAY_HOME_TYPE_KEY, "twt");
                             display_->setHomeType("twt");
                             display_->showHome();
                             display_->hideMenu();
                         }},
            MenuItem{.name = "关闭", .iconTitle = "menu_display_cancel", .onConfirm = [this] {
                         Settings(SETTING_DISPLAY_NS, true)
                             .SetString(SETTING_DISPLAY_HOME_TYPE_KEY, "none");
                         display_->setHomeType("none");
                         display_->showHome();
                         display_->hideMenu();
                     }}};

        std::vector displayMenu = {
            MenuItem{.name = "亮度",
                     .iconTitle = "menu_light",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int {
                         switch (Settings(SETTING_DISPLAY_NS, false)
                                     .GetInt(SETTING_DISPLAY_BRIGHTNESS_KEY, -1)) {
                             case 1:
                                 return 0;
                             case 25:
                                 return 1;
                             case 50:
                                 return 2;
                             case 75:
                                 return 3;
                             case 100:
                                 return 4;
                             default:
                                 return -1;
                         }
                     },
                     .children = displayLightMenu},
            MenuItem{.name = "首页",
                     .iconTitle = "menu_home",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int {
                         const std::string homeType =
                             Settings(SETTING_DISPLAY_NS, false)
                                 .GetString(SETTING_DISPLAY_HOME_TYPE_KEY, "twt");
                         if (homeType == "none") {
                             return 1;
                         }
                         return 0;
                     },
                     .children = displayHomeMenu},
            MenuItem{.name = "休眠",
                     .iconTitle = "menu_light_time",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int {
                         switch (Settings(SETTING_DISPLAY_NS, false)
                                     .GetInt(SETTING_DISPLAY_DELAY_TIME_KEY, 60)) {
                             case 60:
                                 return 0;
                             case 120:
                                 return 1;
                             case 300:
                                 return 2;
                             default:
                                 return -1;
                         }
                     },
                     .children = displayLightTimeMenu}};

        std::vector volumeMenu = {
            MenuItem{
                .name = "静音", .iconTitle = "menu_mute", .onConfirm = [this] { muteVolume(); }},
            MenuItem{.name = "上调",
                     .iconTitle = "menu_high_volume",
                     .onConfirm = [this] { upVolume(); }},
            MenuItem{.name = "下调",
                     .iconTitle = "menu_high_volume",
                     .onConfirm = [this] { downVolume(); }},
        };

        std::vector netMenu = {
            MenuItem{.name = "重连WIFI",
                     .iconTitle = "menu_reconnect_wifi",
                     .onConfirm =
                         [this] {
                             reconnectWifi();
                             display_->hideMenu();
                         }},
            MenuItem{.name = "重置WIFI", .iconTitle = "menu_reconnect_wifi", .onConfirm = [this] {
                         resetWifi();
                         display_->hideMenu();
                     }}};

        std::vector emotionMenu = {
            MenuItem{.name = "可爱猪猪",
                     .iconTitle = "menu_pig",
                     .onConfirm =
                         [this] {
                             display_->SetEmotionType("pig");
                             display_->ShowToast("表情包切换为可爱猪猪", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetString(SETTING_DISPLAY_EMOTION_TYPE_KEY, "pig");
                         }},
            MenuItem{.name = "聪明兔",
                     .iconTitle = "menu_rabbit",
                     .onConfirm =
                         [this] {
                             display_->SetEmotionType("rabbit");
                             display_->ShowToast("表情包切换为聪明兔", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetString(SETTING_DISPLAY_EMOTION_TYPE_KEY, "rabbit");
                         }},
            MenuItem{.name = "傻狗", .iconTitle = "menu_dog", .onConfirm = [this] {
                         display_->SetEmotionType("dog");
                         display_->ShowToast("表情包切换为傻狗", ToastTime::NORMAL);
                         Settings(SETTING_DISPLAY_NS, true)
                             .SetString(SETTING_DISPLAY_EMOTION_TYPE_KEY, "dog");
                     }}};

        std::vector themeMenu = {
            MenuItem{.name = "白天",
                     .iconTitle = "menu_theme_light",
                     .onConfirm =
                         [this] {
                             display_->toggleTheme("light");
                             display_->ShowToast("切换为白天主题", ToastTime::NORMAL);
                             Settings(SETTING_DISPLAY_NS, true)
                                 .SetString(SETTING_DISPLAY_THEME_KEY, "light");
                         }},
            MenuItem{
                .name = "黑夜", .iconTitle = "menu_theme_dark", .onConfirm = [this] {
                    display_->toggleTheme("dark");
                    display_->ShowToast("切换为黑夜主题", ToastTime::NORMAL);
                    Settings(SETTING_DISPLAY_NS, true).SetString(SETTING_DISPLAY_THEME_KEY, "dark");
                }}};

        menu_ = {
            MenuItem{.name = "电源",
                     .iconTitle = "menu_battery",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int { return 0; },
                     .children = batteryMenu},
            MenuItem{.name = "显示",
                     .iconTitle = "menu_display",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int { return 0; },
                     .children = displayMenu},
            MenuItem{
                .name = "音量",
                .iconTitle = "menu_volume",
                .type = MenuItemType::SUBMENU,
                .onCurrentIndex = []() -> int {
                    ESP_LOGI(TAG, "调声%d", Settings(SETTING_AUDIO_NS, false).GetInt(SETTING_AUDIO_VOLUME_KEY, 0));
                    if (Settings(SETTING_AUDIO_NS, false).GetInt(SETTING_AUDIO_VOLUME_KEY, 0) ==
                        0) {
                        return 0;
                    }
                    return -1;
                },
                .children = volumeMenu},
            MenuItem{.name = "网络",
                     .iconTitle = "menu_net",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int { return 0; },
                     .children = netMenu},
            MenuItem{.name = "表情",
                     .iconTitle = "menu_emote",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int {
                         std::string emotion =
                             Settings(SETTING_DISPLAY_NS, false)
                                 .GetString(SETTING_DISPLAY_EMOTION_TYPE_KEY, "pig");
                         if (emotion == "rabbit") {
                             return 1;
                         }
                         if (emotion == "dog") {
                             return 2;
                         }
                         return 0;
                     },
                     .children = emotionMenu},
            MenuItem{.name = "主题",
                     .iconTitle = "menu_theme",
                     .type = MenuItemType::SUBMENU,
                     .onCurrentIndex = []() -> int {
                         std::string emotion = Settings(SETTING_DISPLAY_NS, false)
                                                   .GetString(SETTING_DISPLAY_THEME_KEY, "light");
                         if (emotion == "dark") {
                             return 1;
                         }
                         return 0;
                     },
                     .children = themeMenu},
        };
    }

    void InitializeButtons() {
        boot_button_.OnClick([this]() {
            ButtonEvent event = ButtonEvent::BootClick;
            xQueueSend(button_queue_, &event, 0);
        });

        boot_button_.OnDoubleClick([this]() {
            ButtonEvent event = ButtonEvent::BootDoubleClick;
            xQueueSend(button_queue_, &event, 0);
        });

        boot_button_.OnLongPress([this]() {
            ButtonEvent event = ButtonEvent::BootLongPress;
            xQueueSend(button_queue_, &event, 0);
        });

        up_button_.OnClick([this]() {
            ButtonEvent event = ButtonEvent::UpClick;
            xQueueSend(button_queue_, &event, 0);
        });
        up_button_.OnLongPress([this]() {
            ButtonEvent event = ButtonEvent::UpLongPress;
            xQueueSend(button_queue_, &event, 0);
        });

        down_button_.OnClick([this]() {
            ButtonEvent event = ButtonEvent::DownClick;
            xQueueSend(button_queue_, &event, 0);
        });

        down_button_.OnLongPress([this]() {
            ButtonEvent event = ButtonEvent::DownLongPress;
            xQueueSend(button_queue_, &event, 0);
        });
    }
    void ProcessButtonEvents() {
        ButtonEvent event;

        while (xQueueReceive(button_queue_, &event, 0)) {
            power_save_timer_->WakeUp();
            if (power_save_timer_ != nullptr && power_save_timer_->IsInSleepMode()) {
                GetDisplay()->SetPowerSaveMode(false);
                GetBacklight()->RestoreBrightness();
            }

            switch (event) {
                case ButtonEvent::BootClick:
                    BootButtonClickHandler();
                    break;
                case ButtonEvent::BootDoubleClick:
                    BootButtonDoubleClickHandler();
                    break;

                case ButtonEvent::BootLongPress:
                    BootButtonLongClickHandler();
                    break;

                case ButtonEvent::UpClick:
                    UpButtonClickHandler();
                    break;
                case ButtonEvent::UpLongPress:
                    UpButtonLongClickHandler();
                    break;
                case ButtonEvent::DownClick:
                    DownButtonClickHandler();
                    break;
                case ButtonEvent::DownLongPress:
                    DownButtonLongClickHandler();
                    break;
                default:
                    break;
            }
        }
    }

    void BootButtonClickHandler() {
        if (display_->isMenuOpened()) {
            display_->onKeyEnter();
        } else {
            Application::GetInstance().ToggleChatState();
        }
    }
    void BootButtonDoubleClickHandler() {
        ESP_LOGI(TAG, "目录已%s", (display_->isMenuOpened()) ? "打开" : "关闭");
        if (display_->isMenuOpened()) {
            display_->onKeyBack();
        } else {
            Application::GetInstance().ResetLSState();
            display_->showMenu(menu_);
        }
    }

    void BootButtonLongClickHandler() { enterDeepSleep(); }

    void UpButtonClickHandler() {
        if (display_->isMenuOpened()) {
            display_->onKeyPrev();
        } else {
            upVolume();
        }
    }

    void DownButtonClickHandler() {
        if (display_->isMenuOpened()) {
            display_->onKeyNext();
        } else {
            downVolume();
        }
    }

    void UpButtonLongClickHandler() {
        if (!display_->isMenuOpened()) {
            GetAudioCodec()->SetOutputVolume(100);
        }
    }

    void DownButtonLongClickHandler() {
        if (!display_->isMenuOpened()) {
            GetAudioCodec()->SetOutputVolume(0);
        }
    }

    // 物联网初始化，添加对 AI 可见设备
    void InitializeTools() { static LampController lamp(LAMP_GPIO); }

    void InitializePowerManager() {
        power_manager_ = new PowerManager(CHARGING_GPIO);
        power_manager_->OnTemperatureChanged([](float chip_temp) {
            // data
        });

        power_manager_->OnLowBatteryStatusChanged([](bool is_low_battery) {

        });
    }
    void InitializePowerSaveTimer() {
        power_save_timer_ = new PowerSaveTimer(-1, 120, 180);
        power_save_timer_->OnEnterSleepMode([this]() {
            GetDisplay()->SetPowerSaveMode(true);
            GetBacklight()->SetBrightness(1);
        });
        power_save_timer_->OnExitSleepMode([this]() {
            GetDisplay()->SetPowerSaveMode(false);
            GetBacklight()->RestoreBrightness();
        });
        power_save_timer_->OnShutdownRequest([this]() { enterDeepSleep(); });
        power_save_timer_->SetEnabled(true);
    }

    void enterDeepSleep() {
        if (panel_) {
            esp_lcd_panel_disp_on_off(panel_, false);
        }

        Application::GetInstance().ResetLSState();

        // 等待松开按钮（避免刚睡就唤醒）
        while (gpio_get_level((gpio_num_t)BOOT_BUTTON_GPIO) == 0) {
            vTaskDelay(pdMS_TO_TICKS(10));
        }

        // 内部上拉
        gpio_pullup_en((gpio_num_t)BOOT_BUTTON_GPIO);
        // 低电平唤醒
        esp_sleep_enable_ext0_wakeup(BOOT_BUTTON_GPIO, 0);

        GetBacklight()->SetBrightness(0);

        esp_deep_sleep_start();
    }
    void muteVolume() {
        GetAudioCodec()->SetOutputVolume(0);
        display_->ShowToast("静音模式已打开", ToastTime::NORMAL);
    }
    void upVolume() {
        const auto codec = GetAudioCodec();
        auto newVolume = codec->output_volume() + 10;
        if (newVolume > 100) {
            newVolume = 100;
        }
        codec->SetOutputVolume(newVolume);
        std::string message = "当前音量为：" + std::to_string(newVolume) + "%";
        display_->ShowToast(message.c_str(), ToastTime::NORMAL);
    }
    void downVolume() {
        const auto codec = GetAudioCodec();
        auto newVolume = codec->output_volume() - 10;
        if (newVolume < 0) {
            newVolume = 0;
        }
        codec->SetOutputVolume(newVolume);
        std::string message = "当前音量为：" + std::to_string(newVolume) + "%";
        display_->ShowToast(message.c_str(), ToastTime::NORMAL);
    }
    void reconnectWifi() { ReconnectWifi(); }
    void resetWifi() { EnterWifiConfigMode(); }

public:
    KyleS3LcdBroad()
        : boot_button_(BOOT_BUTTON_GPIO),
          up_button_(TOUCH_BUTTON_UP),
          down_button_(TOUCH_BUTTON_DOWN) {
        button_queue_ = xQueueCreate(10, sizeof(ButtonEvent));
        InitializeSpi();
        InitializeLcdDisplay();
        InitializeButtons();
        InitializeTools();
        InitializePowerManager();
        InitializePowerSaveTimer();
        if (DISPLAY_BACKLIGHT_PIN != GPIO_NUM_NC) {
            GetBacklight()->RestoreBrightness();
        }
        xTaskCreate(
            [](void* arg) {
                auto* board = static_cast<KyleS3LcdBroad*>(arg);
                while (true) {
                    board->ProcessButtonEvents();
                    vTaskDelay(pdMS_TO_TICKS(10));
                }
            },
            "board_task", 4096, this, 5, nullptr);
    }

    Led* GetLed() override {
        static SingleLed led(BUILTIN_LED_GPIO);
        return &led;
    }

    AudioCodec* GetAudioCodec() override {
        static NoAudioCodecSimplex audio_codec(AUDIO_INPUT_SAMPLE_RATE, AUDIO_OUTPUT_SAMPLE_RATE,
                                               AUDIO_I2S_SPK_GPIO_BCLK, AUDIO_I2S_SPK_GPIO_LRCK,
                                               AUDIO_I2S_SPK_GPIO_DOUT, AUDIO_I2S_MIC_GPIO_SCK,
                                               AUDIO_I2S_MIC_GPIO_WS, AUDIO_I2S_MIC_GPIO_DIN);
        return &audio_codec;
    }

    Display* GetDisplay() override { return display_; }

    Backlight* GetBacklight() override {
        if constexpr (DISPLAY_BACKLIGHT_PIN != GPIO_NUM_NC) {
            static PwmBacklight backlight(DISPLAY_BACKLIGHT_PIN, DISPLAY_BACKLIGHT_OUTPUT_INVERT);
            return &backlight;
        }
    }

    bool GetBatteryLevel(int& level, bool& charging, bool& discharging) override {
        charging = power_manager_->IsCharging();
        discharging = power_manager_->IsDischarging();
        level = power_manager_->GetBatteryLevel();
        return true;
    }

    void handleEvent(GlobalEvent event, const char* text) override {
        switch (event) {
            case GlobalEvent::MENU_BATTERY:
                ESP_LOGI(TAG, "选中电池选项");
                break;
            default:
                break;
        }
    }
};

DECLARE_BOARD(KyleS3LcdBroad);
