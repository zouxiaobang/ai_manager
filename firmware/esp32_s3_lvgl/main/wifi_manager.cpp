#include "wifi_manager.h"

#include <cstring>

#include "esp_check.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "freertos/FreeRTOS.h"
#include "freertos/event_groups.h"
#include "panel_config.h"
#include "ui.h"

namespace {
constexpr char TAG[] = "wifi";
constexpr EventBits_t kConnectedBit = BIT0;

EventGroupHandle_t wifi_event_group = nullptr;
int disconnect_count = 0;

void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data) {
  (void)arg;

  if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
    esp_wifi_connect();
  } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
    if (wifi_event_group != nullptr) {
      xEventGroupClearBits(wifi_event_group, kConnectedBit);
    }
    disconnect_count++;
    esp_wifi_connect();
    ESP_LOGW(TAG, "Wi-Fi disconnected, retrying (%d)...", disconnect_count);
  } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
    auto *event = static_cast<ip_event_got_ip_t *>(event_data);
    ESP_LOGI(TAG, "Got IP: " IPSTR, IP2STR(&event->ip_info.ip));
    disconnect_count = 0;
    if (wifi_event_group != nullptr) {
      xEventGroupSetBits(wifi_event_group, kConnectedBit);
    }
  } else {
    (void)event_data;
  }
}

void log_local_ip() {
  esp_netif_t *netif = esp_netif_get_handle_from_ifkey("WIFI_STA_DEF");
  if (netif == nullptr) {
    ESP_LOGW(TAG, "Wi-Fi netif unavailable");
    return;
  }

  esp_netif_ip_info_t ip_info = {};
  if (esp_netif_get_ip_info(netif, &ip_info) != ESP_OK || ip_info.ip.addr == 0) {
    ESP_LOGW(TAG, "Wi-Fi has no IP address yet");
    return;
  }

  ESP_LOGI(TAG, "Local IP: " IPSTR " gateway: " IPSTR, IP2STR(&ip_info.ip), IP2STR(&ip_info.gw));
}
}  // namespace

esp_err_t wifi_connect() {
  ui_set_status("Connecting Wi-Fi...");

  if (wifi_event_group == nullptr) {
    wifi_event_group = xEventGroupCreate();
    ESP_RETURN_ON_FALSE(wifi_event_group != nullptr, ESP_ERR_NO_MEM, TAG, "Create event group failed");
  } else {
    xEventGroupClearBits(wifi_event_group, kConnectedBit);
  }

  static bool wifi_stack_ready = false;
  if (!wifi_stack_ready) {
    ESP_RETURN_ON_ERROR(esp_netif_init(), TAG, "Init netif failed");
    ESP_RETURN_ON_ERROR(esp_event_loop_create_default(), TAG, "Create default event loop failed");
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t init_config = WIFI_INIT_CONFIG_DEFAULT();
    ESP_RETURN_ON_ERROR(esp_wifi_init(&init_config), TAG, "Init Wi-Fi failed");

    ESP_RETURN_ON_ERROR(
      esp_event_handler_instance_register(WIFI_EVENT, ESP_EVENT_ANY_ID, wifi_event_handler, nullptr, nullptr),
      TAG,
      "Register Wi-Fi handler failed"
    );
    ESP_RETURN_ON_ERROR(
      esp_event_handler_instance_register(IP_EVENT, IP_EVENT_STA_GOT_IP, wifi_event_handler, nullptr, nullptr),
      TAG,
      "Register IP handler failed"
    );

    wifi_config_t wifi_config = {};
    std::strncpy(reinterpret_cast<char *>(wifi_config.sta.ssid), WIFI_SSID, sizeof(wifi_config.sta.ssid));
    std::strncpy(reinterpret_cast<char *>(wifi_config.sta.password), WIFI_PASSWORD, sizeof(wifi_config.sta.password));
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

    ESP_RETURN_ON_ERROR(esp_wifi_set_mode(WIFI_MODE_STA), TAG, "Set Wi-Fi mode failed");
    ESP_RETURN_ON_ERROR(esp_wifi_set_config(WIFI_IF_STA, &wifi_config), TAG, "Set Wi-Fi config failed");
    ESP_RETURN_ON_ERROR(esp_wifi_start(), TAG, "Start Wi-Fi failed");
    ESP_RETURN_ON_ERROR(esp_wifi_set_ps(WIFI_PS_NONE), TAG, "Disable Wi-Fi power save failed");
    wifi_stack_ready = true;
  } else {
    esp_wifi_connect();
  }

  EventBits_t bits = xEventGroupWaitBits(wifi_event_group, kConnectedBit, pdFALSE, pdFALSE, pdMS_TO_TICKS(30000));
  if (bits & kConnectedBit) {
    log_local_ip();
    ui_set_status("Wi-Fi connected");
    return ESP_OK;
  }

  ui_set_status("Wi-Fi failed");
  return ESP_ERR_TIMEOUT;
}

bool wifi_is_connected() {
  if (wifi_event_group == nullptr) {
    return false;
  }
  return (xEventGroupGetBits(wifi_event_group) & kConnectedBit) != 0;
}

esp_err_t wifi_ensure_connected() {
  if (wifi_is_connected()) {
    return ESP_OK;
  }

  ESP_LOGW(TAG, "Wi-Fi offline, reconnecting before network access...");
  ui_set_status("Reconnecting Wi-Fi...");
  esp_wifi_connect();

  EventBits_t bits = xEventGroupWaitBits(wifi_event_group, kConnectedBit, pdFALSE, pdFALSE, pdMS_TO_TICKS(15000));
  if (bits & kConnectedBit) {
    log_local_ip();
    ui_set_status("Wi-Fi connected");
    return ESP_OK;
  }

  ESP_LOGW(TAG, "Wi-Fi reconnect timed out");
  return ESP_ERR_TIMEOUT;
}
