#include "media_sync.h"

#include <algorithm>
#include <cstdio>
#include <cstring>

#include "cJSON.h"
#include "esp_http_client.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "freertos/task.h"
#include "media_api_config.h"
#include "media_control.h"
#include "wifi_sta.h"

namespace {
constexpr char TAG[] = "media_sync";
constexpr int kHttpBufSize = 4096;
constexpr int kPollPlayingMs = 800;
constexpr int kPollIdleMs = 2000;
constexpr int kHttpTimeoutMs = 5000;
constexpr int kMaxBackoffMs = 30000;
constexpr int kWifiWaitMs = 60000;

char s_http_buf[kHttpBufSize];
MediaSnapshot s_snapshot = {};
SemaphoreHandle_t s_lock = nullptr;
volatile bool s_dirty = false;
volatile media_cmd_t s_pending_cmd = MEDIA_CMD_PREVIOUS;
volatile bool s_has_pending_cmd = false;
volatile bool s_pending_start = false;
int s_consecutive_failures = 0;

struct HttpResponse {
  int status = 0;
  int len = 0;
};

void copy_str(char *dst, size_t dst_len, const char *src) {
  if (dst == nullptr || dst_len == 0) {
    return;
  }
  if (src == nullptr) {
    dst[0] = '\0';
    return;
  }
  std::strncpy(dst, src, dst_len - 1);
  dst[dst_len - 1] = '\0';
}

void set_snapshot_locked(const MediaSnapshot &next) {
  s_snapshot = next;
  s_dirty = true;
}

esp_err_t http_event_handler(esp_http_client_event_t *evt) {
  auto *resp = static_cast<HttpResponse *>(evt->user_data);
  if (evt->event_id == HTTP_EVENT_ON_DATA && resp != nullptr) {
    if (resp->len < kHttpBufSize - 1) {
      const int copy = std::min(static_cast<int>(evt->data_len), kHttpBufSize - 1 - resp->len);
      std::memcpy(s_http_buf + resp->len, evt->data, copy);
      resp->len += copy;
      s_http_buf[resp->len] = '\0';
    }
  }
  return ESP_OK;
}

void api_base_url(char *out, size_t out_len) {
  std::snprintf(out, out_len, "http://%s:%d", media_api_get_host(), media_api_get_port());
}

bool http_request(const char *method, const char *path, const char *json_body, HttpResponse *resp) {
  char url[192];
  char base[96];
  api_base_url(base, sizeof(base));
  std::snprintf(url, sizeof(url), "%s%s", base, path);

  esp_http_client_config_t config = {};
  config.url = url;
  if (std::strcmp(method, "POST") == 0) {
    config.method = HTTP_METHOD_POST;
  } else {
    config.method = HTTP_METHOD_GET;
  }
  config.event_handler = http_event_handler;
  config.timeout_ms = kHttpTimeoutMs;
  config.keep_alive_enable = false;
  config.user_data = resp;

  resp->len = 0;
  resp->status = 0;
  s_http_buf[0] = '\0';

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return false;
  }
  if (json_body != nullptr) {
    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, json_body, std::strlen(json_body));
  }

  const esp_err_t err = esp_http_client_perform(client);
  resp->status = esp_http_client_get_status_code(client);
  esp_http_client_cleanup(client);
  return err == ESP_OK && resp->status >= 200 && resp->status < 300;
}

bool parse_status_response() {
  cJSON *root = cJSON_Parse(s_http_buf);
  if (root == nullptr) {
    return false;
  }
  const cJSON *data = cJSON_GetObjectItem(root, "data");
  if (!cJSON_IsObject(data)) {
    cJSON_Delete(root);
    return false;
  }

  MediaSnapshot next = {};
  next.connected = true;
  next.app_running = cJSON_IsTrue(cJSON_GetObjectItem(data, "app_running"));
  next.starting = cJSON_IsTrue(cJSON_GetObjectItem(data, "starting"));
  const cJSON *state = cJSON_GetObjectItem(data, "state");
  const char *state_str = cJSON_IsString(state) ? state->valuestring : "closed";
  next.playing = std::strcmp(state_str, "playing") == 0;
  copy_str(next.title, sizeof(next.title), cJSON_GetStringValue(cJSON_GetObjectItem(data, "title")));
  copy_str(next.artist, sizeof(next.artist), cJSON_GetStringValue(cJSON_GetObjectItem(data, "artist")));
  next.position_ms = cJSON_IsNumber(cJSON_GetObjectItem(data, "position_ms"))
                         ? cJSON_GetObjectItem(data, "position_ms")->valueint
                         : 0;
  next.duration_ms = cJSON_IsNumber(cJSON_GetObjectItem(data, "duration_ms"))
                         ? cJSON_GetObjectItem(data, "duration_ms")->valueint
                         : 0;
  next.updated_at_ms = cJSON_IsNumber(cJSON_GetObjectItem(data, "updated_at_ms"))
                           ? cJSON_GetObjectItem(data, "updated_at_ms")->valueint
                           : 0;

  const cJSON *lyrics = cJSON_GetObjectItem(data, "lyrics");
  if (cJSON_IsObject(lyrics)) {
    copy_str(next.prev_line, sizeof(next.prev_line),
             cJSON_GetStringValue(cJSON_GetObjectItem(lyrics, "prev_line")));
    copy_str(next.line, sizeof(next.line), cJSON_GetStringValue(cJSON_GetObjectItem(lyrics, "line")));
    copy_str(next.next_line, sizeof(next.next_line),
             cJSON_GetStringValue(cJSON_GetObjectItem(lyrics, "next_line")));
    next.line_start_ms = cJSON_IsNumber(cJSON_GetObjectItem(lyrics, "line_start_ms"))
                             ? cJSON_GetObjectItem(lyrics, "line_start_ms")->valueint
                             : 0;
    next.line_end_ms = cJSON_IsNumber(cJSON_GetObjectItem(lyrics, "line_end_ms"))
                           ? cJSON_GetObjectItem(lyrics, "line_end_ms")->valueint
                           : 0;
  }

  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    set_snapshot_locked(next);
    xSemaphoreGive(s_lock);
  }
  media_control_set_playing(next.playing);
  cJSON_Delete(root);
  return true;
}

bool pull_status() {
  HttpResponse resp;
  if (!http_request("GET", "/api/media/status", nullptr, &resp)) {
    return false;
  }
  return parse_status_response();
}

bool post_control(const char *command) {
  char body[96];
  std::snprintf(body, sizeof(body), "{\"command\":\"%s\"}", command);
  HttpResponse resp;
  return http_request("POST", "/api/media/control", body, &resp);
}

bool post_start() {
  HttpResponse resp;
  return http_request("POST", "/api/media/start", "{}", &resp);
}

const char *cmd_to_string(media_cmd_t cmd) {
  switch (cmd) {
    case MEDIA_CMD_PREVIOUS:
      return "previous";
    case MEDIA_CMD_TOGGLE_PLAY_PAUSE:
      return "toggle";
    case MEDIA_CMD_NEXT:
      return "next";
    default:
      return nullptr;
  }
}

void process_pending_actions() {
  if (s_pending_start) {
    s_pending_start = false;
    if (!post_start()) {
      ESP_LOGW(TAG, "Start NetEase request failed");
    }
  }

  if (!s_has_pending_cmd) {
    return;
  }

  media_cmd_t cmd = s_pending_cmd;
  s_has_pending_cmd = false;

  if (cmd == MEDIA_CMD_START_APP) {
    if (!post_start()) {
      ESP_LOGW(TAG, "Start NetEase request failed");
    }
    return;
  }

  const char *command = cmd_to_string(cmd);
  if (command == nullptr) {
    return;
  }
  if (!post_control(command)) {
    ESP_LOGW(TAG, "Control command failed: %s", command);
  }
}

int backoff_delay_ms() {
  switch (s_consecutive_failures) {
    case 0:
      return 0;
    case 1:
      return 3000;
    case 2:
      return 6000;
    default:
      return kMaxBackoffMs;
  }
}

bool wait_for_wifi() {
  const int step_ms = 500;
  int waited = 0;
  while (!wifi_sta_is_connected() && waited < kWifiWaitMs) {
    vTaskDelay(pdMS_TO_TICKS(step_ms));
    waited += step_ms;
  }
  return wifi_sta_is_connected();
}

void sync_task(void *arg) {
  (void)arg;
  media_api_config_load();

  if (!wait_for_wifi()) {
    ESP_LOGW(TAG, "WiFi not ready, media sync idle");
    vTaskDelete(nullptr);
    return;
  }

  char base[96];
  api_base_url(base, sizeof(base));
  ESP_LOGI(TAG, "Media sync with %s", base);

  esp_log_level_set("HTTP_CLIENT", ESP_LOG_ERROR);
  esp_log_level_set("esp-tls", ESP_LOG_ERROR);
  esp_log_level_set("transport_base", ESP_LOG_ERROR);

  for (;;) {
    const int backoff = backoff_delay_ms();
    if (backoff > 0) {
      vTaskDelay(pdMS_TO_TICKS(backoff));
    }

    if (!wifi_sta_is_connected()) {
      MediaSnapshot offline = {};
      if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
        set_snapshot_locked(offline);
        xSemaphoreGive(s_lock);
      }
      media_control_set_playing(false);
      vTaskDelay(pdMS_TO_TICKS(kPollIdleMs));
      continue;
    }

    process_pending_actions();

    const bool ok = pull_status();
    if (ok) {
      if (s_consecutive_failures > 0) {
        ESP_LOGI(TAG, "Media bridge restored");
      }
      s_consecutive_failures = 0;
    } else {
      s_consecutive_failures++;
      MediaSnapshot offline = {};
      if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
        set_snapshot_locked(offline);
        xSemaphoreGive(s_lock);
      }
      media_control_set_playing(false);
      ESP_LOGW(TAG, "Media status pull failed (%d)", s_consecutive_failures);
    }

    bool playing = false;
    if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(50)) == pdTRUE) {
      playing = s_snapshot.playing;
      xSemaphoreGive(s_lock);
    }
    vTaskDelay(pdMS_TO_TICKS(playing ? kPollPlayingMs : kPollIdleMs));
  }
}
}  // namespace

extern "C" {

esp_err_t media_sync_start() {
#if !CONFIG_MEDIA_SYNC_ENABLE
  ESP_LOGI(TAG, "Media sync disabled");
  return ESP_ERR_NOT_SUPPORTED;
#else
  if (s_lock == nullptr) {
    s_lock = xSemaphoreCreateMutex();
    if (s_lock == nullptr) {
      return ESP_ERR_NO_MEM;
    }
  }
  BaseType_t ok = xTaskCreate(sync_task, "media_sync", 8192, nullptr, 4, nullptr);
  return ok == pdPASS ? ESP_OK : ESP_FAIL;
#endif
}

bool media_sync_is_connected(void) {
  if (s_lock == nullptr) {
    return false;
  }
  bool connected = false;
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(50)) == pdTRUE) {
    connected = s_snapshot.connected;
    xSemaphoreGive(s_lock);
  }
  return connected;
}

void media_sync_get_snapshot(MediaSnapshot *out) {
  if (out == nullptr || s_lock == nullptr) {
    return;
  }
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    *out = s_snapshot;
    xSemaphoreGive(s_lock);
  }
}

bool media_sync_consume_dirty(void) {
  if (!s_dirty) {
    return false;
  }
  s_dirty = false;
  return true;
}

void media_sync_queue_command(media_cmd_t cmd) {
  s_pending_cmd = cmd;
  s_has_pending_cmd = true;
}

void media_sync_queue_start(void) {
  s_pending_start = true;
}

}  // extern "C"
