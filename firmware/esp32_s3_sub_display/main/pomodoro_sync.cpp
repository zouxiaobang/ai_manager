#include "pomodoro_sync.h"

#include <algorithm>
#include <cstdio>
#include <cstring>

#include "cJSON.h"
#include "esp_http_client.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "pomodoro_model.h"
#include "pomodoro_plan_cache.h"
#include "pomodoro_api_config.h"
#include "wifi_sta.h"

namespace {
constexpr char TAG[] = "pomo_sync";
constexpr int kHttpBufSize = 4096;
constexpr int kPollIntervalMs = 1000;
constexpr int kPlanRefreshPolls = 60;

char s_http_buf[kHttpBufSize];

struct HttpResponse {
  int status = 0;
  int len = 0;
};

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
  std::snprintf(out, out_len, "http://%s:%d", pomodoro_api_get_host(), pomodoro_api_get_port());
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
  config.timeout_ms = 8000;
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
  } else {
    ESP_LOGE(TAG, "HTTP %s %s failed: %s", method, url, esp_err_to_name(err));
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

PomodoroPhase parse_phase(const char *s) {
  if (s == nullptr) {
    return PomodoroPhase::Idle;
  }
  if (std::strcmp(s, "WORK") == 0) {
    return PomodoroPhase::Focus;
  }
  if (std::strcmp(s, "SHORT_BREAK") == 0) {
    return PomodoroPhase::ShortBreak;
  }
  if (std::strcmp(s, "LONG_BREAK") == 0) {
    return PomodoroPhase::LongBreak;
  }
  return PomodoroPhase::Idle;
}

PomodoroPendingPhase parse_pending(const char *s) {
  if (s == nullptr) {
    return PomodoroPendingPhase::None;
  }
  if (std::strcmp(s, "WORK") == 0) {
    return PomodoroPendingPhase::Work;
  }
  if (std::strcmp(s, "SHORT_BREAK") == 0) {
    return PomodoroPendingPhase::ShortBreak;
  }
  if (std::strcmp(s, "LONG_BREAK") == 0) {
    return PomodoroPendingPhase::LongBreak;
  }
  return PomodoroPendingPhase::None;
}

bool parse_session_json(cJSON *data, PomodoroRemoteSession *out) {
  if (data == nullptr || cJSON_IsNull(data) || out == nullptr) {
    return false;
  }

  cJSON *phase = cJSON_GetObjectItem(data, "phase");
  cJSON *run_state = cJSON_GetObjectItem(data, "runState");
  cJSON *remaining = cJSON_GetObjectItem(data, "remainingSec");
  cJSON *total = cJSON_GetObjectItem(data, "phaseTotalSec");
  cJSON *rounds = cJSON_GetObjectItem(data, "sessionWorkRounds");
  cJSON *plan_id = cJSON_GetObjectItem(data, "planId");
  cJSON *pending = cJSON_GetObjectItem(data, "pendingPhase");
  cJSON *synced = cJSON_GetObjectItem(data, "syncedAtMs");
  cJSON *controller = cJSON_GetObjectItem(data, "controller");
  cJSON *source = cJSON_GetObjectItem(data, "source");

  if (!cJSON_IsString(phase) || !cJSON_IsString(run_state)) {
    return false;
  }

  out->phase = parse_phase(phase->valuestring);
  const char *rs = run_state->valuestring;
  out->run_state_idle = std::strcmp(rs, "IDLE") == 0;
  out->running = std::strcmp(rs, "RUNNING") == 0;
  out->remaining_sec = cJSON_IsNumber(remaining) ? remaining->valueint : 0;
  out->phase_total_sec = cJSON_IsNumber(total) ? total->valueint : 25 * 60;
  out->session_work_rounds = cJSON_IsNumber(rounds) ? rounds->valueint : 0;
  out->plan_id = cJSON_IsNumber(plan_id) ? static_cast<int64_t>(plan_id->valuedouble) : 0;
  out->pending = parse_pending(cJSON_IsString(pending) ? pending->valuestring : nullptr);
  out->synced_at_ms = cJSON_IsNumber(synced) ? static_cast<int64_t>(synced->valuedouble) : 0;

  const char *ctrl = cJSON_IsString(controller) ? controller->valuestring : nullptr;
  if (ctrl == nullptr && cJSON_IsString(source)) {
    ctrl = source->valuestring;
  }
  out->controller_is_device = ctrl != nullptr && std::strcmp(ctrl, "DEVICE") == 0;
  out->valid = true;
  return true;
}

bool parse_plan_json(cJSON *data, PomodoroPlanConfig *out) {
  if (data == nullptr || cJSON_IsNull(data) || out == nullptr) {
    return false;
  }
  cJSON *id = cJSON_GetObjectItem(data, "id");
  cJSON *work = cJSON_GetObjectItem(data, "workDurationMin");
  cJSON *short_min = cJSON_GetObjectItem(data, "shortBreakMin");
  cJSON *long_b = cJSON_GetObjectItem(data, "longBreakMin");
  cJSON *rounds = cJSON_GetObjectItem(data, "roundsBeforeLongBreak");
  cJSON *daily_goal = cJSON_GetObjectItem(data, "dailyGoalRounds");

  if (!cJSON_IsNumber(id) || !cJSON_IsNumber(work)) {
    return false;
  }

  out->plan_id = static_cast<int64_t>(id->valuedouble);
  out->work_duration_min = work->valueint;
  out->short_break_min = cJSON_IsNumber(short_min) ? short_min->valueint : 5;
  out->long_break_min = cJSON_IsNumber(long_b) ? long_b->valueint : 15;
  out->rounds_before_long_break = cJSON_IsNumber(rounds) ? rounds->valueint : 4;
  out->daily_goal_rounds = cJSON_IsNumber(daily_goal) ? daily_goal->valueint : 0;
  return true;
}

bool fetch_default_plan(PomodoroPlanConfig *plan) {
  HttpResponse resp;
  if (http_request("GET", "/api/pomodoro/plans/default", nullptr, &resp) != ESP_OK) {
    return false;
  }
  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }
  cJSON *data = cJSON_GetObjectItem(root, "data");
  const bool ok = parse_plan_json(data, plan);
  cJSON_Delete(root);
  return ok;
}

bool fetch_today_stats(int *work_rounds_out) {
  if (work_rounds_out == nullptr) {
    return false;
  }
  HttpResponse resp;
  if (http_request("GET", "/api/pomodoro/stats/today", nullptr, &resp) != ESP_OK) {
    return false;
  }
  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }
  cJSON *data = cJSON_GetObjectItem(root, "data");
  cJSON *rounds = data != nullptr ? cJSON_GetObjectItem(data, "workRounds") : nullptr;
  if (!cJSON_IsNumber(rounds)) {
    cJSON_Delete(root);
    return false;
  }
  *work_rounds_out = rounds->valueint;
  cJSON_Delete(root);
  return true;
}

bool post_work_record(int64_t plan_id, int duration_sec) {
  if (plan_id <= 0 || duration_sec < 1) {
    return false;
  }
  char body[160];
  std::snprintf(body, sizeof(body),
                "{\"planId\":%lld,\"recordType\":\"WORK\",\"durationSec\":%d,\"source\":\"DEVICE\"}",
                static_cast<long long>(plan_id), duration_sec);

  HttpResponse resp;
  if (http_request("POST", "/api/pomodoro/records", body, &resp) != ESP_OK) {
    return false;
  }
  cJSON *root = nullptr;
  const bool ok = api_ok(resp, &root);
  if (root != nullptr) {
    cJSON_Delete(root);
  }
  return ok;
}

void refresh_today_stats_from_server() {
  int rounds = 0;
  if (fetch_today_stats(&rounds)) {
    pomodoro_set_today_work_rounds(rounds);
  }
}

void flush_pending_work_record() {
  int duration = 0;
  if (!pomodoro_consume_work_record_request(&duration)) {
    return;
  }
  const PomodoroPlanConfig plan = pomodoro_get_plan();
  if (post_work_record(plan.plan_id, duration)) {
    refresh_today_stats_from_server();
  }
}

bool push_session(bool take_control) {
  PomodoroSyncPayload payload;
  if (!pomodoro_build_sync_payload(&payload)) {
    return false;
  }
  payload.take_control = take_control;

  char body[512];
  if (payload.pending_phase != nullptr) {
    std::snprintf(body, sizeof(body),
                  "{\"phase\":\"%s\",\"runState\":\"%s\",\"remainingSec\":%d,"
                  "\"phaseTotalSec\":%d,\"sessionWorkRounds\":%d,\"planId\":%lld,"
                  "\"source\":\"DEVICE\",\"takeControl\":%s,\"pendingPhase\":\"%s\"}",
                  payload.phase, payload.run_state, payload.remaining_sec, payload.phase_total_sec,
                  payload.session_work_rounds, static_cast<long long>(payload.plan_id),
                  take_control ? "true" : "false", payload.pending_phase);
  } else {
    std::snprintf(body, sizeof(body),
                  "{\"phase\":\"%s\",\"runState\":\"%s\",\"remainingSec\":%d,"
                  "\"phaseTotalSec\":%d,\"sessionWorkRounds\":%d,\"planId\":%lld,"
                  "\"source\":\"DEVICE\",\"takeControl\":%s}",
                  payload.phase, payload.run_state, payload.remaining_sec, payload.phase_total_sec,
                  payload.session_work_rounds, static_cast<long long>(payload.plan_id),
                  take_control ? "true" : "false");
  }

  HttpResponse resp;
  if (http_request("PUT", "/api/pomodoro/session", body, &resp) != ESP_OK) {
    return false;
  }

  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }
  PomodoroRemoteSession remote;
  if (parse_session_json(cJSON_GetObjectItem(root, "data"), &remote)) {
    pomodoro_apply_remote_session(remote, true);
  }
  cJSON_Delete(root);
  return true;
}

bool pull_session(bool *has_session, bool apply_remote) {
  HttpResponse resp;
  if (http_request("GET", "/api/pomodoro/session", nullptr, &resp) != ESP_OK) {
    return false;
  }
  cJSON *root = nullptr;
  if (!api_ok(resp, &root)) {
    return false;
  }
  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    *has_session = false;
    cJSON_Delete(root);
    return true;
  }
  PomodoroRemoteSession remote;
  if (parse_session_json(data, &remote)) {
    *has_session = true;
    if (apply_remote) {
      if (!remote.controller_is_device ||
          remote.synced_at_ms > pomodoro_last_applied_sync_ms()) {
        pomodoro_apply_remote_session(remote, true);
      }
    }
  }
  cJSON_Delete(root);
  return true;
}

void sync_task(void *arg) {
  (void)arg;

  pomodoro_api_config_load();

  PomodoroPlanConfig cached;
  if (pomodoro_plan_cache_load(&cached) == ESP_OK) {
    pomodoro_apply_plan(cached);
  }

  if (wifi_sta_connect() != ESP_OK) {
    ESP_LOGW(TAG, "WiFi unavailable, offline mode with cached plan");
    pomodoro_set_backend_connected(false);
    vTaskDelete(nullptr);
    return;
  }

  wifi_sta_init_mdns();

  char base[96];
  api_base_url(base, sizeof(base));
  ESP_LOGI(TAG, "WiFi OK, syncing with %s", base);

  PomodoroPlanConfig remote_plan;
  if (fetch_default_plan(&remote_plan)) {
    pomodoro_plan_cache_save(remote_plan);
    pomodoro_apply_plan(remote_plan);
    ESP_LOGI(TAG, "Default plan synced, id=%lld", static_cast<long long>(remote_plan.plan_id));
  } else {
    ESP_LOGW(TAG, "Failed to fetch default plan from backend");
  }

  refresh_today_stats_from_server();

  bool has_session = false;
  if (pull_session(&has_session, true)) {
    pomodoro_set_backend_connected(true);
    if (!has_session) {
      push_session(false);
    }
  } else {
    pomodoro_set_backend_connected(false);
  }

  int poll_count = 0;
  int heartbeat_polls = 0;
  for (;;) {
    flush_pending_work_record();

    bool take_control = false;
    const bool dirty = pomodoro_consume_sync_dirty(&take_control);
    if (dirty) {
      if (push_session(take_control)) {
        pomodoro_set_backend_connected(true);
      }
      heartbeat_polls = 0;
    } else {
      heartbeat_polls++;
      if (heartbeat_polls >= 15) {
        heartbeat_polls = 0;
        push_session(false);
      } else if (pull_session(&has_session, true)) {
        pomodoro_set_backend_connected(true);
      } else {
        pomodoro_set_backend_connected(wifi_sta_is_connected());
      }
    }

    poll_count++;
    if (poll_count >= kPlanRefreshPolls) {
      poll_count = 0;
      if (fetch_default_plan(&remote_plan)) {
        pomodoro_plan_cache_save(remote_plan);
        pomodoro_apply_plan(remote_plan);
      }
      refresh_today_stats_from_server();
    } else if (poll_count % 15 == 0) {
      refresh_today_stats_from_server();
    }

    vTaskDelay(pdMS_TO_TICKS(kPollIntervalMs));
  }
}
}  // namespace

esp_err_t pomodoro_sync_start() {
#if !CONFIG_POMO_SYNC_ENABLE
  ESP_LOGI(TAG, "Pomodoro sync disabled");
  return ESP_ERR_NOT_SUPPORTED;
#endif

#if CONFIG_POMO_SYNC_ENABLE
  BaseType_t ok = xTaskCreate(sync_task, "pomo_sync", 8192, nullptr, 5, nullptr);
  return ok == pdPASS ? ESP_OK : ESP_FAIL;
#else
  return ESP_ERR_NOT_SUPPORTED;
#endif
}
