#pragma once

#include "esp_err.h"

esp_err_t dog_api_config_load();

const char *dog_api_get_host();
int dog_api_get_port();