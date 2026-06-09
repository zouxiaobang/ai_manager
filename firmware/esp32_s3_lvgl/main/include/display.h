#pragma once

#include "esp_err.h"
#include "lvgl.h"

esp_err_t display_init();
void display_start_lvgl_task();
void display_lock();
void display_unlock();
lv_display_t *display_get();
