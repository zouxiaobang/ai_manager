#pragma once

#include "lvgl.h"

enum class PowerVisualState { Bright, Dimmed, Sleeping };

void app_power_init();
void app_power_set_visual(PowerVisualState state);
PowerVisualState app_power_get_visual();
void app_power_notify_activity();
void app_power_enter_sleep();
void app_power_wake_from_sleep();
void app_power_bind_overlays(lv_obj_t *dim, lv_obj_t *sleep);
void app_power_tick(bool locked);
