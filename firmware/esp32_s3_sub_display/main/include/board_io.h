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

/** TF card chip select on CH422G EXIO (active low). */
esp_err_t board_sd_cs_set(bool selected);

#ifdef __cplusplus
}
#endif
