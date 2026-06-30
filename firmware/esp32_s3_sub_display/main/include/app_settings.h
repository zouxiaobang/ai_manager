#pragma once

#include <cstdint>

struct AppSettings {
  uint8_t brightness = 100;
  uint8_t dim_brightness = 30;
  uint8_t idle_dim_minutes = 5;
  bool night_dim_enable = true;
  uint8_t night_start_hour = 22;
  uint8_t night_start_min = 0;
  uint8_t night_end_hour = 8;
  uint8_t night_end_min = 0;
  uint8_t font_scale = 1;
};

void app_settings_init();
const AppSettings &app_settings_get();
void app_settings_set(const AppSettings &settings);
void app_settings_save();

bool app_settings_is_night_period(int hour, int min);
