#pragma once

#include "esp_err.h"

/** 启动后台同步任务（WiFi + 计划拉取 + 会话轮询）。 */
esp_err_t pomodoro_sync_start();
