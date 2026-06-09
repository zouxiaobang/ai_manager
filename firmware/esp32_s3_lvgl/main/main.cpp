#include "display.h"
#include "esp_err.h"
#include "esp_log.h"
#include "gt911_touch.h"
#include "modules/pomodoro_module.h"
#include "nvs_flash.h"
#include "ui.h"
#include "websocket_client.h"
#include "wifi_manager.h"

namespace {
constexpr char TAG[] = "app";
}

extern "C" void app_main(void) {
  esp_err_t err = nvs_flash_init();
  if (err == ESP_ERR_NVS_NO_FREE_PAGES || err == ESP_ERR_NVS_NEW_VERSION_FOUND) {
    ESP_ERROR_CHECK(nvs_flash_erase());
    err = nvs_flash_init();
  }
  ESP_ERROR_CHECK(err);

  ESP_ERROR_CHECK(display_init());
  ESP_ERROR_CHECK(ui_init());

  if (touch_init() != ESP_OK) {
    ESP_LOGW(TAG, "GT911 init failed, continue without touch");
    ui_set_status("Touch disabled");
  }

  display_start_lvgl_task();

  ESP_ERROR_CHECK(wifi_connect());

  pomodoro_module::request_bootstrap();

  // Replace this with a real play-state trigger later.
  ESP_ERROR_CHECK(websocket_start_playback_session());
}
