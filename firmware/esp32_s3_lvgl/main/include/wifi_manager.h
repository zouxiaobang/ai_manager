#pragma once

#include "esp_err.h"

esp_err_t wifi_connect();
bool wifi_is_connected();
esp_err_t wifi_ensure_connected();
