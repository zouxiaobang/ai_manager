#pragma once

#include "esp_err.h"
#include "pomodoro_model.h"

/** 从 NVS 加载缓存计划；无缓存时使用内置默认值。 */
esp_err_t pomodoro_plan_cache_load(PomodoroPlanConfig *out);

/** 将计划写入 NVS。 */
esp_err_t pomodoro_plan_cache_save(const PomodoroPlanConfig &plan);
