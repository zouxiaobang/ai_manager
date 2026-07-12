#include "media_api_config.h"

#include <cstring>

#include "app_settings.h"
#include "esp_log.h"
#include "pomodoro_api_config.h"

namespace {
constexpr char TAG[] = "media_api";
}  // namespace

esp_err_t media_api_config_load() {
  const AppSettings &s = app_settings_get();
  if (s.media_host[0] != '\0') {
    ESP_LOGI(TAG, "Media bridge from settings: %s:%u", s.media_host, s.media_port);
  } else {
    const char *fallback = pomodoro_api_get_host();
    ESP_LOGI(TAG, "Media bridge fallback to pomodoro host: %s:8765", fallback);
  }
  return ESP_OK;
}

const char *media_api_get_host() {
  const AppSettings &s = app_settings_get();
  if (s.media_host[0] != '\0') {
    return s.media_host;
  }
  return pomodoro_api_get_host();
}

int media_api_get_port() {
  const AppSettings &s = app_settings_get();
  if (s.media_port != 0) {
    return s.media_port;
  }
  return 8765;
}
