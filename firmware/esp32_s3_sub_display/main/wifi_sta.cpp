#include "wifi_sta.h"

#include <cstring>

#include "esp_event.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "mdns.h"
#include "freertos/FreeRTOS.h"
#include "freertos/event_groups.h"
#include "nvs_flash.h"

namespace {
constexpr char TAG[] = "wifi_sta";
constexpr int WIFI_CONNECTED_BIT = BIT0;
constexpr int WIFI_FAIL_BIT = BIT1;

EventGroupHandle_t s_wifi_event_group = nullptr;
int s_retry_count = 0;
constexpr int kMaxRetry = 10;

void event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data) {
  (void)arg;
  (void)event_data;
  if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
    esp_wifi_connect();
  } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
    if (s_retry_count < kMaxRetry) {
      esp_wifi_connect();
      s_retry_count++;
      ESP_LOGW(TAG, "Retry connect %d/%d", s_retry_count, kMaxRetry);
    } else {
      xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
    }
  } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
    s_retry_count = 0;
    xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    ESP_LOGI(TAG, "Got IP");
  }
}
}  // namespace

esp_err_t wifi_sta_connect() {
#if !CONFIG_POMO_SYNC_ENABLE
  return ESP_ERR_NOT_SUPPORTED;
#endif

#if CONFIG_POMO_SYNC_ENABLE
  if (std::strlen(CONFIG_WIFI_SSID) == 0 || std::strcmp(CONFIG_WIFI_SSID, "YOUR_SSID") == 0) {
    ESP_LOGW(TAG, "WiFi SSID not configured, skip connect");
    return ESP_ERR_INVALID_STATE;
  }

  if (s_wifi_event_group == nullptr) {
    s_wifi_event_group = xEventGroupCreate();
  }

  ESP_ERROR_CHECK(esp_netif_init());
  ESP_ERROR_CHECK(esp_event_loop_create_default());
  esp_netif_create_default_wifi_sta();

  wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
  ESP_ERROR_CHECK(esp_wifi_init(&cfg));

  ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &event_handler,
                                                      nullptr, nullptr));
  ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &event_handler,
                                                      nullptr, nullptr));

  wifi_config_t wifi_config = {};
  std::strncpy(reinterpret_cast<char *>(wifi_config.sta.ssid), CONFIG_WIFI_SSID,
               sizeof(wifi_config.sta.ssid) - 1);
  std::strncpy(reinterpret_cast<char *>(wifi_config.sta.password), CONFIG_WIFI_PASSWORD,
               sizeof(wifi_config.sta.password) - 1);
  wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

  ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
  ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
  ESP_ERROR_CHECK(esp_wifi_start());

  EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group, WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
                                         pdFALSE, pdFALSE, pdMS_TO_TICKS(30000));
  if (bits & WIFI_CONNECTED_BIT) {
    return ESP_OK;
  }
  ESP_LOGE(TAG, "WiFi connect failed");
  return ESP_FAIL;
#else
  return ESP_ERR_NOT_SUPPORTED;
#endif
}

bool wifi_sta_is_connected() {
#if CONFIG_POMO_SYNC_ENABLE
  if (s_wifi_event_group == nullptr) {
    return false;
  }
  return (xEventGroupGetBits(s_wifi_event_group) & WIFI_CONNECTED_BIT) != 0;
#else
  return false;
#endif
}

void wifi_sta_init_mdns() {
#if CONFIG_POMO_SYNC_ENABLE
  esp_err_t err = mdns_init();
  if (err != ESP_OK && err != ESP_ERR_INVALID_STATE) {
    ESP_LOGW(TAG, "mdns_init: %s", esp_err_to_name(err));
    return;
  }
  ESP_LOGI(TAG, "mDNS ready (supports hostname.local)");
#endif
}
