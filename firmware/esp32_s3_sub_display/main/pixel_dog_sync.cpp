#include "pixel_dog_sync.h"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <cinttypes>

#include "cJSON.h"
#include "esp_http_client.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "pixel_dog_api_config.h"
#include "pixel_dog_model.h"
#include "wifi_sta.h"


constexpr int kPollIntervalMs = 15000;
constexpr int kMaxBackoffMs = 30000;

constexpr char TAG[] = "dog_sync";
constexpr int kHttpBufSize = 4096;
constexpr int kHttpTimeoutMs = 5000;

char s_http_buf[kHttpBufSize];
TaskHandle_t s_sync_task_handle = nullptr;

struct HttpResponse {
  int status = 0;
  int len = 0;
};

struct SyncBackoff {
  int consecutive_failures = 0;
  int64_t last_error_log_us = 0;
};

SyncBackoff g_backoff;
volatile bool g_sync_dirty = false;

int sync_backoff_delay_ms() {
  switch (g_backoff.consecutive_failures) {
    case 0:
      return kPollIntervalMs;
    case 1:
      return 10000;
    case 2:
      return 15000;
    case 3:
      return 20000;
    default:
      return kMaxBackoffMs;
  }
}

void note_sync_success() {
  if (g_backoff.consecutive_failures > 0) {
    ESP_LOGI(TAG, "Pixel Dog backend sync restored");
  }
  g_backoff.consecutive_failures = 0;
}

void note_sync_failure(const char *action) {
  g_backoff.consecutive_failures++;
  const int64_t now_us = esp_timer_get_time();
  if (g_backoff.consecutive_failures == 1 ||
      now_us - g_backoff.last_error_log_us >= 30000000) {
    ESP_LOGW(TAG, "%s failed (%d consecutive), retry in %ds", action, g_backoff.consecutive_failures,
             sync_backoff_delay_ms() / 1000);
    g_backoff.last_error_log_us = now_us;
  }
}

bool sync_network_ready() {
  return wifi_sta_is_connected();
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
  std::snprintf(out, out_len, "http://%s:%d", dog_api_get_host(), dog_api_get_port());
}

esp_err_t http_request(const char *method, const char *path, const char *json_body,
                       HttpResponse *resp) {
  char url[160];
  char base[96];
  api_base_url(base, sizeof(base));
  std::snprintf(url, sizeof(url), "%s%s", base, path);

  esp_http_client_config_t config = {};
  config.url = url;
  if (std::strcmp(method, "PUT") == 0) {
    config.method = HTTP_METHOD_PUT;
  } else if (std::strcmp(method, "POST") == 0) {
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
    return ESP_FAIL;
  }

  esp_http_client_set_header(client, "Content-Type", "application/json");
  if (json_body != nullptr) {
    esp_http_client_set_post_field(client, json_body, std::strlen(json_body));
  }

  esp_err_t err = esp_http_client_perform(client);
  if (err == ESP_OK) {
    resp->status = esp_http_client_get_status_code(client);
  }
  esp_http_client_cleanup(client);
  return err;
}

bool api_ok(const HttpResponse &resp, cJSON **root_out) {
  if (resp.status != 200 || resp.len <= 0) {
    if (resp.status != 0) {
      ESP_LOGW(TAG, "HTTP status=%d body_len=%d", resp.status, resp.len);
    }
    return false;
  }
  cJSON *root = cJSON_Parse(s_http_buf);
  if (root == nullptr) {
    return false;
  }
  cJSON *code = cJSON_GetObjectItem(root, "code");
  if (!cJSON_IsNumber(code) || code->valueint != 0) {
    cJSON_Delete(root);
    return false;
  }
  *root_out = root;
  return true;
}

bool push_dog_state() {
  const DogState *state = dog_model_get();
  char body[512];
  std::snprintf(body, sizeof(body),
                "{\"level\":%" PRIu32 ",\"xp\":%" PRIu32 ",\"xpNext\":%" PRIu32 ",\"bond\":%" PRIu32 ","
                "\"emotion\":%d,\"lastInteractTs\":%" PRId64 ",\"lastGreetTs\":%" PRId64 ","
                "\"status\":%d,\"unlockedItems\":%" PRIu8 "}",
                state->level, state->xp, state->xp_next, state->bond,
                state->emotion, state->last_interact_ts,
                state->last_greet_ts,
                state->status, state->unlocked_items);

  HttpResponse resp;
  if (http_request("PUT", "/api/pixel-dog/state", body, &resp) != ESP_OK) {
    return false;
  }

  cJSON *root = nullptr;
  const bool ok = api_ok(resp, &root);
  if (root != nullptr) {
    cJSON_Delete(root);
  }
  return ok;
}

bool pull_dog_state() {
  HttpResponse resp;
  if (http_request("GET", "/api/pixel-dog/state", nullptr, &resp) != ESP_OK) {
    return false;
  }

  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }

  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    cJSON_Delete(root);
    return true;
  }

  DogState state = {};
  cJSON *level = cJSON_GetObjectItem(data, "level");
  cJSON *xp = cJSON_GetObjectItem(data, "xp");
  cJSON *xp_next = cJSON_GetObjectItem(data, "xpNext");
  cJSON *bond = cJSON_GetObjectItem(data, "bond");
  cJSON *emotion = cJSON_GetObjectItem(data, "emotion");
  cJSON *last_interact = cJSON_GetObjectItem(data, "lastInteractTs");
  cJSON *last_greet = cJSON_GetObjectItem(data, "lastGreetTs");
  cJSON *status = cJSON_GetObjectItem(data, "status");
  cJSON *unlocked = cJSON_GetObjectItem(data, "unlockedItems");

  if (cJSON_IsNumber(level)) state.level = level->valueint;
  if (cJSON_IsNumber(xp)) state.xp = xp->valueint;
  if (cJSON_IsNumber(xp_next)) state.xp_next = xp_next->valueint;
  if (cJSON_IsNumber(bond)) state.bond = bond->valueint;
  if (cJSON_IsNumber(emotion)) state.emotion = static_cast<int8_t>(emotion->valueint);
  if (cJSON_IsNumber(last_interact)) state.last_interact_ts = static_cast<int64_t>(last_interact->valuedouble);
  if (cJSON_IsNumber(last_greet)) state.last_greet_ts = static_cast<int64_t>(last_greet->valuedouble);
  if (cJSON_IsNumber(status)) state.status = static_cast<DogStatus>(status->valueint);
  if (cJSON_IsNumber(unlocked)) state.unlocked_items = unlocked->valueint;
  cJSON *equipped = cJSON_GetObjectItem(data, "equippedItems");
  if (cJSON_IsNumber(equipped)) state.equipped_items = (uint64_t)equipped->valuedouble;

  if (state.level > 0) {
    dog_model_apply_remote_state(&state);
    ESP_LOGI(TAG, "Pull success: level=%u, xp=%u/%u, bond=%u, emotion=%d, status=%d",
             state.level, state.xp, state.xp_next, state.bond, state.emotion, state.status);
    }

  cJSON_Delete(root);
  return true;
}

bool dog_sync_interact(const char *action) {
  if (!sync_network_ready()) {
    return false;
  }

  char path[80];
  std::snprintf(path, sizeof(path), "/api/pixel-dog/interact?action=%s", action);

  HttpResponse resp;
  if (http_request("POST", path, nullptr, &resp) != ESP_OK) {
    return false;
  }

  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }

  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    cJSON_Delete(root);
    return false;
  }

  DogState state = {};
  cJSON *level = cJSON_GetObjectItem(data, "level");
  cJSON *xp = cJSON_GetObjectItem(data, "xp");
  cJSON *xp_next = cJSON_GetObjectItem(data, "xpNext");
  cJSON *bond = cJSON_GetObjectItem(data, "bond");
  cJSON *emotion = cJSON_GetObjectItem(data, "emotion");
  cJSON *last_interact = cJSON_GetObjectItem(data, "lastInteractTs");
  cJSON *last_greet = cJSON_GetObjectItem(data, "lastGreetTs");
  cJSON *status = cJSON_GetObjectItem(data, "status");
  cJSON *unlocked = cJSON_GetObjectItem(data, "unlockedItems");

  if (cJSON_IsNumber(level)) state.level = level->valueint;
  if (cJSON_IsNumber(xp)) state.xp = xp->valueint;
  if (cJSON_IsNumber(xp_next)) state.xp_next = xp_next->valueint;
  if (cJSON_IsNumber(bond)) state.bond = bond->valueint;
  if (cJSON_IsNumber(emotion)) state.emotion = static_cast<int8_t>(emotion->valueint);
  if (cJSON_IsNumber(last_interact)) state.last_interact_ts = static_cast<int64_t>(last_interact->valuedouble);
  if (cJSON_IsNumber(last_greet)) state.last_greet_ts = static_cast<int64_t>(last_greet->valuedouble);
  if (cJSON_IsNumber(status)) state.status = static_cast<DogStatus>(status->valueint);
  if (cJSON_IsNumber(unlocked)) state.unlocked_items = unlocked->valueint;
  cJSON *equipped = cJSON_GetObjectItem(data, "equippedItems");
  if (cJSON_IsNumber(equipped)) state.equipped_items = (uint64_t)equipped->valuedouble;

  if (state.level > 0) {
    dog_model_override_state(&state);
    ESP_LOGI(TAG, "Interact '%s' success: level=%u, bond=%u, emotion=%d",
             action, state.level, state.bond, state.emotion);
  }

  cJSON_Delete(root);
  return true;
}

void sync_task(void *arg) {
  (void)arg;

  dog_api_config_load();

  // Wait for main WiFi to be connected (pomodoro_sync or app_main will call wifi_sta_connect)
  const int kMaxWaitMs = 15000;
  const int step_ms = 500;
  int waited = 0;
  while (!wifi_sta_is_connected() && waited < kMaxWaitMs) {
    vTaskDelay(pdMS_TO_TICKS(step_ms));
    waited += step_ms;
  }

  if (!wifi_sta_is_connected()) {
    ESP_LOGW(TAG, "WiFi unavailable, offline mode");
    vTaskDelete(nullptr);
    return;
  }

  esp_log_level_set("HTTP_CLIENT", ESP_LOG_ERROR);
  esp_log_level_set("esp-tls", ESP_LOG_ERROR);
  esp_log_level_set("transport_base", ESP_LOG_ERROR);

  char base[96];
  api_base_url(base, sizeof(base));
  ESP_LOGI(TAG, "WiFi OK, syncing pixel dog with %s", base);

  s_sync_task_handle = xTaskGetCurrentTaskHandle();

  pull_dog_state();

  for (;;) {
    // 等待定时或任务通知唤醒（狗互动时立即唤醒，无需等满15s）
    const int delay_ms = sync_backoff_delay_ms();
    ulTaskNotifyTake(pdTRUE, pdMS_TO_TICKS(delay_ms));

    if (!sync_network_ready()) {
      vTaskDelay(pdMS_TO_TICKS(delay_ms));
      continue;
    }

    bool sync_ok = false;
    bool dirty = __atomic_exchange_n(&g_sync_dirty, false, __ATOMIC_RELAXED);

    // 先 push：将本地增量（如番茄钟奖励）写入后端，避免被后续 pull 覆盖
    if (dirty) {
      sync_ok = push_dog_state();
      if (!sync_ok) {
        __atomic_store_n(&g_sync_dirty, true, __ATOMIC_RELAXED);
        note_sync_failure("Push dog state");
      } else {
        note_sync_success();
      }
    }

    // 再 pull：获取后端的权威数据（包含 PC 侧的互动及刚刚 push 的本地增量）
    bool pull_ok = pull_dog_state();
    if (!pull_ok) {
      note_sync_failure("Pull dog state");
    } else {
      note_sync_success();
    }
  }
}

esp_err_t dog_sync_start() {
#if !CONFIG_PIXEL_DOG_SYNC_ENABLE
  ESP_LOGI(TAG, "Pixel Dog sync disabled");
  return ESP_ERR_NOT_SUPPORTED;
#endif

#if CONFIG_PIXEL_DOG_SYNC_ENABLE
  BaseType_t ok = xTaskCreate(sync_task, "dog_sync", 8192, nullptr, 5, nullptr);
  return ok == pdPASS ? ESP_OK : ESP_FAIL;
#else
  return ESP_ERR_NOT_SUPPORTED;
#endif
}

void dog_sync_mark_dirty(void) {
  __atomic_store_n(&g_sync_dirty, true, __ATOMIC_RELAXED);
  // 唤醒同步任务立即处理，配合 ulTaskNotifyTake 实现即时 push
  if (s_sync_task_handle != nullptr) {
    xTaskNotifyGive(s_sync_task_handle);
  }
}