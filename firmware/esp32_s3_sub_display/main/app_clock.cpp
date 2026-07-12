#include "app_clock.h"

#include <cstdio>
#include <ctime>
#include <sys/time.h>

#include "esp_log.h"
#include "esp_sntp.h"
#include "esp_timer.h"
#include "wifi_sta.h"

namespace {
constexpr char TAG[] = "clock";
bool synced = false;
int64_t boot_base_us = 0;
int boot_offset_minutes = 8 * 60;

void time_sync_cb(struct timeval *tv) {
  (void)tv;
  synced = true;
  ESP_LOGI(TAG, "SNTP time synced");
}

bool g_sntp_started = false;
}  // namespace

void app_clock_init() {
  boot_base_us = esp_timer_get_time();

  setenv("TZ", "CST-8", 1);
  tzset();

  struct timeval tv = {};
  tv.tv_sec = 1704067200;
  settimeofday(&tv, nullptr);
  synced = false;
  g_sntp_started = false;

  ESP_LOGI(TAG, "Clock init, SNTP will start after WiFi");
}

void app_clock_tick() {
  if (synced || g_sntp_started) {
    return;
  }
  if (!wifi_sta_is_connected()) {
    return;
  }
  g_sntp_started = true;
  esp_sntp_setoperatingmode(SNTP_OPMODE_POLL);
  esp_sntp_setservername(0, "pool.ntp.org");
  esp_sntp_set_time_sync_notification_cb(time_sync_cb);
  esp_sntp_init();
  ESP_LOGI(TAG, "SNTP started, polling pool.ntp.org");
}

bool app_clock_is_synced() {
  return synced;
}

void app_clock_get_hm(int *hour, int *min) {
  time_t now = time(nullptr);
  struct tm tm_info = {};
  localtime_r(&now, &tm_info);
  if (!synced && now < 1704153600) {
    const int64_t elapsed_min = (esp_timer_get_time() - boot_base_us) / 60000000LL;
    const int total = (boot_offset_minutes + static_cast<int>(elapsed_min)) % (24 * 60);
    if (hour != nullptr) {
      *hour = total / 60;
    }
    if (min != nullptr) {
      *min = total % 60;
    }
    return;
  }
  if (hour != nullptr) {
    *hour = tm_info.tm_hour;
  }
  if (min != nullptr) {
    *min = tm_info.tm_min;
  }
}

void app_clock_format_time(char *buf, size_t len) {
  int h = 0;
  int m = 0;
  app_clock_get_hm(&h, &m);
  snprintf(buf, len, "%02d:%02d", h, m);
}
