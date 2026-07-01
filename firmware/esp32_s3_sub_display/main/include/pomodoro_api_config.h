#pragma once

#include "esp_err.h"

/** 加载后端地址：优先 SD 卡 config/pomodoro_host.txt，否则 menuconfig。 */
esp_err_t pomodoro_api_config_load();

const char *pomodoro_api_get_host();
int pomodoro_api_get_port();
