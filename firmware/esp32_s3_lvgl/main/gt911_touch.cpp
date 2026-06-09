#include "gt911_touch.h"

#include <cstdio>
#include <cstring>

#include "display.h"
#include "driver/gpio.h"
#include "driver/i2c.h"
#include "esp_check.h"
#include "esp_lcd_panel_io.h"
#include "esp_lcd_touch.h"
#include "esp_lcd_touch_gt911.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/portmacro.h"
#include "freertos/task.h"
#include "lvgl.h"
#include "panel_config.h"
#include "ui.h"

namespace {
constexpr char TAG[] = "gt911";
constexpr i2c_port_t kI2cPort = I2C_NUM_0;
constexpr uint32_t kI2cClockHz = 100000;
constexpr uint8_t kCh422OutputModeAddr = 0x24;
constexpr uint8_t kCh422OutputDataAddr = 0x38;
constexpr uint8_t kGt911DefaultAddr = ESP_LCD_TOUCH_IO_I2C_GT911_ADDRESS;
constexpr uint8_t kGt911BackupAddr = ESP_LCD_TOUCH_IO_I2C_GT911_ADDRESS_BACKUP;

esp_lcd_touch_handle_t touch_handle = nullptr;
lv_indev_t *touch_indev = nullptr;
uint16_t last_x = 0;
uint16_t last_y = 0;
bool last_pressed = false;
int64_t last_idle_log_us = 0;
int64_t last_touch_log_us = 0;
portMUX_TYPE touch_state_lock = portMUX_INITIALIZER_UNLOCKED;
bool cached_pressed = false;
uint16_t cached_x = 0;
uint16_t cached_y = 0;
int64_t last_indev_log_us = 0;
int64_t last_pressed_us = 0;
lv_point_t swipe_anchor = {};
bool swipe_anchor_valid = false;
bool prev_indev_pressed = false;

void map_touch_point(uint16_t &x, uint16_t &y) {
  (void)x;
#if TOUCH_INVERT_Y
  if (y < PANEL_HEIGHT) {
    y = static_cast<uint16_t>(PANEL_HEIGHT - 1 - y);
  }
#endif
}

esp_err_t i2c_probe(uint8_t address) {
  i2c_cmd_handle_t cmd = i2c_cmd_link_create();
  ESP_RETURN_ON_FALSE(cmd != nullptr, ESP_ERR_NO_MEM, TAG, "Create I2C cmd failed");

  i2c_master_start(cmd);
  i2c_master_write_byte(cmd, static_cast<uint8_t>((address << 1) | I2C_MASTER_WRITE), true);
  i2c_master_stop(cmd);
  esp_err_t err = i2c_master_cmd_begin(kI2cPort, cmd, pdMS_TO_TICKS(100));
  i2c_cmd_link_delete(cmd);
  return err;
}

esp_err_t ch422_write(uint8_t address, uint8_t value) {
  return i2c_master_write_to_device(kI2cPort, address, &value, 1, pdMS_TO_TICKS(100));
}

void scan_i2c_bus(char *summary, size_t summary_len) {
  if (summary != nullptr && summary_len > 0) {
    std::snprintf(summary, summary_len, "I2C:");
  }

  ESP_LOGI(TAG, "Scanning I2C bus");
  for (uint8_t addr = 1; addr < 127; addr++) {
    if (i2c_probe(addr) == ESP_OK) {
      ESP_LOGI(TAG, "Found I2C device at 0x%02X", addr);
      if (summary != nullptr && std::strlen(summary) + 6 < summary_len) {
        char item[8] = {};
        std::snprintf(item, sizeof(item), " %02X", addr);
        std::strcat(summary, item);
      }
    }
  }

  if (summary != nullptr && std::strcmp(summary, "I2C:") == 0) {
    std::snprintf(summary, summary_len, "I2C: none");
  }
}

void reset_gt911_with_ch422() {
  ESP_LOGI(TAG, "Reset GT911 through CH422G EXIO%d", TP_RST_EXIO);

  esp_err_t err = ch422_write(kCh422OutputModeAddr, 0x01);
  if (err != ESP_OK) {
    ESP_LOGW(TAG, "CH422G output mode write failed: %s", esp_err_to_name(err));
  }

  gpio_set_direction(static_cast<gpio_num_t>(TP_IRQ), GPIO_MODE_OUTPUT);
  gpio_set_level(static_cast<gpio_num_t>(TP_IRQ), 0);

  err = ch422_write(kCh422OutputDataAddr, 0x2C);
  if (err != ESP_OK) {
    ESP_LOGW(TAG, "CH422G TP_RST low failed: %s", esp_err_to_name(err));
  }
  vTaskDelay(pdMS_TO_TICKS(100));

  err = ch422_write(kCh422OutputDataAddr, 0x2E);
  if (err != ESP_OK) {
    ESP_LOGW(TAG, "CH422G TP_RST high failed: %s", esp_err_to_name(err));
  }
  vTaskDelay(pdMS_TO_TICKS(200));

  gpio_set_direction(static_cast<gpio_num_t>(TP_IRQ), GPIO_MODE_INPUT);
  gpio_set_pull_mode(static_cast<gpio_num_t>(TP_IRQ), GPIO_PULLUP_ONLY);
  vTaskDelay(pdMS_TO_TICKS(20));
}

esp_err_t init_i2c() {
  i2c_config_t config = {};
  config.mode = I2C_MODE_MASTER;
  config.sda_io_num = static_cast<gpio_num_t>(TP_SDA);
  config.scl_io_num = static_cast<gpio_num_t>(TP_SCL);
  config.sda_pullup_en = GPIO_PULLUP_DISABLE;
  config.scl_pullup_en = GPIO_PULLUP_DISABLE;
  config.master.clk_speed = kI2cClockHz;

  ESP_RETURN_ON_ERROR(i2c_param_config(kI2cPort, &config), TAG, "Configure I2C failed");
  esp_err_t err = i2c_driver_install(kI2cPort, config.mode, 0, 0, 0);
  if (err == ESP_ERR_INVALID_STATE) {
    ESP_LOGW(TAG, "I2C driver already installed");
    return ESP_OK;
  }
  return err;
}

esp_err_t create_gt911(uint8_t address, esp_lcd_touch_handle_t *out_handle) {
  esp_lcd_panel_io_handle_t io_handle = nullptr;
  static esp_lcd_touch_io_gt911_config_t gt911_io_config = {};
  gt911_io_config.dev_addr = address;

  esp_lcd_panel_io_i2c_config_t io_config = {};
  io_config.dev_addr = address;
  io_config.control_phase_bytes = 1;
  io_config.dc_bit_offset = 0;
  io_config.lcd_cmd_bits = 16;
  io_config.lcd_param_bits = 0;
  io_config.flags.disable_control_phase = 1;

  esp_err_t err = esp_lcd_new_panel_io_i2c(kI2cPort, &io_config, &io_handle);
  if (err != ESP_OK) {
    char message[96] = {};
    std::snprintf(message, sizeof(message), "Touch fail: IO 0x%02X %s", address, esp_err_to_name(err));
    ui_set_touch_debug(message);
    ESP_LOGE(TAG, "Create GT911 panel IO 0x%02X failed: %s", address, esp_err_to_name(err));
    return err;
  }

  esp_lcd_touch_config_t touch_config = {};
  touch_config.x_max = PANEL_WIDTH;
  touch_config.y_max = PANEL_HEIGHT;
  touch_config.rst_gpio_num = static_cast<gpio_num_t>(-1);
  touch_config.int_gpio_num = static_cast<gpio_num_t>(-1);
  touch_config.levels.reset = 0;
  touch_config.levels.interrupt = 0;
  touch_config.flags.swap_xy = TOUCH_SWAP_XY;
  touch_config.flags.mirror_x = TOUCH_MIRROR_X;
  touch_config.flags.mirror_y = TOUCH_MIRROR_Y;
  touch_config.driver_data = &gt911_io_config;

  err = esp_lcd_touch_new_i2c_gt911(io_handle, &touch_config, out_handle);
  if (err != ESP_OK) {
    char message[96] = {};
    std::snprintf(message, sizeof(message), "Touch fail: GT911 0x%02X %s", address, esp_err_to_name(err));
    ui_set_touch_debug(message);
    ESP_LOGE(TAG, "Create GT911 touch 0x%02X failed: %s", address, esp_err_to_name(err));
  }

  return err;
}

void gt911_read_cb(lv_indev_t *indev, lv_indev_data_t *data) {
  (void)indev;

  bool pressed = false;
  uint16_t x = 0;
  uint16_t y = 0;

  portENTER_CRITICAL(&touch_state_lock);
  pressed = cached_pressed;
  x = cached_x;
  y = cached_y;
  portEXIT_CRITICAL(&touch_state_lock);

  map_touch_point(x, y);

  if (pressed && !prev_indev_pressed) {
    swipe_anchor.x = static_cast<lv_coord_t>(x);
    swipe_anchor.y = static_cast<lv_coord_t>(y);
    swipe_anchor_valid = true;
    ui_note_touch_press(static_cast<int>(x), static_cast<int>(y));
  } else if (!pressed && prev_indev_pressed && swipe_anchor_valid) {
    const int dx = static_cast<int>(x) - swipe_anchor.x;
    const int dy = static_cast<int>(y) - swipe_anchor.y;
    ui_handle_swipe_release(dx, dy);
    swipe_anchor_valid = false;
  }
  prev_indev_pressed = pressed;

  data->state = pressed ? LV_INDEV_STATE_PRESSED : LV_INDEV_STATE_RELEASED;
  data->point.x = x;
  data->point.y = y;

  if (pressed) {
    int64_t now = esp_timer_get_time();
    if (now - last_indev_log_us > 120000) {
      last_indev_log_us = now;
      ESP_LOGI(TAG, "LVGL indev read pressed=(%u,%u)", x, y);
    }
  }
}

void touch_poll_task(void *arg) {
  (void)arg;

  while (true) {
    esp_err_t err = esp_lcd_touch_read_data(touch_handle);
    if (err == ESP_OK) {
      esp_lcd_touch_point_data_t point = {};
      uint8_t touch_count = 0;
      err = esp_lcd_touch_get_data(touch_handle, &point, &touch_count, 1);

      if (err == ESP_OK && touch_count > 0) {
        last_x = point.x;
        last_y = point.y;
        last_pressed = true;
        last_pressed_us = esp_timer_get_time();

        portENTER_CRITICAL(&touch_state_lock);
        cached_pressed = true;
        cached_x = point.x;
        cached_y = point.y;
        portEXIT_CRITICAL(&touch_state_lock);

        int64_t now = esp_timer_get_time();
        if (now - last_touch_log_us > 120000) {
          last_touch_log_us = now;
          ESP_LOGI(TAG, "GT911 touch mapped=(%u,%u) strength=%u irq=%d", point.x, point.y, point.strength,
                   gpio_get_level(static_cast<gpio_num_t>(TP_IRQ)));
        }
        ui_show_touch_point(point.x, point.y, true);
      } else {
        bool keep_pressed = last_pressed && (esp_timer_get_time() - last_pressed_us < 180000);
        portENTER_CRITICAL(&touch_state_lock);
        cached_pressed = keep_pressed;
        portEXIT_CRITICAL(&touch_state_lock);

        if (last_pressed && !keep_pressed) {
          last_pressed = false;
          ui_show_touch_point(last_x, last_y, false);
          ESP_LOGI(TAG, "GT911 touch released");
        }

        int64_t now = esp_timer_get_time();
        if (now - last_idle_log_us > 2000000) {
          last_idle_log_us = now;
          ESP_LOGI(TAG, "GT911 poll alive, irq=%d", gpio_get_level(static_cast<gpio_num_t>(TP_IRQ)));
        }
      }
    } else {
      portENTER_CRITICAL(&touch_state_lock);
      cached_pressed = false;
      portEXIT_CRITICAL(&touch_state_lock);

      int64_t now = esp_timer_get_time();
      if (now - last_idle_log_us > 2000000) {
        last_idle_log_us = now;
        ESP_LOGW(TAG, "GT911 read failed: %s", esp_err_to_name(err));
      }
    }

    vTaskDelay(pdMS_TO_TICKS(50));
  }
}
}  // namespace

esp_err_t touch_init() {
  ui_set_touch_debug("Touch: init I2C");
  esp_err_t err = init_i2c();
  if (err != ESP_OK) {
    ui_set_touch_debug("Touch fail: I2C init");
    ESP_LOGE(TAG, "Init I2C failed: %s", esp_err_to_name(err));
    return err;
  }

  ui_set_touch_debug("Touch: reset GT911");
  reset_gt911_with_ch422();

  char scan_summary[96] = {};
  scan_i2c_bus(scan_summary, sizeof(scan_summary));
  ui_set_touch_debug(scan_summary);
  vTaskDelay(pdMS_TO_TICKS(800));

  uint8_t address = GT911_I2C_ADDRESS != 0 ? GT911_I2C_ADDRESS : kGt911DefaultAddr;
  char message[96] = {};
  std::snprintf(message, sizeof(message), "Touch: try GT911 0x%02X", address);
  ui_set_touch_debug(message);

  err = create_gt911(address, &touch_handle);
  if (err != ESP_OK && GT911_I2C_ADDRESS == 0) {
    ESP_LOGW(TAG, "GT911 init at 0x%02X failed, try 0x%02X", kGt911DefaultAddr, kGt911BackupAddr);
    std::snprintf(message, sizeof(message), "GT911 0x%02X fail, try 0x%02X", kGt911DefaultAddr, kGt911BackupAddr);
    ui_set_touch_debug(message);
    err = create_gt911(kGt911BackupAddr, &touch_handle);
  }
  if (err != ESP_OK) {
    ESP_LOGE(TAG, "Create GT911 touch failed: %s", esp_err_to_name(err));
    return err;
  }

  touch_indev = lv_indev_create();
  lv_indev_set_type(touch_indev, LV_INDEV_TYPE_POINTER);
  lv_indev_set_read_cb(touch_indev, gt911_read_cb);
  lv_display_t *disp = display_get();
  if (disp != nullptr) {
    lv_indev_set_display(touch_indev, disp);
  }

  ESP_LOGI(TAG, "GT911 LVGL9 input device registered");
  ui_set_touch_debug("Touch: GT911 ready");
#if TOUCH_POLL_TASK_ENABLED
  xTaskCreate(touch_poll_task, "touch_poll", 4096, nullptr, 4, nullptr);
  ESP_LOGI(TAG, "GT911 touch poll task started for LVGL indev cache");
#endif
  return ESP_OK;
}
