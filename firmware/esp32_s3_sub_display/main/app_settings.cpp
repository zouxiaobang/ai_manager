#include "app_settings.h"

#include <cstring>

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

namespace {
constexpr char TAG[] = "settings";
constexpr char kNvsNs[] = "sub_disp";
constexpr char kNvsKey[] = "cfg";

AppSettings g_settings = {};
}  // namespace

void app_settings_init() {
  nvs_handle_t handle = 0;
  if (nvs_open(kNvsNs, NVS_READONLY, &handle) != ESP_OK) {
    ESP_LOGI(TAG, "Using default settings");
    return;
  }

  size_t len = sizeof(AppSettings);
  if (nvs_get_blob(handle, kNvsKey, &g_settings, &len) == ESP_OK && len == sizeof(AppSettings)) {
    ESP_LOGI(TAG, "Settings loaded from NVS");
  }
  nvs_close(handle);
}

const AppSettings &app_settings_get() {
  return g_settings;
}

void app_settings_set(const AppSettings &settings) {
  g_settings = settings;
}

void app_settings_save() {
  nvs_handle_t handle = 0;
  if (nvs_open(kNvsNs, NVS_READWRITE, &handle) != ESP_OK) {
    return;
  }
  nvs_set_blob(handle, kNvsKey, &g_settings, sizeof(AppSettings));
  nvs_commit(handle);
  nvs_close(handle);
  ESP_LOGI(TAG, "Settings saved");
}

bool app_settings_is_night_period(int hour, int min) {
  const AppSettings &s = app_settings_get();
  if (!s.night_dim_enable) {
    return false;
  }
  const int now = hour * 60 + min;
  const int start = s.night_start_hour * 60 + s.night_start_min;
  const int end = s.night_end_hour * 60 + s.night_end_min;
  if (start == end) {
    return false;
  }
  if (start < end) {
    return now >= start && now < end;
  }
  return now >= start || now < end;
}
