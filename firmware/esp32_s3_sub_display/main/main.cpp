#include "app_ui.h"

#include "assets_seed.h"
#include "board_io.h"
#include "display.h"
#include "esp_err.h"
#include "esp_log.h"
#include "esp_system.h"
#include "gt911_touch.h"

#include "hal/brownout_ll.h"
#include "soc/rtc_cntl_reg.h"
#include "soc/soc.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#include "nvs_flash.h"
#include "pixel_dog_sync.h"
#include "pomodoro_sync.h"
#include "pomodoro_plan_cache.h"
#include "pomodoro_model.h"
#include "sd_storage.h"
#include "wifi_sta.h"

namespace {
constexpr char TAG[] = "app";
}

static void print_reset_diagnostics() {
  esp_reset_reason_t reason = esp_reset_reason();
  const char *reason_str = "UNKNOWN";
  switch (reason) {
    case ESP_RST_POWERON:   reason_str = "POWERON";    break;
    case ESP_RST_EXT:       reason_str = "EXT_PIN";    break;
    case ESP_RST_SW:        reason_str = "SW_RESET";   break;
    case ESP_RST_PANIC:     reason_str = "PANIC";      break;
    case ESP_RST_INT_WDT:   reason_str = "INT_WDT";    break;
    case ESP_RST_TASK_WDT:  reason_str = "TASK_WDT";   break;
    case ESP_RST_WDT:       reason_str = "OTHER_WDT";  break;
    case ESP_RST_BROWNOUT:  reason_str = "BROWNOUT";   break;
    case ESP_RST_SDIO:      reason_str = "SDIO";       break;
    case ESP_RST_USB:       reason_str = "USB";        break;
    case ESP_RST_JTAG:      reason_str = "JTAG";       break;
    case ESP_RST_EFUSE:     reason_str = "EFUSE";      break;
    case ESP_RST_PWR_GLITCH: reason_str = "PWR_GLITCH"; break;
    case ESP_RST_CPU_LOCKUP: reason_str = "CPU_LOCKUP"; break;
    default: break;
  }

  const uint32_t brownout_reg = REG_READ(RTC_CNTL_BROWN_OUT_REG);
  ESP_LOGI(TAG, "=============================================");
  ESP_LOGI(TAG, " RESET DIAGNOSTIC");
  ESP_LOGI(TAG, "  reason         : %s (%d)", reason_str, static_cast<int>(reason));
  ESP_LOGI(TAG, "  brownout reg   : 0x%08lx", static_cast<unsigned long>(brownout_reg));
  ESP_LOGI(TAG, "    ena          : %d",  static_cast<int>((brownout_reg >> 30) & 1));
  ESP_LOGI(TAG, "    rst_ena      : %d",  static_cast<int>((brownout_reg >> 26) & 1));
  ESP_LOGI(TAG, "    ana_rst_en   : %d",  static_cast<int>((brownout_reg >> 28) & 1));
  ESP_LOGI(TAG, "    rst_sel      : %d",  static_cast<int>((brownout_reg >> 27) & 1));
  ESP_LOGI(TAG, "    det          : %d",  static_cast<int>((brownout_reg >> 31) & 1));
  ESP_LOGI(TAG, "=============================================");
}

extern "C" void app_main(void) {
  /* ---- Stage 0: early power stabilisation & diagnostics ---- */

  /* Let the power rail capacitors fully charge before any heavy init.
   * Voltage measured at board drops from 4.7V under load; this delay
   * ensures the regulator has reserve charge before the first spike. */
  vTaskDelay(pdMS_TO_TICKS(2000));

  print_reset_diagnostics();

  /* Belt-and-suspenders: disable ALL brownout mechanisms */
  RTCCNTL.fib_sel.val &= ~static_cast<uint32_t>(BROWNOUT_DETECTOR_LL_FIB_ENABLE);
  brownout_ll_ana_reset_enable(false);
  brownout_ll_bod_enable(false);
  brownout_ll_reset_config(false, 0, BROWNOUT_RESET_LEVEL_SYSTEM);

  /* ---- Stage 1: NVS ---- */
  esp_err_t err = nvs_flash_init();
  if (err == ESP_ERR_NVS_NO_FREE_PAGES || err == ESP_ERR_NVS_NEW_VERSION_FOUND) {
    ESP_ERROR_CHECK(nvs_flash_erase());
    err = nvs_flash_init();
  }
  ESP_ERROR_CHECK(err);

  ESP_LOGI(TAG, "ESP32-S3 sub display starting");
  ESP_LOGI(TAG, "Brownout reg after disable: 0x%08lx",
           static_cast<unsigned long>(REG_READ(RTC_CNTL_BROWN_OUT_REG)));

  /* ---- Stage 2: display init ---- */
  esp_err_t display_err = display_init();
  if (display_err != ESP_OK) {
    ESP_LOGE(TAG, "Display init failed (0x%x), stopping", display_err);
    return;
  }
  display_start_lvgl_task();

  /* Turn off backlight before WiFi phy_init (~500mA spike).
   * LCD panel keeps refreshing (framebuffer in PSRAM) but backlight
   * LED (~300mA) is off, keeping total draw under USB 500mA limit. */
  board_backlight_set(false);
  vTaskDelay(pdMS_TO_TICKS(200));

  /* ---- Stage 3: SD card ---- */
  if (sd_storage_init() != ESP_OK) {
    ESP_LOGW(TAG, "SD card unavailable, using built-in UI fallbacks");
  } else if (assets_seed_sdcard() != ESP_OK) {
    ESP_LOGW(TAG, "SD asset seed incomplete, missing files fall back to embed");
  }

  /* ---- Stage 4: UI build (touches PSRAM, draws framebuffer) ---- */
  ESP_ERROR_CHECK(app_ui_init());
  vTaskDelay(pdMS_TO_TICKS(200));

  /* Enter low-power mode immediately after UI build.
   * Backlight is already off; dropping PCLK + suspending LVGL brings
   * total board power to its minimum (only CPU+PSRAM idle ~80mA).
   * This 3s "charging window" lets the 220uF input cap (and bulk caps
   * on the 3.3V rail) fully charge before the WiFi phy_init spike.
   * Cold boot with discharged caps was causing brownout; this mimics
   * the "warm reboot" state where caps are already charged. */
  display_set_low_power(true);
  ESP_LOGI(TAG, "Charging window: 3s low-power charge before WiFi");
  vTaskDelay(pdMS_TO_TICKS(3000));

  /* ---- Stage 5: touch ---- */
  if (touch_init() != ESP_OK) {
    ESP_LOGW(TAG, "Touch init failed");
  }

  /* ---- Stage 6: pomodoro plan (NVS read, no big power draw) ---- */
  PomodoroPlanConfig cached_plan;
  if (pomodoro_plan_cache_load(&cached_plan) == ESP_OK) {
    pomodoro_apply_plan(cached_plan);
  }

  /* ---- Stage 7: WiFi-based sync (largest power surge: phy_init) ---- */
  if (pomodoro_sync_start() != ESP_OK) {
    ESP_LOGW(TAG, "Pomodoro sync not started (disabled or WiFi not configured)");
  }

  if (dog_sync_start() != ESP_OK) {
    ESP_LOGW(TAG, "Pixel Dog sync not started (disabled or WiFi not configured)");
  }

  /* ---- Stage 8: restore display + backlight after phy_init spike ---- */
  wifi_sta_wait_heavy_init_done(8000);
  display_set_low_power(false);
  if (board_backlight_on() != ESP_OK) {
    ESP_LOGW(TAG, "Backlight re-enable failed");
  }

  if (sd_storage_is_mounted()) {
    board_sd_cs_set(true);
  }

  ESP_LOGI(TAG, "Sub display running");
}
