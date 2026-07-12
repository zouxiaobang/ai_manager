#include "pomodoro_api_config.h"

#include <cstring>

#include "app_settings.h"
#include "esp_log.h"

namespace {
constexpr char TAG[] = "pomo_api";
}  // namespace

esp_err_t pomodoro_api_config_load() {
  const AppSettings &s = app_settings_get();
  ESP_LOGI(TAG, "Pomodoro backend: %s:%u",
           s.pomodoro_host[0] ? s.pomodoro_host : "(not set)", s.pomodoro_port);
  return ESP_OK;
}

const char *pomodoro_api_get_host() {
  const AppSettings &s = app_settings_get();
  return s.pomodoro_host[0] != '\0' ? s.pomodoro_host : CONFIG_POMO_API_HOST;
}

int pomodoro_api_get_port() {
  const AppSettings &s = app_settings_get();
  return s.pomodoro_port != 0 ? s.pomodoro_port : CONFIG_POMO_API_PORT;
}
