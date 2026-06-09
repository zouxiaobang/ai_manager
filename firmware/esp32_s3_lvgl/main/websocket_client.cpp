#include "websocket_client.h"

#include <cstdio>
#include <cstring>
#include <string>

#include "cJSON.h"
#include "esp_check.h"
#include "esp_heap_caps.h"
#include "esp_log.h"
#include "esp_mac.h"
#include "esp_timer.h"
#include "esp_websocket_client.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include "freertos/task.h"
#include "panel_config.h"
#include "ui.h"
#include "wifi_manager.h"

esp_err_t websocket_start_playback_session();

namespace {
constexpr char TAG[] = "websocket";

enum class PlaybackMode {
  Idle,
  Playing,
  Paused,
};

esp_websocket_client_handle_t ws_client = nullptr;
PlaybackMode playback_mode = PlaybackMode::Idle;
bool websocket_connected = false;
bool ws_client_task_alive = false;
std::string session_id;
std::string websocket_uri;
std::string pending_command;
std::string last_control_command;
uint64_t last_control_command_ms = 0;
constexpr uint32_t kControlDebounceMs = 2000;
char *ws_text_buffer = nullptr;
size_t ws_text_capacity = 0;
size_t ws_text_length = 0;
SemaphoreHandle_t send_mutex = nullptr;
QueueHandle_t incoming_message_queue = nullptr;
QueueHandle_t latest_lyrics_queue = nullptr;
QueueHandle_t latest_state_queue = nullptr;
QueueHandle_t latest_cover_queue = nullptr;

struct WsQueuedMessage {
  uint32_t length;
  char *data;
};

enum class WsMessageKind {
  General,
  Lyrics,
  PlaybackState,
  Cover,
};

constexpr int kIncomingMessageQueueDepth = 8;
constexpr int kLatestMessageQueueDepth = 1;
constexpr int kWsWorkerStackSize = 8192;
constexpr int kCoverWorkerStackSize = 24576;

uint64_t now_ms() {
  return static_cast<uint64_t>(esp_timer_get_time() / 1000ULL);
}

std::string make_session_id() {
  uint8_t mac[6] = {};
  esp_efuse_mac_get_default(mac);

  char buffer[64] = {};
  std::snprintf(
    buffer,
    sizeof(buffer),
    "esp32-s3-%02x%02x%02x%02x%02x%02x-%llu",
    mac[0],
    mac[1],
    mac[2],
    mac[3],
    mac[4],
    mac[5],
    static_cast<unsigned long long>(now_ms())
  );
  return buffer;
}

std::string make_message(const char *type, const char *request_id, const char *payload_json) {
  cJSON *root = cJSON_CreateObject();
  cJSON_AddStringToObject(root, "type", type);
  if (request_id != nullptr && request_id[0] != '\0') {
    cJSON_AddStringToObject(root, "request_id", request_id);
  }
  cJSON_AddStringToObject(root, "session_id", session_id.c_str());
  cJSON_AddNumberToObject(root, "timestamp_ms", static_cast<double>(now_ms()));

  cJSON *payload = cJSON_Parse(payload_json != nullptr ? payload_json : "{}");
  if (payload == nullptr) {
    payload = cJSON_CreateObject();
  }
  cJSON_AddItemToObject(root, "payload", payload);

  char *rendered = cJSON_PrintUnformatted(root);
  std::string message = rendered != nullptr ? rendered : "{}";
  cJSON_free(rendered);
  cJSON_Delete(root);
  return message;
}

void handle_text_message(const char *data, int length);

char *allocate_message_copy(const char *data, int length) {
  const size_t size = static_cast<size_t>(length) + 1U;
  char *copy = static_cast<char *>(heap_caps_malloc(size, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
  if (copy == nullptr) {
    copy = static_cast<char *>(heap_caps_malloc(size, MALLOC_CAP_DEFAULT));
  }
  if (copy == nullptr) {
    return nullptr;
  }
  std::memcpy(copy, data, static_cast<size_t>(length));
  copy[length] = '\0';
  return copy;
}

const char *find_json_string_value(const char *json, const char *field) {
  if (json == nullptr || field == nullptr) {
    return nullptr;
  }

  char pattern[48] = {};
  std::snprintf(pattern, sizeof(pattern), "\"%s\":\"", field);
  const char *start = std::strstr(json, pattern);
  if (start == nullptr) {
    std::snprintf(pattern, sizeof(pattern), "\"%s\": \"", field);
    start = std::strstr(json, pattern);
  }
  if (start == nullptr) {
    return nullptr;
  }
  return start + std::strlen(pattern);
}

bool parse_json_int_field(const char *json, const char *field, int &value) {
  if (json == nullptr || field == nullptr) {
    return false;
  }

  char pattern[32] = {};
  std::snprintf(pattern, sizeof(pattern), "\"%s\":", field);
  const char *start = std::strstr(json, pattern);
  if (start == nullptr) {
    return false;
  }

  start += std::strlen(pattern);
  while (*start == ' ') {
    ++start;
  }

  char *end = nullptr;
  const long parsed = std::strtol(start, &end, 10);
  if (end == start) {
    return false;
  }

  value = static_cast<int>(parsed);
  return true;
}

void handle_cover_message(const char *data, int length) {
  (void)length;

  if (std::strstr(data, "\"format\":\"rgb565\"") == nullptr && std::strstr(data, "\"format\": \"rgb565\"") == nullptr) {
    return;
  }

  const char *track_key_start = find_json_string_value(data, "track_key");
  const char *data_b64_start = find_json_string_value(data, "data_b64");
  if (track_key_start == nullptr || data_b64_start == nullptr) {
    ESP_LOGW(TAG, "Cover message missing track_key or data_b64");
    return;
  }

  const char *track_key_end = std::strchr(track_key_start, '"');
  const char *data_b64_end = std::strchr(data_b64_start, '"');
  if (track_key_end == nullptr || data_b64_end == nullptr || data_b64_end <= data_b64_start) {
    ESP_LOGW(TAG, "Cover message has invalid string fields");
    return;
  }

  int width = 0;
  int height = 0;
  if (!parse_json_int_field(data, "width", width) || !parse_json_int_field(data, "height", height)) {
    ESP_LOGW(TAG, "Cover message missing width/height");
    return;
  }

  std::string track_key(track_key_start, track_key_end - track_key_start);
  const size_t b64_len = static_cast<size_t>(data_b64_end - data_b64_start);
  char *data_b64 = static_cast<char *>(heap_caps_malloc(b64_len + 1U, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
  if (data_b64 == nullptr) {
    data_b64 = static_cast<char *>(heap_caps_malloc(b64_len + 1U, MALLOC_CAP_DEFAULT));
  }
  if (data_b64 == nullptr) {
    ESP_LOGW(TAG, "Failed to allocate cover base64 buffer (%u bytes)", static_cast<unsigned>(b64_len));
    return;
  }
  std::memcpy(data_b64, data_b64_start, b64_len);
  data_b64[b64_len] = '\0';

  ui_set_cover(track_key.c_str(), data_b64, width, height);
  heap_caps_free(data_b64);
}

WsMessageKind classify_message(const char *data) {
  if (data == nullptr) {
    return WsMessageKind::General;
  }
  if (std::strstr(data, "\"type\":\"lyrics.line\"") != nullptr || std::strstr(data, "\"type\": \"lyrics.line\"") != nullptr) {
    return WsMessageKind::Lyrics;
  }
  if (std::strstr(data, "\"type\":\"playback.state\"") != nullptr || std::strstr(data, "\"type\": \"playback.state\"") != nullptr) {
    return WsMessageKind::PlaybackState;
  }
  if (std::strstr(data, "\"type\":\"playback.cover\"") != nullptr || std::strstr(data, "\"type\": \"playback.cover\"") != nullptr) {
    return WsMessageKind::Cover;
  }
  return WsMessageKind::General;
}

void free_queued_message(WsQueuedMessage &message) {
  if (message.data != nullptr) {
    heap_caps_free(message.data);
    message.data = nullptr;
  }
  message.length = 0;
}

bool enqueue_to_latest_slot(QueueHandle_t queue, WsQueuedMessage message) {
  if (queue == nullptr) {
    free_queued_message(message);
    return false;
  }

  WsQueuedMessage previous = {};
  if (xQueueReceive(queue, &previous, 0) == pdTRUE) {
    free_queued_message(previous);
  }

  if (xQueueSend(queue, &message, 0) != pdTRUE) {
    free_queued_message(message);
    return false;
  }
  return true;
}

bool enqueue_to_main_with_eviction(WsQueuedMessage message) {
  if (incoming_message_queue == nullptr) {
    free_queued_message(message);
    return false;
  }

  if (xQueueSend(incoming_message_queue, &message, 0) == pdTRUE) {
    return true;
  }

  WsQueuedMessage stale = {};
  if (xQueueReceive(incoming_message_queue, &stale, 0) == pdTRUE) {
    free_queued_message(stale);
  }

  if (xQueueSend(incoming_message_queue, &message, 0) == pdTRUE) {
    ESP_LOGW(TAG, "WS general queue full, dropped oldest message");
    return true;
  }

  const uint32_t dropped_length = message.length;
  free_queued_message(message);
  ESP_LOGW(TAG, "WS general queue full, dropping message (%u bytes)", static_cast<unsigned>(dropped_length));
  return false;
}

bool enqueue_incoming_message(const char *data, int length) {
  if (data == nullptr || length <= 0) {
    return false;
  }

  char *copy = allocate_message_copy(data, length);
  if (copy == nullptr) {
    ESP_LOGW(TAG, "Failed to allocate WS message buffer (%d bytes)", length);
    return false;
  }

  WsQueuedMessage message = {
    .length = static_cast<uint32_t>(length),
    .data = copy,
  };

  switch (classify_message(copy)) {
    case WsMessageKind::Lyrics:
      return enqueue_to_latest_slot(latest_lyrics_queue, message);
    case WsMessageKind::PlaybackState:
      return enqueue_to_latest_slot(latest_state_queue, message);
    case WsMessageKind::Cover:
      return enqueue_to_latest_slot(latest_cover_queue, message);
    case WsMessageKind::General:
    default:
      return enqueue_to_main_with_eviction(message);
  }
}

bool enqueue_owned_message(char *data, int length) {
  if (data == nullptr || length <= 0) {
    if (data != nullptr) {
      heap_caps_free(data);
    }
    return false;
  }

  WsQueuedMessage message = {
    .length = static_cast<uint32_t>(length),
    .data = data,
  };

  switch (classify_message(data)) {
    case WsMessageKind::Lyrics:
      return enqueue_to_latest_slot(latest_lyrics_queue, message);
    case WsMessageKind::PlaybackState:
      return enqueue_to_latest_slot(latest_state_queue, message);
    case WsMessageKind::Cover:
      return enqueue_to_latest_slot(latest_cover_queue, message);
    case WsMessageKind::General:
    default:
      return enqueue_to_main_with_eviction(message);
  }
}

bool try_drain_queue(QueueHandle_t queue) {
  if (queue == nullptr) {
    return false;
  }

  WsQueuedMessage message = {};
  if (xQueueReceive(queue, &message, 0) != pdTRUE) {
    return false;
  }

  if (message.data != nullptr) {
    handle_text_message(message.data, static_cast<int>(message.length));
    free_queued_message(message);
  }
  return true;
}

void cover_worker_task(void *arg) {
  (void)arg;

  WsQueuedMessage message = {};
  while (true) {
    if (latest_cover_queue == nullptr) {
      vTaskDelay(pdMS_TO_TICKS(20));
      continue;
    }

    if (xQueueReceive(latest_cover_queue, &message, pdMS_TO_TICKS(50)) != pdTRUE) {
      continue;
    }

    if (message.data != nullptr) {
      handle_cover_message(message.data, static_cast<int>(message.length));
      free_queued_message(message);
    }
  }
}

void ws_worker_task(void *arg) {
  (void)arg;

  WsQueuedMessage message = {};
  while (true) {
    if (try_drain_queue(latest_state_queue)) {
      continue;
    }
    if (try_drain_queue(latest_lyrics_queue)) {
      continue;
    }

    if (incoming_message_queue == nullptr) {
      vTaskDelay(pdMS_TO_TICKS(5));
      continue;
    }

    if (xQueueReceive(incoming_message_queue, &message, pdMS_TO_TICKS(5)) != pdTRUE) {
      continue;
    }

    if (message.data != nullptr) {
      handle_text_message(message.data, static_cast<int>(message.length));
      free_queued_message(message);
    }
  }
}

void send_json(const char *type, const char *request_id, const char *payload_json) {
  if (ws_client == nullptr) {
    ESP_LOGW(TAG, "Drop message because WebSocket client is null: %s", type);
    return;
  }

  bool connected = websocket_connected && esp_websocket_client_is_connected(ws_client);
  if (!connected) {
    ESP_LOGW(TAG, "Drop message because WebSocket is not connected: %s flag=%d", type, websocket_connected);
    return;
  }

  if (send_mutex != nullptr) {
    xSemaphoreTake(send_mutex, portMAX_DELAY);
  }

  std::string message = make_message(type, request_id, payload_json);
  ESP_LOGI(TAG, "Send message: %s request=%s payload=%s len=%u", type, request_id != nullptr ? request_id : "", payload_json, static_cast<unsigned>(message.size()));
  int sent = esp_websocket_client_send_text(ws_client, message.c_str(), static_cast<int>(message.size()), pdMS_TO_TICKS(3000));
  if (sent < 0) {
    ESP_LOGW(TAG, "Failed to send message: %s ret=%d", type, sent);
  } else {
    ESP_LOGI(TAG, "Sent message: %s bytes=%d", type, sent);
  }

  if (send_mutex != nullptr) {
    xSemaphoreGive(send_mutex);
  }
}

void send_control_command_now(const char *command) {
  char payload[64] = {};
  char request_id[32] = {};
  std::snprintf(payload, sizeof(payload), "{\"command\":\"%s\"}", command);
  std::snprintf(request_id, sizeof(request_id), "cmd-%llu", static_cast<unsigned long long>(now_ms()));
  send_json("control.command", request_id, payload);
}

void send_seek_command_now(int position_ms) {
  char payload[96] = {};
  char request_id[32] = {};
  if (position_ms < 0) {
    position_ms = 0;
  }
  std::snprintf(payload, sizeof(payload), "{\"command\":\"seek\",\"position_ms\":%d}", position_ms);
  std::snprintf(request_id, sizeof(request_id), "seek-%llu", static_cast<unsigned long long>(now_ms()));
  send_json("control.command", request_id, payload);
}

void reset_ws_text_assembly(size_t expected_len) {
  const size_t needed = expected_len > 0 ? expected_len + 1U : 256U;
  if (ws_text_buffer == nullptr || ws_text_capacity < needed) {
    if (ws_text_buffer != nullptr) {
      heap_caps_free(ws_text_buffer);
      ws_text_buffer = nullptr;
      ws_text_capacity = 0;
    }

    ws_text_buffer = static_cast<char *>(heap_caps_malloc(needed, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
    if (ws_text_buffer == nullptr) {
      ws_text_buffer = static_cast<char *>(heap_caps_malloc(needed, MALLOC_CAP_DEFAULT));
    }
    if (ws_text_buffer == nullptr) {
      ESP_LOGE(TAG, "Failed to allocate WS assembly buffer (%u bytes)", static_cast<unsigned>(needed));
      ws_text_length = 0;
      ws_text_capacity = 0;
      return;
    }
    ws_text_capacity = needed;
  }
  ws_text_length = 0;
}

void clear_ws_text_assembly() {
  ws_text_length = 0;
}

void handle_websocket_text_fragment(const char *data, int length, const esp_websocket_event_data_t *event) {
  if (data == nullptr || length <= 0 || event == nullptr) {
    return;
  }

  if (event->payload_offset == 0) {
    reset_ws_text_assembly(event->payload_len > 0 ? static_cast<size_t>(event->payload_len) : static_cast<size_t>(length));
  }

  if (ws_text_buffer == nullptr || ws_text_capacity == 0) {
    ESP_LOGW(TAG, "WS assembly buffer unavailable");
    return;
  }

  const size_t append_len = static_cast<size_t>(length);
  if (ws_text_length + append_len >= ws_text_capacity) {
    ESP_LOGW(TAG, "WS assembly buffer overflow (%u + %u >= %u)", static_cast<unsigned>(ws_text_length), static_cast<unsigned>(append_len), static_cast<unsigned>(ws_text_capacity));
    clear_ws_text_assembly();
    return;
  }

  std::memcpy(ws_text_buffer + ws_text_length, data, append_len);
  ws_text_length += append_len;
  ws_text_buffer[ws_text_length] = '\0';

  const int received_end = event->payload_offset + length;
  if (event->payload_len > 0 && received_end < event->payload_len) {
    return;
  }

  const int message_length = static_cast<int>(ws_text_length);
  const WsMessageKind kind = classify_message(ws_text_buffer);
  bool queued = false;
  if (kind == WsMessageKind::Cover) {
    char *owned = ws_text_buffer;
    ws_text_buffer = nullptr;
    ws_text_capacity = 0;
    ws_text_length = 0;
    queued = enqueue_owned_message(owned, message_length);
  } else {
    queued = enqueue_incoming_message(ws_text_buffer, message_length);
    clear_ws_text_assembly();
  }

  if (!queued) {
    ESP_LOGW(TAG, "Failed to queue WebSocket message (%d bytes)", message_length);
  }
}

void handle_text_message(const char *data, int length) {
  if (classify_message(data) == WsMessageKind::Cover) {
    handle_cover_message(data, length);
    return;
  }

  cJSON *root = cJSON_ParseWithLength(data, length);
  if (root == nullptr) {
    ESP_LOGW(TAG, "Failed to parse WebSocket JSON");
    return;
  }

  cJSON *type = cJSON_GetObjectItem(root, "type");
  cJSON *payload = cJSON_GetObjectItem(root, "payload");
  cJSON *request_id = cJSON_GetObjectItem(root, "request_id");
  const char *type_text = cJSON_IsString(type) ? type->valuestring : "";
  const char *request_text = cJSON_IsString(request_id) ? request_id->valuestring : "";

  if (std::strcmp(type_text, "playback.state") == 0 && cJSON_IsObject(payload)) {
    cJSON *state = cJSON_GetObjectItem(payload, "state");
    cJSON *title = cJSON_GetObjectItem(payload, "title");
    cJSON *artist = cJSON_GetObjectItem(payload, "artist");
    cJSON *position_ms = cJSON_GetObjectItem(payload, "position_ms");
    cJSON *duration_ms = cJSON_GetObjectItem(payload, "duration_ms");
    ui_set_song(cJSON_IsString(title) ? title->valuestring : "Unknown title", cJSON_IsString(artist) ? artist->valuestring : "Unknown artist");
    ui_set_playback_progress(cJSON_IsNumber(position_ms) ? position_ms->valueint : 0, cJSON_IsNumber(duration_ms) ? duration_ms->valueint : 0);
    ui_set_playback_state(cJSON_IsString(state) ? state->valuestring : "playing");
  } else if (std::strcmp(type_text, "lyrics.line") == 0 && cJSON_IsObject(payload)) {
    cJSON *prev2_line = cJSON_GetObjectItem(payload, "prev2_line");
    cJSON *prev_line = cJSON_GetObjectItem(payload, "prev_line");
    cJSON *line = cJSON_GetObjectItem(payload, "line");
    cJSON *next_line = cJSON_GetObjectItem(payload, "next_line");
    cJSON *next2_line = cJSON_GetObjectItem(payload, "next2_line");
    cJSON *position_ms = cJSON_GetObjectItem(payload, "position_ms");
    cJSON *duration_ms = cJSON_GetObjectItem(payload, "duration_ms");
    ESP_LOGI(TAG, "Lyrics line received: %s", cJSON_IsString(line) ? line->valuestring : "");
    ui_set_lyric_lines(
      cJSON_IsString(prev2_line) ? prev2_line->valuestring : "",
      cJSON_IsString(prev_line) ? prev_line->valuestring : "",
      cJSON_IsString(line) ? line->valuestring : "",
      cJSON_IsString(next_line) ? next_line->valuestring : "",
      cJSON_IsString(next2_line) ? next2_line->valuestring : ""
    );
    ui_set_playback_progress(cJSON_IsNumber(position_ms) ? position_ms->valueint : 0, cJSON_IsNumber(duration_ms) ? duration_ms->valueint : 0);
  } else if (std::strcmp(type_text, "error") == 0 && cJSON_IsObject(payload)) {
    cJSON *message = cJSON_GetObjectItem(payload, "message");
    ESP_LOGW(TAG, "Received error request=%s message=%s", request_text, cJSON_IsString(message) ? message->valuestring : "");
    ui_set_status(cJSON_IsString(message) ? message->valuestring : "PC daemon error");
  } else if (std::strcmp(type_text, "ack") == 0) {
    ESP_LOGI(TAG, "Received ack request=%s", request_text);
  }

  cJSON_Delete(root);
}

void websocket_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data) {
  (void)handler_args;
  (void)base;

  auto *data = static_cast<esp_websocket_event_data_t *>(event_data);
  switch (event_id) {
    case WEBSOCKET_EVENT_BEGIN:
      ws_client_task_alive = true;
      break;
    case WEBSOCKET_EVENT_CONNECTED:
      websocket_connected = true;
      ui_set_status("WebSocket connected");
      ESP_LOGI(TAG, "WebSocket connected");
      send_json("hello", "", "{\"device_id\":\"esp32-s3-display\",\"fw_version\":\"0.1.0\",\"display\":{\"width\":800,\"height\":480}}");
      send_json("playback.sync", "sync-1", "{\"source\":\"netease_cloud_music\"}");
      if (!pending_command.empty()) {
        ESP_LOGI(TAG, "Send pending control command: %s", pending_command.c_str());
        send_control_command_now(pending_command.c_str());
        pending_command.clear();
      }
      break;
    case WEBSOCKET_EVENT_DISCONNECTED:
      websocket_connected = false;
      ESP_LOGW(TAG, "WebSocket disconnected");
      ui_set_status("Reconnecting...");
      break;
    case WEBSOCKET_EVENT_CLOSED:
      websocket_connected = false;
      ESP_LOGW(TAG, "WebSocket closed by server");
      ui_set_status("Reconnecting...");
      break;
    case WEBSOCKET_EVENT_DATA:
      if (data->op_code == 0x1) {
        handle_websocket_text_fragment(data->data_ptr, data->data_len, data);
      }
      break;
    case WEBSOCKET_EVENT_ERROR:
      websocket_connected = false;
      ESP_LOGW(TAG, "WebSocket error");
      ui_set_status("Reconnecting...");
      break;
    case WEBSOCKET_EVENT_FINISH:
      websocket_connected = false;
      ws_client_task_alive = false;
      ESP_LOGW(TAG, "WebSocket client task stopped");
      ui_set_status("Reconnecting...");
      break;
    default:
      break;
  }
}

void ping_task(void *arg) {
  (void)arg;

  while (true) {
    if (playback_mode == PlaybackMode::Playing && websocket_connected) {
      char request_id[32] = {};
      std::snprintf(request_id, sizeof(request_id), "ping-%llu", static_cast<unsigned long long>(now_ms()));
      send_json("ping", request_id, "{}");
    }
    vTaskDelay(pdMS_TO_TICKS(5000));
  }
}

void recreate_websocket_client() {
  websocket_connected = false;
  ws_client_task_alive = false;

  if (ws_client != nullptr) {
    esp_websocket_client_stop(ws_client);
    esp_websocket_client_destroy(ws_client);
    ws_client = nullptr;
  }

  websocket_start_playback_session();
}

void ensure_websocket_client_running(bool force_recreate) {
  if (playback_mode != PlaybackMode::Playing) {
    return;
  }

  if (wifi_ensure_connected() != ESP_OK) {
    ESP_LOGW(TAG, "Wi-Fi unavailable, postpone WebSocket reconnect");
    return;
  }

  if (force_recreate) {
    ESP_LOGW(TAG, "Recreating WebSocket client after repeated failures");
    recreate_websocket_client();
    return;
  }

  if (ws_client == nullptr) {
    websocket_start_playback_session();
    return;
  }

  if (esp_websocket_client_is_connected(ws_client)) {
    return;
  }

  esp_err_t err = esp_websocket_client_start(ws_client);
  if (err == ESP_OK) {
    ESP_LOGI(TAG, "Restarted WebSocket client task");
    return;
  }

  ESP_LOGW(TAG, "WebSocket client restart failed (%s), recreating client", esp_err_to_name(err));
  recreate_websocket_client();
}

void reconnect_task(void *arg) {
  (void)arg;
  uint32_t delay_ms = 2000;
  int offline_attempts = 0;

  while (true) {
    vTaskDelay(pdMS_TO_TICKS(delay_ms));
    if (playback_mode != PlaybackMode::Playing) {
      delay_ms = 2000;
      offline_attempts = 0;
      continue;
    }

    if (ws_client != nullptr && esp_websocket_client_is_connected(ws_client)) {
      delay_ms = 2000;
      offline_attempts = 0;
      continue;
    }

    offline_attempts++;
    ESP_LOGI(
      TAG,
      "WebSocket offline, trying to reconnect to PC daemon at %s:%d (attempt=%d)...",
      PC_DAEMON_HOST,
      PC_DAEMON_PORT,
      offline_attempts
    );
    ensure_websocket_client_running(offline_attempts >= 3);
    delay_ms = delay_ms < 10000 ? delay_ms + 1000 : 10000;
  }
}
}  // namespace

esp_err_t websocket_start_playback_session() {
  if (playback_mode == PlaybackMode::Playing && ws_client != nullptr) {
    if (esp_websocket_client_is_connected(ws_client)) {
      return ESP_OK;
    }
    if (ws_client_task_alive) {
      return ESP_OK;
    }
    esp_err_t restart_err = esp_websocket_client_start(ws_client);
    if (restart_err == ESP_OK) {
      return ESP_OK;
    }
    ESP_LOGW(TAG, "Existing WebSocket client cannot restart (%s), recreating", esp_err_to_name(restart_err));
    esp_websocket_client_destroy(ws_client);
    ws_client = nullptr;
    ws_client_task_alive = false;
  }

  playback_mode = PlaybackMode::Playing;
  session_id = make_session_id();
  ui_set_status("Connecting WebSocket...");

  char uri_buffer[128] = {};
  std::snprintf(uri_buffer, sizeof(uri_buffer), "ws://%s:%d%s", PC_DAEMON_HOST, PC_DAEMON_PORT, WS_PATH);
  websocket_uri = uri_buffer;

  esp_websocket_client_config_t config = {};
  config.uri = websocket_uri.c_str();
  config.reconnect_timeout_ms = 3000;
  config.network_timeout_ms = 20000;
  config.keep_alive_enable = true;
  config.keep_alive_idle = 30;
  config.keep_alive_interval = 5;
  config.keep_alive_count = 3;
  config.ping_interval_sec = 30;
  config.disable_pingpong_discon = true;
  config.enable_close_reconnect = true;
  config.buffer_size = 8192;

  ws_client = esp_websocket_client_init(&config);
  ESP_RETURN_ON_FALSE(ws_client != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket client failed");
  if (send_mutex == nullptr) {
    send_mutex = xSemaphoreCreateMutex();
    ESP_RETURN_ON_FALSE(send_mutex != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket send mutex failed");
  }
  ESP_RETURN_ON_ERROR(esp_websocket_register_events(ws_client, WEBSOCKET_EVENT_ANY, websocket_event_handler, nullptr), TAG, "Register WebSocket event failed");
  ESP_RETURN_ON_ERROR(esp_websocket_client_start(ws_client), TAG, "Start WebSocket failed");

  static bool background_tasks_started = false;
  if (!background_tasks_started) {
    if (incoming_message_queue == nullptr) {
      incoming_message_queue = xQueueCreate(kIncomingMessageQueueDepth, sizeof(WsQueuedMessage));
      ESP_RETURN_ON_FALSE(incoming_message_queue != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket incoming queue failed");
    }
    if (latest_lyrics_queue == nullptr) {
      latest_lyrics_queue = xQueueCreate(kLatestMessageQueueDepth, sizeof(WsQueuedMessage));
      ESP_RETURN_ON_FALSE(latest_lyrics_queue != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket lyrics queue failed");
    }
    if (latest_state_queue == nullptr) {
      latest_state_queue = xQueueCreate(kLatestMessageQueueDepth, sizeof(WsQueuedMessage));
      ESP_RETURN_ON_FALSE(latest_state_queue != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket state queue failed");
    }
    if (latest_cover_queue == nullptr) {
      latest_cover_queue = xQueueCreate(kLatestMessageQueueDepth, sizeof(WsQueuedMessage));
      ESP_RETURN_ON_FALSE(latest_cover_queue != nullptr, ESP_ERR_NO_MEM, TAG, "Create WebSocket cover queue failed");
    }
    xTaskCreate(ws_worker_task, "ws_worker", kWsWorkerStackSize, nullptr, 5, nullptr);
    xTaskCreate(cover_worker_task, "ws_cover", kCoverWorkerStackSize, nullptr, 3, nullptr);
    xTaskCreate(ping_task, "ws_ping", 4096, nullptr, 4, nullptr);
    xTaskCreate(reconnect_task, "ws_reconnect", 4096, nullptr, 4, nullptr);
    background_tasks_started = true;
  }

  return ESP_OK;
}

void websocket_close_playback_session(const char *reason_type) {
  if (websocket_connected) {
    send_json(reason_type != nullptr ? reason_type : "session.close", "close-1", "{}");
    vTaskDelay(pdMS_TO_TICKS(80));
  }

  playback_mode = PlaybackMode::Paused;
  websocket_connected = false;
  ws_client_task_alive = false;

  if (ws_client != nullptr) {
    esp_websocket_client_stop(ws_client);
    esp_websocket_client_destroy(ws_client);
    ws_client = nullptr;
  }

  ui_set_status("Paused");
}

void websocket_send_control_command(const char *command) {
  ESP_LOGI(TAG, "Control command requested: %s connected=%d mode=%d", command, websocket_connected, static_cast<int>(playback_mode));

  if (command != nullptr && command[0] != '\0') {
    const uint64_t now = now_ms();
    if (
      !last_control_command.empty() && last_control_command == command
      && now - last_control_command_ms < kControlDebounceMs
    ) {
      ESP_LOGW(TAG, "Ignore duplicate control command: %s", command);
      return;
    }
    last_control_command = command;
    last_control_command_ms = now;
  }

  if (playback_mode != PlaybackMode::Playing || !websocket_connected) {
    pending_command = command;
    websocket_start_playback_session();
    ui_set_status("Command pending...");
    return;
  }

  send_control_command_now(command);
}

void websocket_send_seek(int position_ms) {
  ESP_LOGI(TAG, "Seek requested: position_ms=%d connected=%d", position_ms, websocket_connected);

  if (playback_mode != PlaybackMode::Playing || !websocket_connected) {
    websocket_start_playback_session();
    ui_set_status("Seek pending...");
    return;
  }

  send_seek_command_now(position_ms);
}
