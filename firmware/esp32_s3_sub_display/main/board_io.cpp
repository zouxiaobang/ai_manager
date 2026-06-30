#include "board_io.h"

#include "driver/gpio.h"
#include "driver/i2c.h"
#include "esp_check.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "panel_config.h"

constexpr char TAG[] = "board_io";

namespace {
constexpr i2c_port_t kI2cPort = I2C_NUM_0;
constexpr uint32_t kI2cClockHz = 100000;
constexpr uint8_t kCh422WrSetAddr = 0x24;
constexpr uint8_t kCh422WrIoAddr = 0x38;
/** Enable bidirectional IO7-0 as outputs (esp_io_expander_ch422g IO_OE bit). */
constexpr uint8_t kCh422WrSetAllOutput = 0x01;

/** EXIO1 TP_RST, EXIO2 BL, EXIO3 LCD_RST, EXIO4 SD_CS (active low), EXIO5 USB_SEL (low=USB). */
constexpr uint8_t kExioPinMask = 0x3E;
uint8_t s_exio_data = 0;

esp_err_t ch422_write(uint8_t address, uint8_t value) {
  return i2c_master_write_to_device(kI2cPort, address, &value, 1, pdMS_TO_TICKS(100));
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
    return ESP_OK;
  }
  return err;
}

esp_err_t write_exio_data(uint8_t value) {
  s_exio_data = value & kExioPinMask;
  return ch422_write(kCh422WrIoAddr, s_exio_data);
}

uint8_t default_exio_data() {
  return static_cast<uint8_t>((1 << TP_RST_EXIO) | (1 << LCD_DISP_EXIO) | (1 << LCD_RST_EXIO) |
                              (1 << SD_CS_EXIO));
}
}  // namespace

esp_err_t board_io_init() {
  ESP_RETURN_ON_ERROR(init_i2c(), TAG, "Init I2C failed");
  ESP_RETURN_ON_ERROR(ch422_write(kCh422WrSetAddr, kCh422WrSetAllOutput), TAG, "CH422G WR_SET failed");
  ESP_RETURN_ON_ERROR(write_exio_data(default_exio_data()), TAG, "CH422G default outputs failed");
  ESP_LOGI(TAG, "CH422G ready, WR_SET=0x%02X EXIO=0x%02X (USB_SEL low)", kCh422WrSetAllOutput, s_exio_data);
  return ESP_OK;
}

esp_err_t board_backlight_on() {
  return board_backlight_set(true);
}

esp_err_t board_backlight_set(bool on) {
  uint8_t value = s_exio_data;
  if (on) {
    value |= static_cast<uint8_t>(1 << LCD_DISP_EXIO);
  } else {
    value &= static_cast<uint8_t>(~(1 << LCD_DISP_EXIO));
  }
  value |= static_cast<uint8_t>(1 << TP_RST_EXIO);
  ESP_RETURN_ON_ERROR(write_exio_data(value), TAG, "Backlight set failed");
  ESP_LOGI(TAG, "LCD backlight %s", on ? "on" : "off");
  return ESP_OK;
}

esp_err_t board_reset_touch() {
  gpio_set_direction(static_cast<gpio_num_t>(TP_IRQ), GPIO_MODE_OUTPUT);
  gpio_set_level(static_cast<gpio_num_t>(TP_IRQ), 0);

  const uint8_t rst_low = static_cast<uint8_t>(s_exio_data & ~(1 << TP_RST_EXIO));
  const uint8_t rst_high = static_cast<uint8_t>(s_exio_data | (1 << TP_RST_EXIO));
  ESP_RETURN_ON_ERROR(write_exio_data(rst_low), TAG, "TP_RST low failed");
  vTaskDelay(pdMS_TO_TICKS(100));
  ESP_RETURN_ON_ERROR(write_exio_data(rst_high), TAG, "TP_RST high failed");
  vTaskDelay(pdMS_TO_TICKS(200));

  gpio_set_direction(static_cast<gpio_num_t>(TP_IRQ), GPIO_MODE_INPUT);
  gpio_set_pull_mode(static_cast<gpio_num_t>(TP_IRQ), GPIO_PULLUP_ONLY);
  vTaskDelay(pdMS_TO_TICKS(20));
  ESP_LOGI(TAG, "GT911 reset via CH422G EXIO%d", TP_RST_EXIO);
  return ESP_OK;
}

esp_err_t board_sd_cs_set(bool selected) {
  uint8_t value = s_exio_data;
  if (selected) {
    value &= static_cast<uint8_t>(~(1 << SD_CS_EXIO));
  } else {
    value |= static_cast<uint8_t>(1 << SD_CS_EXIO);
  }
  ESP_RETURN_ON_ERROR(write_exio_data(value), TAG, "SD CS set failed");
  return ESP_OK;
}
