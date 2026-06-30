#include "app_power.h"

#include "app_clock.h"
#include "app_settings.h"
#include "board_io.h"
#include "app_ui.h"
#include "display.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "lvgl.h"

namespace {
constexpr char TAG[] = "power";
constexpr int64_t kIdleCheckMs = 1000;

PowerVisualState visual = PowerVisualState::Bright;
lv_obj_t *dim_overlay = nullptr;
lv_obj_t *sleep_scr = nullptr;
int64_t last_activity_us = 0;
bool hardware_backlight_on = true;
}  // namespace

void app_power_init() {
  last_activity_us = esp_timer_get_time();
}

void app_power_set_visual(PowerVisualState state) {
  visual = state;
  display_lock();

  if (dim_overlay != nullptr) {
    if (state == PowerVisualState::Dimmed) {
      lv_obj_remove_flag(dim_overlay, LV_OBJ_FLAG_HIDDEN);
      lv_obj_move_foreground(dim_overlay);
      const uint8_t dim = app_settings_get().dim_brightness;
      lv_obj_set_style_bg_opa(dim_overlay, static_cast<lv_opa_t>(255 - dim * 255 / 100), 0);
    } else {
      lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_HIDDEN);
    }
  }

  if (sleep_scr != nullptr) {
    if (state == PowerVisualState::Sleeping) {
      lv_obj_remove_flag(sleep_scr, LV_OBJ_FLAG_HIDDEN);
      lv_scr_load(sleep_scr);
      board_backlight_set(false);
      hardware_backlight_on = false;
    } else if (!hardware_backlight_on) {
      board_backlight_set(true);
      hardware_backlight_on = true;
    }
  }

  display_unlock();
}

PowerVisualState app_power_get_visual() {
  return visual;
}

void app_power_bind_overlays(lv_obj_t *dim, lv_obj_t *sleep) {
  dim_overlay = dim;
  sleep_scr = sleep;
}

void app_power_notify_activity() {
  last_activity_us = esp_timer_get_time();
  if (visual == PowerVisualState::Sleeping) {
    app_power_wake_from_sleep();
    return;
  }
  if (visual == PowerVisualState::Dimmed) {
    app_power_set_visual(PowerVisualState::Bright);
  }
}

void app_power_enter_sleep() {
  app_power_set_visual(PowerVisualState::Sleeping);
}

void app_power_wake_from_sleep() {
  board_backlight_set(true);
  hardware_backlight_on = true;
  visual = PowerVisualState::Bright;
  last_activity_us = esp_timer_get_time();
  app_ui_show_home();
}

void app_power_tick(bool locked) {
  if (visual == PowerVisualState::Sleeping || locked) {
    return;
  }

  const AppSettings &s = app_settings_get();
  int h = 0;
  int m = 0;
  app_clock_get_hm(&h, &m);

  const bool night = app_settings_is_night_period(h, m);
  const int64_t idle_ms = (esp_timer_get_time() - last_activity_us) / 1000;
  const bool idle = idle_ms >= static_cast<int64_t>(s.idle_dim_minutes) * 60 * 1000;

  if (night || idle) {
    if (visual != PowerVisualState::Dimmed) {
      app_power_set_visual(PowerVisualState::Dimmed);
    }
  } else if (visual == PowerVisualState::Dimmed) {
    app_power_set_visual(PowerVisualState::Bright);
  }
}
