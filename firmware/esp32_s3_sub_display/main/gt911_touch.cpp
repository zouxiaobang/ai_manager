#include "gt911_touch.h"

#include <cstdio>

#include "app_ui.h"
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
#include "board_io.h"
#include "panel_config.h"

namespace {
constexpr char TAG[] = "gt911";
constexpr i2c_port_t kI2cPort = I2C_NUM_0;
constexpr uint8_t kGt911DefaultAddr = ESP_LCD_TOUCH_IO_I2C_GT911_ADDRESS;
constexpr uint8_t kGt911BackupAddr = ESP_LCD_TOUCH_IO_I2C_GT911_ADDRESS_BACKUP;

esp_lcd_touch_handle_t touch_handle = nullptr;
lv_indev_t *touch_indev = nullptr;
portMUX_TYPE touch_state_lock = portMUX_INITIALIZER_UNLOCKED;
bool cached_pressed = false;
uint16_t cached_x = 0;
uint16_t cached_y = 0;
int64_t last_pressed_us = 0;
bool last_pressed = false;
uint16_t last_x = 0;
uint16_t last_y = 0;
bool prev_indev_pressed = false;

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

  ESP_RETURN_ON_ERROR(esp_lcd_new_panel_io_i2c(kI2cPort, &io_config, &io_handle), TAG, "Create panel IO failed");

  esp_lcd_touch_config_t touch_config = {};
  touch_config.x_max = PANEL_WIDTH;
  touch_config.y_max = PANEL_HEIGHT;
  touch_config.rst_gpio_num = GPIO_NUM_NC;
  touch_config.int_gpio_num = GPIO_NUM_NC;
  touch_config.flags.swap_xy = TOUCH_SWAP_XY;
  touch_config.flags.mirror_x = TOUCH_MIRROR_X;
  touch_config.flags.mirror_y = TOUCH_MIRROR_Y;
  touch_config.driver_data = &gt911_io_config;

  return esp_lcd_touch_new_i2c_gt911(io_handle, &touch_config, out_handle);
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

  if (pressed) {
#if PANEL_MIRROR_X
    x = static_cast<uint16_t>(PANEL_WIDTH - 1 - x);
#endif
#if PANEL_MIRROR_Y
    y = static_cast<uint16_t>(PANEL_HEIGHT - 1 - y);
#endif
    if (x >= PANEL_WIDTH) {
      x = static_cast<uint16_t>(PANEL_WIDTH - 1);
    }
    if (y >= PANEL_HEIGHT) {
      y = static_cast<uint16_t>(PANEL_HEIGHT - 1);
    }
  }

  if (pressed && !prev_indev_pressed) {
    app_ui_notify_activity();
  } else if (pressed) {
    app_ui_notify_activity();
  }
  prev_indev_pressed = pressed;

  data->state = pressed ? LV_INDEV_STATE_PRESSED : LV_INDEV_STATE_RELEASED;
  data->point.x = x;
  data->point.y = y;
}

void touch_poll_task(void *arg) {
  (void)arg;
  while (true) {
    if (esp_lcd_touch_read_data(touch_handle) == ESP_OK) {
      esp_lcd_touch_point_data_t point = {};
      uint8_t touch_count = 0;
      if (esp_lcd_touch_get_data(touch_handle, &point, &touch_count, 1) == ESP_OK && touch_count > 0) {
        last_x = point.x;
        last_y = point.y;
        last_pressed = true;
        last_pressed_us = esp_timer_get_time();

        portENTER_CRITICAL(&touch_state_lock);
        cached_pressed = true;
        cached_x = point.x;
        cached_y = point.y;
        portEXIT_CRITICAL(&touch_state_lock);
      } else {
        const bool keep_pressed = last_pressed && (esp_timer_get_time() - last_pressed_us < 180000);
        portENTER_CRITICAL(&touch_state_lock);
        cached_pressed = keep_pressed;
        portEXIT_CRITICAL(&touch_state_lock);
        if (last_pressed && !keep_pressed) {
          last_pressed = false;
        }
      }
    } else {
      portENTER_CRITICAL(&touch_state_lock);
      cached_pressed = false;
      portEXIT_CRITICAL(&touch_state_lock);
    }
    vTaskDelay(pdMS_TO_TICKS(30));
  }
}
}  // namespace

esp_err_t touch_init() {
  ESP_RETURN_ON_ERROR(board_reset_touch(), TAG, "Reset GT911 failed");
  vTaskDelay(pdMS_TO_TICKS(300));

  uint8_t address = GT911_I2C_ADDRESS != 0 ? GT911_I2C_ADDRESS : kGt911DefaultAddr;
  esp_err_t err = create_gt911(address, &touch_handle);
  if (err != ESP_OK && GT911_I2C_ADDRESS == 0) {
    ESP_LOGW(TAG, "GT911 0x%02X failed, try 0x%02X", kGt911DefaultAddr, kGt911BackupAddr);
    err = create_gt911(kGt911BackupAddr, &touch_handle);
  }
  ESP_RETURN_ON_ERROR(err, TAG, "Create GT911 failed");

  touch_indev = lv_indev_create();
  lv_indev_set_type(touch_indev, LV_INDEV_TYPE_POINTER);
  lv_indev_set_read_cb(touch_indev, gt911_read_cb);
  lv_display_t *disp = display_get();
  if (disp != nullptr) {
    lv_indev_set_display(touch_indev, disp);
  }

  xTaskCreate(touch_poll_task, "touch_poll", 4096, nullptr, 4, nullptr);
  ESP_LOGI(TAG, "GT911 touch ready");
  return ESP_OK;
}
