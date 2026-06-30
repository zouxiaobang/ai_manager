#pragma once

#include "esp_err.h"

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

esp_err_t sd_storage_init();
bool sd_storage_is_mounted();

#ifdef __cplusplus
}
#endif
