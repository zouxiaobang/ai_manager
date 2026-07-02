#pragma once

#include "esp_err.h"

esp_err_t media_api_config_load();
const char *media_api_get_host();
int media_api_get_port();
