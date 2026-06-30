#include "app_ui.h"

#include "board_io.h"
#include "display.h"
#include "esp_err.h"
#include "esp_log.h"
#include "gt911_touch.h"
#include "nvs_flash.h"
#include "sd_storage.h"

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

  ESP_LOGI(TAG, "ESP32-S3 sub display starting");
  ESP_ERROR_CHECK(display_init());
  display_start_lvgl_task();

  if (sd_storage_init() != ESP_OK) {
    ESP_LOGW(TAG, "SD card unavailable, using built-in UI fallbacks");
  }

  ESP_ERROR_CHECK(app_ui_init());

  if (touch_init() != ESP_OK) {
    ESP_LOGW(TAG, "Touch init failed");
  } else if (sd_storage_is_mounted()) {
    board_sd_cs_set(true);
  }

  ESP_LOGI(TAG, "Sub display running");
}
