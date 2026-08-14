#include "application.h"
#include "button.h"
#include "config.h"
#include "driver/gpio.h"
#include "driver/spi_common.h"
#include "esp_lcd_panel_io.h"
#include "esp_lcd_panel_ops.h"
#include "esp_lcd_panel_vendor.h"
#include "esp_log.h"
#include "wifi_board.h"

#include <driver/i2c_master.h>
#include "codecs/no_audio_codec.h"
#include "display/oled_display.h"
#include "esp_sleep.h"
#include "esp_system.h"
#include "esp_timer.h"
#include "i2s_direct_codec.h"

#define TAG "SuperMiniC3"
#define LONG_PRESS_MS 1500
RTC_DATA_ATTR bool rtc_screen_on = true;

class SuperMiniC3Board : public WifiBoard {
private:
    i2c_master_bus_handle_t display_i2c_bus_;
    esp_lcd_panel_io_handle_t panel_io_ = nullptr;
    esp_lcd_panel_handle_t panel_ = nullptr;
    Display* display_ = nullptr;
    I2sDirectCodec* audio_codec_;
    Button boot_button_;
    Button volume_up_button_;
    Button volume_down_button_;
    esp_timer_handle_t long_press_timer_ = nullptr;
    bool long_press_triggered_ = false;

public:
    SuperMiniC3Board()
        : display_(nullptr),
          audio_codec_(nullptr),
          boot_button_(BOOT_BUTTON_GPIO),
          volume_up_button_(VOLUME_UP_BUTTON_GPIO),
          volume_down_button_(VOLUME_DOWN_BUTTON_GPIO) {
        esp_sleep_wakeup_cause_t wakeup_reason = esp_sleep_get_wakeup_cause();
        if (wakeup_reason == ESP_SLEEP_WAKEUP_EXT0) {
            ESP_LOGI(TAG, "Wakeup from deep sleep by button");
        }
        ESP_LOGI(TAG, "SuperMini C3 Board constructor");

        // 初始化按钮
        InitializeButtons();
        // 初始化显示
        InitializeDisplayI2c();
        InitializeSsd1306Display();

        // 初始化GPIO
        InitializeGPIO();

        // 创建音频编解码器实例
        audio_codec_ = new I2sDirectCodec(AUDIO_INPUT_SAMPLE_RATE, AUDIO_OUTPUT_SAMPLE_RATE);

        // 设置引脚
        audio_codec_->SetPins(AUDIO_I2S_GPIO_BCLK,   // BCLK
                              AUDIO_I2S_GPIO_WS,     // WS/LRC
                              AUDIO_I2S_GPIO_DIN,    // 麦克风数据输入
                              AUDIO_I2S_GPIO_DOUT,   // 功放数据输出
                              AUDIO_I2S_GPIO_MCLK);  // 主时钟

        ESP_LOGI(TAG, "Board initialization complete");
    }

    virtual ~SuperMiniC3Board() {
        if (audio_codec_) {
            delete audio_codec_;
        }
        if (display_) {
            delete display_;
        }
    }

private:
    void InitializeButtons() {
        boot_button_.OnClick([this]() {
            Application::GetInstance().ToggleChatState();
            // if (rtc_screen_on) {
            //     Application::GetInstance().ResetLSState();
            // }

            // rtc_screen_on = !rtc_screen_on;

            // if (panel_) {
            //     esp_lcd_panel_disp_on_off(panel_, rtc_screen_on);
            // }
        });
        boot_button_.OnLongPress([this]() {
            if (gpio_get_level((gpio_num_t)BOOT_BUTTON_GPIO) == 0) {
                rtc_screen_on = false;
                if (panel_) {
                    esp_lcd_panel_disp_on_off(panel_, rtc_screen_on);
                }

                // 等待松开按钮（避免刚睡就唤醒）
                while (gpio_get_level((gpio_num_t)BOOT_BUTTON_GPIO) == 0) {
                    vTaskDelay(pdMS_TO_TICKS(10));
                }

                // ⚠️ 必须是 GPIO0~5 才能 DeepSleep 唤醒
                esp_deep_sleep_enable_gpio_wakeup((1ULL << BOOT_BUTTON_GPIO),
                                                  ESP_GPIO_WAKEUP_GPIO_LOW);

                vTaskDelay(pdMS_TO_TICKS(50));
                esp_deep_sleep_start();
            }
        });
        volume_up_button_.OnClick([this]() {
            auto codec = GetAudioCodec();
            auto volume = codec->output_volume() + 10;
            if (volume > 100) {
                volume = 100;
            }
            codec->SetOutputVolume(volume);
        });

        volume_up_button_.OnLongPress([this]() { GetAudioCodec()->SetOutputVolume(100); });

        volume_down_button_.OnClick([this]() {
            auto codec = GetAudioCodec();
            auto volume = codec->output_volume() - 10;
            if (volume < 0) {
                volume = 0;
            }
            codec->SetOutputVolume(volume);
        });

        volume_down_button_.OnLongPress([this]() { GetAudioCodec()->SetOutputVolume(0); });
    }

    void InitializeGPIO() {
        ESP_LOGI(TAG, "Initializing GPIO...");

        // 配置状态LED
        if (STATUS_LED_PIN >= 0) {
            gpio_config_t led_cfg = {};
            led_cfg.pin_bit_mask = (1ULL << STATUS_LED_PIN);
            led_cfg.mode = GPIO_MODE_OUTPUT;
            led_cfg.pull_up_en = GPIO_PULLUP_DISABLE;
            led_cfg.pull_down_en = GPIO_PULLDOWN_DISABLE;
            gpio_config(&led_cfg);

            // LED闪烁表示系统运行
            gpio_set_level(STATUS_LED_PIN, 1);
            vTaskDelay(pdMS_TO_TICKS(100));
            gpio_set_level(STATUS_LED_PIN, 0);
            vTaskDelay(pdMS_TO_TICKS(100));
            gpio_set_level(STATUS_LED_PIN, 1);
        }

        ESP_LOGI(TAG, "GPIO initialized");
    }

    // SPI初始化（用于显示屏）
    void InitializeDisplayI2c() {
        i2c_master_bus_config_t bus_config = {
            .i2c_port = (i2c_port_t)0,
            .sda_io_num = DISPLAY_SDA_PIN,
            .scl_io_num = DISPLAY_SCL_PIN,
            .clk_source = I2C_CLK_SRC_DEFAULT,
            .glitch_ignore_cnt = 7,
            .intr_priority = 0,
            .trans_queue_depth = 0,
            .flags =
                {
                    .enable_internal_pullup = 1,
                },
        };
        ESP_ERROR_CHECK(i2c_new_master_bus(&bus_config, &display_i2c_bus_));
    }

    void InitializeSsd1306Display() {
        // SSD1306 config
        esp_lcd_panel_io_i2c_config_t io_config = {
            .dev_addr = 0x3C,
            .on_color_trans_done = nullptr,
            .user_ctx = nullptr,
            .control_phase_bytes = 1,
            .dc_bit_offset = 6,
            .lcd_cmd_bits = 8,
            .lcd_param_bits = 8,
            .flags =
                {
                    .dc_low_on_data = 0,
                    .disable_control_phase = 0,
                },
            .scl_speed_hz = 400 * 1000,
        };

        ESP_ERROR_CHECK(esp_lcd_new_panel_io_i2c_v2(display_i2c_bus_, &io_config, &panel_io_));

        ESP_LOGI(TAG, "Install SSD1306 driver");
        esp_lcd_panel_dev_config_t panel_config = {};
        panel_config.reset_gpio_num = -1;
        panel_config.bits_per_pixel = 1;

        esp_lcd_panel_ssd1306_config_t ssd1306_config = {
            .height = static_cast<uint8_t>(DISPLAY_HEIGHT),
        };
        panel_config.vendor_config = &ssd1306_config;

        ESP_ERROR_CHECK(esp_lcd_new_panel_ssd1306(panel_io_, &panel_config, &panel_));
        ESP_LOGI(TAG, "SSD1306 driver installed");

        // Reset the display
        ESP_ERROR_CHECK(esp_lcd_panel_reset(panel_));
        if (esp_lcd_panel_init(panel_) != ESP_OK) {
            ESP_LOGE(TAG, "Failed to initialize display");
            display_ = new NoDisplay();
            return;
        }

        // Set the display to on
        ESP_LOGI(TAG, "Turning display on");
        ESP_ERROR_CHECK(esp_lcd_panel_disp_on_off(panel_, true));

        display_ = new OledDisplay(panel_io_, panel_, DISPLAY_WIDTH, DISPLAY_HEIGHT,
                                   DISPLAY_MIRROR_X, DISPLAY_MIRROR_Y);
    }

public:
    virtual AudioCodec* GetAudioCodec() override {
        // static NoAudioCodecSimplex audio_codec(AUDIO_INPUT_SAMPLE_RATE, AUDIO_OUTPUT_SAMPLE_RATE,
        //     AUDIO_I2S_SPK_GPIO_BCLK, AUDIO_I2S_SPK_GPIO_LRCK, AUDIO_I2S_SPK_GPIO_DOUT,
        //     AUDIO_I2S_MIC_GPIO_SCK, AUDIO_I2S_MIC_GPIO_WS, AUDIO_I2S_MIC_GPIO_DIN);
        static NoAudioCodecDuplex audio_codec(AUDIO_INPUT_SAMPLE_RATE, AUDIO_OUTPUT_SAMPLE_RATE,
                                              AUDIO_I2S_SPK_GPIO_BCLK, AUDIO_I2S_SPK_GPIO_LRCK,
                                              AUDIO_I2S_SPK_GPIO_DOUT, AUDIO_I2S_MIC_GPIO_DIN);
        return &audio_codec;
        // return audio_codec_;
    }

    virtual Display* GetDisplay() override { return display_; }

    virtual Backlight* GetBacklight() override {
        // 返回nullptr，背光直接接3.3V，无需控制
        return nullptr;
    }
};

DECLARE_BOARD(SuperMiniC3Board);