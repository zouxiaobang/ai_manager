#include "pomodoro_api_client.h"

#include <cstdio>
#include <cstring>

#include "cJSON.h"
#include "esp_http_client.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "freertos/task.h"
#include "panel_config.h"
#include "wifi_manager.h"
#include "modules/pomodoro_module.h"

namespace {
constexpr char TAG[] = "pomodoro_api";
constexpr int kHttpBufferSize = 2048;
constexpr int kPollIntervalOkMs = 2000;
constexpr int kPollIntervalMaxMs = 15000;
constexpr int kHttpPollTimeoutMs = 4000;
constexpr int kHttpSyncTimeoutMs = 8000;

struct PomodoroSyncPayload {
  char phase[20];
  char run_state[16];
  char pending_phase[20];
  int remaining_sec;
  int phase_total_sec;
  int session_work_rounds;
  int64_t plan_id;
  bool take_control;
};

struct PomodoroRecordPayload {
  char record_type[20];
  int duration_sec;
  int64_t plan_id;
  int round_index;
};

QueueHandle_t sync_queue = nullptr;
QueueHandle_t record_queue = nullptr;
bool worker_started = false;
bool record_worker_started = false;
bool poll_started = false;
pomodoro_remote_handler_t remote_handler = nullptr;
int64_t last_applied_admin_ms = 0;
int skip_publish_until_tick = 0;
int64_t suppress_admin_apply_until_ms = 0;

int64_t now_ms() {
  return static_cast<int64_t>(esp_timer_get_time() / 1000);
}

void copy_field(char *dest, size_t dest_len, const char *src) {
  if (dest == nullptr || dest_len == 0) {
    return;
  }
  if (src == nullptr) {
    dest[0] = '\0';
    return;
  }
  std::strncpy(dest, src, dest_len - 1);
  dest[dest_len - 1] = '\0';
}

esp_err_t read_http_response(esp_http_client_handle_t client, char *buffer, int buffer_size, int *out_len) {
  if (buffer == nullptr || buffer_size <= 0 || out_len == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  int total = 0;
  while (true) {
    const int read = esp_http_client_read(client, buffer + total, buffer_size - total - 1);
    if (read < 0) {
      return ESP_FAIL;
    }
    if (read == 0) {
      break;
    }
    total += read;
    if (total >= buffer_size - 1) {
      break;
    }
  }
  buffer[total] = '\0';
  *out_len = total;
  return ESP_OK;
}

bool parse_session_json(const char *json, PomodoroRemoteSession *out) {
  if (json == nullptr || out == nullptr) {
    return false;
  }
  cJSON *root = cJSON_Parse(json);
  if (root == nullptr) {
    return false;
  }

  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    cJSON_Delete(root);
    out->valid = false;
    return true;
  }

  cJSON *phase = cJSON_GetObjectItem(data, "phase");
  cJSON *run_state = cJSON_GetObjectItem(data, "runState");
  cJSON *remaining = cJSON_GetObjectItem(data, "remainingSec");
  cJSON *total = cJSON_GetObjectItem(data, "phaseTotalSec");
  cJSON *rounds = cJSON_GetObjectItem(data, "sessionWorkRounds");
  cJSON *plan_id = cJSON_GetObjectItem(data, "planId");
  cJSON *source = cJSON_GetObjectItem(data, "source");
  cJSON *synced_at = cJSON_GetObjectItem(data, "syncedAtMs");

  if (!cJSON_IsString(phase) || !cJSON_IsString(run_state) || !cJSON_IsNumber(remaining) ||
      !cJSON_IsNumber(total)) {
    cJSON_Delete(root);
    return false;
  }

  copy_field(out->phase, sizeof(out->phase), phase->valuestring);
  copy_field(out->run_state, sizeof(out->run_state), run_state->valuestring);
  out->remaining_sec = remaining->valueint;
  out->phase_total_sec = total->valueint;
  out->session_work_rounds = cJSON_IsNumber(rounds) ? rounds->valueint : 0;
  out->plan_id = cJSON_IsNumber(plan_id) ? static_cast<int64_t>(plan_id->valuedouble) : 0;
  copy_field(out->source, sizeof(out->source), cJSON_IsString(source) ? source->valuestring : "DEVICE");
  cJSON *controller = cJSON_GetObjectItem(data, "controller");
  if (cJSON_IsString(controller)) {
    copy_field(out->controller, sizeof(out->controller), controller->valuestring);
  } else {
    copy_field(out->controller, sizeof(out->controller), out->source);
  }
  out->synced_at_ms = cJSON_IsNumber(synced_at) ? static_cast<int64_t>(synced_at->valuedouble) : 0;
  cJSON *pending = cJSON_GetObjectItem(data, "pendingPhase");
  if (cJSON_IsString(pending)) {
    copy_field(out->pending_phase, sizeof(out->pending_phase), pending->valuestring);
  } else {
    out->pending_phase[0] = '\0';
  }
  out->valid = true;

  cJSON_Delete(root);
  return true;
}

void append_pending_json(char *body, size_t body_len, const char *pending_phase) {
  if (body == nullptr || pending_phase == nullptr || pending_phase[0] == '\0') {
    return;
  }
  const size_t len = std::strlen(body);
  if (len + 32 >= body_len) {
    return;
  }
  char suffix[48] = {};
  std::snprintf(suffix, sizeof(suffix), ",\"pendingPhase\":\"%s\"}", pending_phase);
  if (len > 0 && body[len - 1] == '}') {
    body[len - 1] = '\0';
    std::strncat(body, suffix, body_len - len - 1);
  }
}

esp_err_t http_get_session(PomodoroRemoteSession *out) {
  if (out == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  *out = {};

  char url[160] = {};
  std::snprintf(url, sizeof(url), "http://%s:%d%s", ADMIN_API_HOST, ADMIN_API_PORT, ADMIN_API_SESSION_PATH);

  esp_http_client_config_t config = {};
  config.url = url;
  config.method = HTTP_METHOD_GET;
  config.timeout_ms = kHttpPollTimeoutMs;

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return ESP_FAIL;
  }

  const esp_err_t err = esp_http_client_open(client, 0);
  if (err != ESP_OK) {
    esp_http_client_cleanup(client);
    return err;
  }

  int content_length = esp_http_client_fetch_headers(client);
  if (content_length < 0) {
    content_length = kHttpBufferSize;
  }
  if (content_length > kHttpBufferSize - 1) {
    content_length = kHttpBufferSize - 1;
  }

  char buffer[kHttpBufferSize] = {};
  int read_len = 0;
  const esp_err_t read_err = read_http_response(client, buffer, sizeof(buffer), &read_len);
  const int status = esp_http_client_get_status_code(client);
  esp_http_client_close(client);
  esp_http_client_cleanup(client);

  if (read_err != ESP_OK || status < 200 || status >= 300) {
    ESP_LOGW(TAG, "HTTP GET session failed status=%d", status);
    return ESP_FAIL;
  }

  if (!parse_session_json(buffer, out)) {
    ESP_LOGW(TAG, "Failed to parse session JSON");
    return ESP_FAIL;
  }
  return ESP_OK;
}

bool parse_plan_json(const char *json, PomodoroPlanConfig *out) {
  if (json == nullptr || out == nullptr) {
    return false;
  }
  *out = {};
  cJSON *root = cJSON_Parse(json);
  if (root == nullptr) {
    return false;
  }

  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    cJSON_Delete(root);
    return false;
  }

  cJSON *id = cJSON_GetObjectItem(data, "id");
  cJSON *work = cJSON_GetObjectItem(data, "workDurationMin");
  cJSON *short_break = cJSON_GetObjectItem(data, "shortBreakMin");
  cJSON *long_break = cJSON_GetObjectItem(data, "longBreakMin");
  cJSON *rounds = cJSON_GetObjectItem(data, "roundsBeforeLongBreak");
  cJSON *goal_rounds = cJSON_GetObjectItem(data, "dailyGoalRounds");
  cJSON *goal_minutes = cJSON_GetObjectItem(data, "dailyGoalMinutes");

  if (!cJSON_IsNumber(work) || !cJSON_IsNumber(short_break)) {
    cJSON_Delete(root);
    return false;
  }

  out->plan_id = cJSON_IsNumber(id) ? static_cast<int64_t>(id->valuedouble) : 0;
  out->work_sec = work->valueint * 60;
  out->short_break_sec = short_break->valueint * 60;
  out->long_break_sec = cJSON_IsNumber(long_break) ? long_break->valueint * 60 : out->short_break_sec;
  out->rounds_before_long_break = cJSON_IsNumber(rounds) ? rounds->valueint : 4;
  if (out->work_sec < 60) {
    out->work_sec = 60;
  }
  if (out->short_break_sec < 60) {
    out->short_break_sec = 60;
  }
  if (out->long_break_sec < 60) {
    out->long_break_sec = out->short_break_sec;
  }
  if (out->rounds_before_long_break < 1) {
    out->rounds_before_long_break = 4;
  }
  out->daily_goal_rounds = cJSON_IsNumber(goal_rounds) ? goal_rounds->valueint : 8;
  out->daily_goal_minutes = cJSON_IsNumber(goal_minutes) ? goal_minutes->valueint : 200;
  if (out->daily_goal_rounds < 1) {
    out->daily_goal_rounds = 1;
  }
  if (out->daily_goal_minutes < 1) {
    out->daily_goal_minutes = 1;
  }
  out->valid = true;

  cJSON_Delete(root);
  return true;
}

bool parse_today_json(const char *json, PomodoroTodayStat *out) {
  if (json == nullptr || out == nullptr) {
    return false;
  }
  *out = {};
  cJSON *root = cJSON_Parse(json);
  if (root == nullptr) {
    return false;
  }

  cJSON *data = cJSON_GetObjectItem(root, "data");
  if (data == nullptr || cJSON_IsNull(data)) {
    cJSON_Delete(root);
    return false;
  }

  cJSON *rounds = cJSON_GetObjectItem(data, "workRounds");
  cJSON *work_min = cJSON_GetObjectItem(data, "workMinutes");
  cJSON *break_min = cJSON_GetObjectItem(data, "breakMinutes");
  if (!cJSON_IsNumber(rounds) || !cJSON_IsNumber(work_min)) {
    cJSON_Delete(root);
    return false;
  }

  out->work_rounds = rounds->valueint;
  out->work_minutes = work_min->valueint;
  out->break_minutes = cJSON_IsNumber(break_min) ? break_min->valueint : 0;
  out->valid = true;
  cJSON_Delete(root);
  return true;
}

esp_err_t http_get_today_stat(PomodoroTodayStat *out) {
  if (out == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  *out = {};

  char url[192] = {};
  std::snprintf(
      url,
      sizeof(url),
      "http://%s:%d%s",
      ADMIN_API_HOST,
      ADMIN_API_PORT,
      ADMIN_API_STATS_TODAY_PATH);

  esp_http_client_config_t config = {};
  config.url = url;
  config.method = HTTP_METHOD_GET;
  config.timeout_ms = kHttpPollTimeoutMs;

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return ESP_FAIL;
  }

  const esp_err_t err = esp_http_client_open(client, 0);
  if (err != ESP_OK) {
    esp_http_client_cleanup(client);
    return err;
  }

  int content_length = esp_http_client_fetch_headers(client);
  if (content_length < 0) {
    content_length = kHttpBufferSize;
  }
  if (content_length > kHttpBufferSize - 1) {
    content_length = kHttpBufferSize - 1;
  }

  char buffer[kHttpBufferSize] = {};
  int read_len = 0;
  const esp_err_t read_err = read_http_response(client, buffer, sizeof(buffer), &read_len);
  const int status = esp_http_client_get_status_code(client);
  esp_http_client_close(client);
  esp_http_client_cleanup(client);

  if (read_err != ESP_OK || status < 200 || status >= 300) {
    ESP_LOGW(TAG, "HTTP GET today stat failed status=%d", status);
    return ESP_FAIL;
  }

  if (!parse_today_json(buffer, out)) {
    ESP_LOGW(TAG, "Failed to parse today stat JSON");
    return ESP_FAIL;
  }
  return ESP_OK;
}

esp_err_t http_put_session(const PomodoroSyncPayload *payload, PomodoroRemoteSession *out_ack) {
  if (payload == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  if (!payload->take_control && skip_publish_until_tick > 0) {
    --skip_publish_until_tick;
    return ESP_OK;
  }

  char url[160] = {};
  std::snprintf(url, sizeof(url), "http://%s:%d%s", ADMIN_API_HOST, ADMIN_API_PORT, ADMIN_API_SESSION_PATH);

  char body[400] = {};
  if (payload->plan_id > 0) {
    std::snprintf(
        body,
        sizeof(body),
        "{\"phase\":\"%s\",\"runState\":\"%s\",\"remainingSec\":%d,\"phaseTotalSec\":%d,"
        "\"sessionWorkRounds\":%d,\"planId\":%lld,\"source\":\"DEVICE\",\"takeControl\":%s}",
        payload->phase,
        payload->run_state,
        payload->remaining_sec,
        payload->phase_total_sec,
        payload->session_work_rounds,
        static_cast<long long>(payload->plan_id),
        payload->take_control ? "true" : "false");
  } else {
    std::snprintf(
        body,
        sizeof(body),
        "{\"phase\":\"%s\",\"runState\":\"%s\",\"remainingSec\":%d,\"phaseTotalSec\":%d,"
        "\"sessionWorkRounds\":%d,\"source\":\"DEVICE\",\"takeControl\":%s}",
        payload->phase,
        payload->run_state,
        payload->remaining_sec,
        payload->phase_total_sec,
        payload->session_work_rounds,
        payload->take_control ? "true" : "false");
  }
  append_pending_json(body, sizeof(body), payload->pending_phase);

  esp_http_client_config_t config = {};
  config.url = url;
  config.method = HTTP_METHOD_PUT;
  config.timeout_ms = kHttpSyncTimeoutMs;

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return ESP_FAIL;
  }

  esp_http_client_set_header(client, "Content-Type", "application/json");
  esp_http_client_set_post_field(client, body, static_cast<int>(std::strlen(body)));

  const esp_err_t err = esp_http_client_perform(client);
  const int status = esp_http_client_get_status_code(client);

  char buffer[kHttpBufferSize] = {};
  int read_len = 0;
  if (out_ack != nullptr && err == ESP_OK && status >= 200 && status < 300) {
    (void)read_http_response(client, buffer, sizeof(buffer), &read_len);
  }

  esp_http_client_cleanup(client);

  if (err != ESP_OK || status < 200 || status >= 300) {
    ESP_LOGW(TAG, "HTTP PUT failed status=%d", status);
    return ESP_FAIL;
  }

  if (out_ack != nullptr) {
    if (!parse_session_json(buffer, out_ack)) {
      ESP_LOGW(TAG, "Failed to parse PUT response JSON");
      return ESP_FAIL;
    }
  }

  return ESP_OK;
}

esp_err_t http_post_record(const PomodoroRecordPayload *payload) {
  if (payload == nullptr || payload->duration_sec < 1) {
    return ESP_ERR_INVALID_ARG;
  }

  char url[160] = {};
  std::snprintf(url, sizeof(url), "http://%s:%d%s", ADMIN_API_HOST, ADMIN_API_PORT, ADMIN_API_RECORDS_PATH);

  char body[256] = {};
  if (payload->plan_id > 0 && payload->round_index > 0) {
    std::snprintf(
        body,
        sizeof(body),
        "{\"planId\":%lld,\"recordType\":\"%s\",\"durationSec\":%d,\"roundIndex\":%d,\"source\":\"DEVICE\"}",
        static_cast<long long>(payload->plan_id),
        payload->record_type,
        payload->duration_sec,
        payload->round_index);
  } else if (payload->plan_id > 0) {
    std::snprintf(
        body,
        sizeof(body),
        "{\"planId\":%lld,\"recordType\":\"%s\",\"durationSec\":%d,\"source\":\"DEVICE\"}",
        static_cast<long long>(payload->plan_id),
        payload->record_type,
        payload->duration_sec);
  } else {
    std::snprintf(
        body,
        sizeof(body),
        "{\"recordType\":\"%s\",\"durationSec\":%d,\"source\":\"DEVICE\"}",
        payload->record_type,
        payload->duration_sec);
  }

  esp_http_client_config_t config = {};
  config.url = url;
  config.method = HTTP_METHOD_POST;
  config.timeout_ms = 8000;

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return ESP_FAIL;
  }

  esp_http_client_set_header(client, "Content-Type", "application/json");
  esp_http_client_set_post_field(client, body, static_cast<int>(std::strlen(body)));

  const esp_err_t err = esp_http_client_perform(client);
  const int status = esp_http_client_get_status_code(client);
  esp_http_client_cleanup(client);

  if (err != ESP_OK || status < 200 || status >= 300) {
    ESP_LOGW(TAG, "HTTP POST record failed type=%s status=%d", payload->record_type, status);
    return ESP_FAIL;
  }
  ESP_LOGI(TAG, "Record saved: %s %ds", payload->record_type, payload->duration_sec);
  return ESP_OK;
}

void sync_worker_task(void *arg) {
  (void)arg;
  PomodoroSyncPayload payload;

  while (true) {
    if (xQueueReceive(sync_queue, &payload, portMAX_DELAY) != pdTRUE) {
      continue;
    }
    if (!wifi_is_connected()) {
      continue;
    }
    PomodoroRemoteSession ack = {};
    PomodoroRemoteSession *const ack_ptr = payload.take_control ? &ack : nullptr;
    if (http_put_session(&payload, ack_ptr) == ESP_OK && payload.take_control && ack.valid) {
      last_applied_admin_ms = ack.synced_at_ms;
    }
  }
}

void record_worker_task(void *arg) {
  (void)arg;
  PomodoroRecordPayload payload;
  while (true) {
    if (xQueueReceive(record_queue, &payload, portMAX_DELAY) != pdTRUE) {
      continue;
    }
    if (!wifi_is_connected()) {
      continue;
    }
    const bool is_work = std::strcmp(payload.record_type, "WORK") == 0;
    if (http_post_record(&payload) == ESP_OK && is_work) {
      pomodoro_module::check_today_goal_after_work_record();
    }
  }
}

void poll_worker_task(void *arg) {
  (void)arg;
  int poll_interval_ms = kPollIntervalOkMs;
  int poll_fail_streak = 0;

  while (true) {
    vTaskDelay(pdMS_TO_TICKS(poll_interval_ms));

    if (!wifi_is_connected() || remote_handler == nullptr) {
      continue;
    }

    PomodoroRemoteSession remote = {};
    const esp_err_t get_err = http_get_session(&remote);

    if (get_err != ESP_OK) {
      ++poll_fail_streak;
      poll_interval_ms =
          poll_fail_streak <= 1 ? kPollIntervalOkMs
                                : (kPollIntervalOkMs * poll_fail_streak > kPollIntervalMaxMs
                                       ? kPollIntervalMaxMs
                                       : kPollIntervalOkMs * poll_fail_streak);
      ESP_LOGW(TAG, "poll failed streak=%d next=%dms", poll_fail_streak, poll_interval_ms);
      continue;
    }

    if (!remote.valid) {
      if (poll_fail_streak == 0) {
        last_applied_admin_ms = 0;
      }
      ++poll_fail_streak;
      poll_interval_ms = kPollIntervalOkMs * 2;
      continue;
    }

    poll_fail_streak = 0;
    poll_interval_ms = kPollIntervalOkMs;

    if (std::strcmp(remote.controller, "ADMIN") != 0) {
      continue;
    }
    if (now_ms() < suppress_admin_apply_until_ms) {
      continue;
    }
    if (pomodoro_module::is_pending_phase()) {
      continue;
    }
    if (remote.synced_at_ms <= last_applied_admin_ms) {
      continue;
    }
    if (pomodoro_module::remote_session_matches_local(&remote)) {
      last_applied_admin_ms = remote.synced_at_ms;
      continue;
    }

    skip_publish_until_tick = 3;
    remote_handler(&remote);
    last_applied_admin_ms = remote.synced_at_ms;
  }
}

void set_last_applied_synced_ms_impl(int64_t synced_at_ms) {
  last_applied_admin_ms = synced_at_ms;
}

}  // namespace

esp_err_t pomodoro_api_fetch_active_session(PomodoroRemoteSession *out) {
  return http_get_session(out);
}

esp_err_t pomodoro_api_fetch_today_stat(PomodoroTodayStat *out) {
  return http_get_today_stat(out);
}

void pomodoro_api_set_last_applied_synced_ms(int64_t synced_at_ms) {
  set_last_applied_synced_ms_impl(synced_at_ms);
}

esp_err_t pomodoro_api_fetch_default_plan(PomodoroPlanConfig *out) {
  if (out == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  *out = {};

  char url[192] = {};
  std::snprintf(
      url,
      sizeof(url),
      "http://%s:%d%s",
      ADMIN_API_HOST,
      ADMIN_API_PORT,
      ADMIN_API_PLAN_DEFAULT_PATH);

  esp_http_client_config_t config = {};
  config.url = url;
  config.method = HTTP_METHOD_GET;
  config.timeout_ms = 8000;

  esp_http_client_handle_t client = esp_http_client_init(&config);
  if (client == nullptr) {
    return ESP_FAIL;
  }

  const esp_err_t err = esp_http_client_open(client, 0);
  if (err != ESP_OK) {
    esp_http_client_cleanup(client);
    return err;
  }

  int content_length = esp_http_client_fetch_headers(client);
  if (content_length < 0) {
    content_length = kHttpBufferSize;
  }
  if (content_length > kHttpBufferSize - 1) {
    content_length = kHttpBufferSize - 1;
  }

  char buffer[kHttpBufferSize] = {};
  int read_len = 0;
  const esp_err_t read_err = read_http_response(client, buffer, sizeof(buffer), &read_len);
  const int status = esp_http_client_get_status_code(client);
  esp_http_client_close(client);
  esp_http_client_cleanup(client);

  if (read_err != ESP_OK || status < 200 || status >= 300) {
    ESP_LOGW(TAG, "HTTP GET default plan failed status=%d", status);
    return ESP_FAIL;
  }

  if (!parse_plan_json(buffer, out)) {
    ESP_LOGW(TAG, "Failed to parse default plan JSON");
    return ESP_FAIL;
  }

  ESP_LOGI(
      TAG,
      "Plan id=%lld work=%ds short=%ds long=%ds",
      static_cast<long long>(out->plan_id),
      out->work_sec,
      out->short_break_sec,
      out->long_break_sec);
  return ESP_OK;
}

esp_err_t pomodoro_api_client_init() {
  if (sync_queue == nullptr) {
    sync_queue = xQueueCreate(1, sizeof(PomodoroSyncPayload));
    if (sync_queue == nullptr) {
      return ESP_ERR_NO_MEM;
    }
  }
  if (!worker_started) {
    if (xTaskCreate(sync_worker_task, "pomodoro_put", 6144, nullptr, 4, nullptr) != pdPASS) {
      return ESP_ERR_NO_MEM;
    }
    worker_started = true;
  }
  if (!poll_started) {
    if (xTaskCreate(poll_worker_task, "pomodoro_poll", 8192, nullptr, 4, nullptr) != pdPASS) {
      return ESP_ERR_NO_MEM;
    }
    poll_started = true;
  }
  if (record_queue == nullptr) {
    record_queue = xQueueCreate(4, sizeof(PomodoroRecordPayload));
    if (record_queue == nullptr) {
      return ESP_ERR_NO_MEM;
    }
  }
  if (!record_worker_started) {
    if (xTaskCreate(record_worker_task, "pomodoro_rec", 6144, nullptr, 4, nullptr) != pdPASS) {
      return ESP_ERR_NO_MEM;
    }
    record_worker_started = true;
  }
  return ESP_OK;
}

void pomodoro_api_set_remote_handler(pomodoro_remote_handler_t handler) {
  remote_handler = handler;
}

void pomodoro_api_mark_local_control(void) {
  suppress_admin_apply_until_ms = now_ms() + 5000;
}

void pomodoro_api_sync_session(
    const char *phase,
    const char *run_state,
    int remaining_sec,
    int phase_total_sec,
    int session_work_rounds,
    int64_t plan_id,
    bool take_control,
    const char *pending_phase) {
  if (sync_queue == nullptr) {
    if (pomodoro_api_client_init() != ESP_OK) {
      return;
    }
  }

  PomodoroSyncPayload payload = {};
  copy_field(payload.phase, sizeof(payload.phase), phase);
  copy_field(payload.run_state, sizeof(payload.run_state), run_state);
  copy_field(payload.pending_phase, sizeof(payload.pending_phase), pending_phase);
  payload.remaining_sec = remaining_sec < 0 ? 0 : remaining_sec;
  payload.phase_total_sec = phase_total_sec < 1 ? 1 : phase_total_sec;
  payload.session_work_rounds = session_work_rounds < 0 ? 0 : session_work_rounds;
  payload.plan_id = plan_id;
  payload.take_control = take_control;

  if (take_control) {
    if (!wifi_is_connected()) {
      return;
    }
    suppress_admin_apply_until_ms = now_ms() + 5000;
  }

  (void)xQueueOverwrite(sync_queue, &payload);
}

void pomodoro_api_create_record(
    const char *record_type,
    int duration_sec,
    int64_t plan_id,
    int round_index) {
  if (record_queue == nullptr) {
    if (pomodoro_api_client_init() != ESP_OK) {
      return;
    }
  }
  if (record_type == nullptr || duration_sec < 1) {
    return;
  }

  PomodoroRecordPayload payload = {};
  copy_field(payload.record_type, sizeof(payload.record_type), record_type);
  payload.duration_sec = duration_sec;
  payload.plan_id = plan_id;
  payload.round_index = round_index;
  (void)xQueueSend(record_queue, &payload, 0);
}
