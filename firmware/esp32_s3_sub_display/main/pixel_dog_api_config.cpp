#include "pixel_dog_api_config.h"

#include <cstring>

#include "app_settings.h"
#include "esp_log.h"

namespace {
constexpr char TAG[] = "dog_api";
}  // namespace

esp_err_t dog_api_config_load() {
  const AppSettings &s = app_settings_get();
  ESP_LOGI(TAG, "Pixel Dog backend: %s:%u",
           s.pixel_dog_host[0] ? s.pixel_dog_host : "(not set)", s.pixel_dog_port);
  return ESP_OK;
}

const char *dog_api_get_host() {
  const AppSettings &s = app_settings_get();
  return s.pixel_dog_host[0] != '\0' ? s.pixel_dog_host : CONFIG_PIXEL_DOG_API_HOST;
}

int dog_api_get_port() {
  const AppSettings &s = app_settings_get();
  return s.pixel_dog_port != 0 ? s.pixel_dog_port : CONFIG_PIXEL_DOG_API_PORT;
}
