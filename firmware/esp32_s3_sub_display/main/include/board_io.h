#pragma once

#include "esp_err.h"

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

esp_err_t board_io_init();
esp_err_t board_backlight_on();
esp_err_t board_backlight_set(bool on);
esp_err_t board_reset_touch();

/** Configure GT911 IRQ (GPIO4) as input with pull-up after driver init. */
esp_err_t board_touch_irq_configure();

/** Serialize access to the shared I2C bus (CH422G + GT911). */
void board_i2c_lock();
void board_i2c_unlock();

/** Probe a 7-bit I2C address on the shared bus. */
bool board_i2c_probe(uint8_t address);

/** Log detected I2C devices (CH422G 0x24/0x38, GT911 0x5D/0x14). */
void board_i2c_scan_log();

/** TF card chip select on CH422G EXIO (active low). */
esp_err_t board_sd_cs_set(bool selected);

#ifdef __cplusplus
}
#endif
