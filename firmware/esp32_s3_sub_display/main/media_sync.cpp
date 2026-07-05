#include "media_sync.h"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <string>

#include "cJSON.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "freertos/task.h"
#include "media_api_config.h"
#include "media_control.h"
#include "sd_storage.h"
#include "wifi_sta.h"

#include <dirent.h>
#include <esp_vfs_fat.h>
#include <sys/stat.h>
#include <unistd.h>

namespace {
constexpr char TAG[] = "media_sync";
constexpr int kHttpBufSize = 8192;
constexpr int kPollPlayingMs = 500;
constexpr int kPollIdleMs = 2000;
constexpr int kHttpTimeoutMs = 5000;
constexpr int kMaxRetries = 7;
constexpr int kWifiWaitMs = 60000;
constexpr int kTimerIntervalUs = 50 * 1000; /* 50ms */

/* 梯度退避延迟（秒）：1, 2, 4, 8, 16, 32, 64 */
constexpr int kRetryDelaysSec[kMaxRetries] = {1, 2, 4, 8, 16, 32, 64};

char s_http_buf[kHttpBufSize];
MediaSnapshot s_snapshot = {};
SemaphoreHandle_t s_lock = nullptr;
volatile bool s_dirty = false;
volatile media_cmd_t s_pending_cmd = MEDIA_CMD_PREVIOUS;
volatile bool s_has_pending_cmd = false;
volatile bool s_pending_start = false;
volatile media_cmd_result_t s_cmd_result = MEDIA_CMD_RESULT_IDLE;
volatile media_cmd_t s_last_result_cmd = MEDIA_CMD_PREVIOUS;

/* WS 重连状态（这里指 HTTP 连接状态） */
volatile media_ws_state_t s_ws_state = MEDIA_WS_IDLE;
int s_consecutive_failures = 0;
volatile bool s_reconnect_requested = false;

/* 当前歌曲标识，检测切歌 */
char s_current_song_key[MEDIA_TITLE_MAX + MEDIA_ARTIST_MAX + 4] = "";

/* 定期请求歌词的时间戳 (ms) */
int64_t s_last_lyrics_pull_ms = 0;

/* 本地计时器 */
esp_timer_handle_t s_timer = nullptr;
volatile int64_t s_timer_base_ms = 0;   /* 计时器启动时的 position */
volatile int64_t s_timer_start_us = 0;  /* 计时器启动时的 esp_timer 时间 */
volatile bool s_timer_running = false;

/* LRC lyrics directory on SD */
constexpr char kLyricDir[] = "/sdcard/lyrics";
constexpr char kLrcCacheFile[] = "/sdcard/lyrics/current.lrc";
constexpr char kMetaCacheFile[] = "/sdcard/lyrics/current.meta";

struct HttpResponse {
  int status = 0;
  int len = 0;
};

void copy_str(char *dst, size_t dst_len, const char *src) {
  if (dst == nullptr || dst_len == 0) return;
  if (src == nullptr) { dst[0] = '\0'; return; }
  std::strncpy(dst, src, dst_len - 1);
  dst[dst_len - 1] = '\0';
}

void set_snapshot_locked(const MediaSnapshot &next) {
  s_snapshot = next;
  s_dirty = true;
}

/* ========== LRC 解析 ========== */

int parse_lrc_lines(const char *lrc_text, LrcLine *out_lines, int max_lines) {
  if (lrc_text == nullptr || lrc_text[0] == '\0') return 0;
  int count = 0;
  const char *p = lrc_text;
  while (*p && count < max_lines) {
    /* 找一行 */
    const char *nl = std::strchr(p, '\n');
    std::string line;
    if (nl) {
      line = std::string(p, nl - p);
      p = nl + 1;
    } else {
      line = std::string(p);
      p += line.size();
    }
    /* 跳过空行 */
    if (line.empty()) continue;
    /* 解析时间戳 [mm:ss.xx] */
    int minute = 0, second = 0, frac = 0;
    if (std::sscanf(line.c_str(), "[%d:%d.%d]", &minute, &second, &frac) >= 2) {
      int32_t start_ms = minute * 60000 + second * 1000 + frac * 10;
      /* 提取歌词文本（去掉时间戳） */
      const char *text_start = std::strchr(line.c_str(), ']');
      if (text_start == nullptr) continue;
      text_start++; /* skip ']' */
      while (*text_start == ' ' || *text_start == '\t') text_start++;
      if (*text_start == '\0') continue;
      out_lines[count].start_ms = start_ms;
      copy_str(out_lines[count].text, sizeof(out_lines[count].text), text_start);
      count++;
    } else {
      /* 没有时间戳的行跳过（可能是元数据） */
      continue;
    }
  }
  return count;
}

void update_lyric_snapshot(MediaSnapshot *snap) {
  if (snap == nullptr) return;
  /* 根据当前位置找行 */
  int idx = 0;
  for (int i = 0; i < snap->lrc_line_count; i++) {
    if (snap->lrc_lines[i].start_ms <= snap->position_ms) {
      idx = i;
    } else {
      break;
    }
  }
  snap->current_line_index = idx;

  /* 填充 5 行快照 */
  snap->prev_prev_line[0] = '\0';
  snap->prev_line[0] = '\0';
  snap->line[0] = '\0';
  snap->next_line[0] = '\0';
  snap->next_next_line[0] = '\0';

  if (idx >= 0 && idx < snap->lrc_line_count) {
    copy_str(snap->line, sizeof(snap->line), snap->lrc_lines[idx].text);
    snap->line_start_ms = snap->lrc_lines[idx].start_ms;
    snap->line_end_ms = (idx + 1 < snap->lrc_line_count)
                            ? snap->lrc_lines[idx + 1].start_ms
                            : snap->lrc_lines[idx].start_ms + 8000;
    if (idx >= 2) copy_str(snap->prev_prev_line, sizeof(snap->prev_prev_line), snap->lrc_lines[idx - 2].text);
    if (idx >= 1) copy_str(snap->prev_line, sizeof(snap->prev_line), snap->lrc_lines[idx - 1].text);
    if (idx + 1 < snap->lrc_line_count)
      copy_str(snap->next_line, sizeof(snap->next_line), snap->lrc_lines[idx + 1].text);
    if (idx + 2 < snap->lrc_line_count)
      copy_str(snap->next_next_line, sizeof(snap->next_next_line), snap->lrc_lines[idx + 2].text);
  }
}

/* ========== TF 卡 LRC 缓存 ========== */

bool ensure_lyric_dir() {
  struct stat st = {};
  if (::stat(kLyricDir, &st) == 0 && S_ISDIR(st.st_mode)) return true;
  return ::mkdir(kLyricDir, 0777) == 0;
}

bool save_lrc_to_sd(const char *title, const char *artist, const char *lrc_text) {
  if (!sd_storage_is_mounted()) return false;
  ensure_lyric_dir();

  /* 保存元数据 */
  FILE *meta = ::fopen(kMetaCacheFile, "w");
  if (meta) {
    if (title && title[0]) {
      ::fprintf(meta, "%s", title);
      if (artist && artist[0]) ::fprintf(meta, " - %s", artist);
    }
    ::fclose(meta);
  }

  /* 保存 LRC 文本 */
  FILE *f = ::fopen(kLrcCacheFile, "w");
  if (!f) return false;
  if (lrc_text) ::fprintf(f, "%s", lrc_text);
  ::fclose(f);
  ESP_LOGI(TAG, "LRC saved to SD: %s (%d bytes)", kLrcCacheFile,
           lrc_text ? (int)std::strlen(lrc_text) : 0);
  return true;
}

bool load_lrc_from_sd(char *title_out, size_t title_len,
                      char *lrc_out, size_t lrc_len) {
  if (!sd_storage_is_mounted()) return false;

  /* 读元数据 */
  if (title_out && title_len > 0) {
    FILE *meta = ::fopen(kMetaCacheFile, "r");
    if (meta) {
      size_t n = ::fread(title_out, 1, title_len - 1, meta);
      title_out[n] = '\0';
      /* 去掉末尾换行 */
      while (n > 0 && (title_out[n - 1] == '\n' || title_out[n - 1] == '\r')) title_out[--n] = '\0';
      ::fclose(meta);
    } else {
      title_out[0] = '\0';
    }
  }

  /* 读 LRC */
  if (lrc_out && lrc_len > 0) {
    FILE *f = ::fopen(kLrcCacheFile, "r");
    if (!f) return false;
    size_t n = ::fread(lrc_out, 1, lrc_len - 1, f);
    lrc_out[n] = '\0';
    ::fclose(f);
    return n > 0;
  }
  return false;
}

/* ========== HTTP 请求 ========== */

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
  char url[256];
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
  if (client == nullptr) return false;
  if (json_body != nullptr) {
    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, json_body, std::strlen(json_body));
  }

  const esp_err_t err = esp_http_client_perform(client);
  resp->status = esp_http_client_get_status_code(client);
  esp_http_client_cleanup(client);
  return err == ESP_OK && resp->status >= 200 && resp->status < 300;
}

/* ========== 解析响应 ========== */

/**
 * 解析 /api/media/lyrics 响应
 * {"cmd":"song","title":"夜曲","artist":"周杰伦","lyrics":"....\n...\n"}
 */
bool parse_lyrics_response(const char *song_key) {
  cJSON *root = cJSON_Parse(s_http_buf);
  if (root == nullptr) return false;

  const char *cmd = cJSON_GetStringValue(cJSON_GetObjectItem(root, "cmd"));
  if (cmd == nullptr || std::strcmp(cmd, "song") != 0) {
    cJSON_Delete(root);
    return false;
  }

  const char *title = cJSON_GetStringValue(cJSON_GetObjectItem(root, "title"));
  const char *artist = cJSON_GetStringValue(cJSON_GetObjectItem(root, "artist"));
  const char *lyrics = cJSON_GetStringValue(cJSON_GetObjectItem(root, "lyrics"));

  if (lyrics == nullptr || lyrics[0] == '\0') {
    lyrics = title ? title : "No lyrics";
  }
  if (title == nullptr) title = "";
  if (artist == nullptr) artist = "";

  ESP_LOGI(TAG, "Lyrics received: title='%s' artist='%s' lyrics_len=%d", title, artist,
           lyrics ? (int)std::strlen(lyrics) : 0);

  /* 保存到 TF 卡 */
  save_lrc_to_sd(title, artist, lyrics);

  /* 解析到内存 */
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    MediaSnapshot snap = s_snapshot;
    snap.lrc_line_count = parse_lrc_lines(lyrics, snap.lrc_lines, MEDIA_LRC_LINES_MAX);
    copy_str(snap.title, sizeof(snap.title), title);
    copy_str(snap.artist, sizeof(snap.artist), artist);
    ESP_LOGI(TAG, "Parsed %d LRC lines from server", snap.lrc_line_count);
    if (snap.lrc_line_count > 0) {
      update_lyric_snapshot(&snap);
    }
    if (song_key) copy_str(s_current_song_key, sizeof(s_current_song_key), song_key);
    s_snapshot = snap;
    s_dirty = true;
    xSemaphoreGive(s_lock);
  }

  cJSON_Delete(root);
  return true;
}

/**
 * 解析 /api/media/status 响应
 * {"cmd":"sync","position":65231,"playing":true}
 */
bool parse_status_response() {
  cJSON *root = cJSON_Parse(s_http_buf);
  if (root == nullptr) return false;

  /* 根层直接就是 {"cmd":"sync","position":...,"playing":...} */
  const char *cmd = cJSON_GetStringValue(cJSON_GetObjectItem(root, "cmd"));
  if (cmd == nullptr || std::strcmp(cmd, "sync") != 0) {
    cJSON_Delete(root);
    return false;
  }

  MediaSnapshot next = {};
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    next = s_snapshot;  /* 保留之前的 LRC 数据 */
    xSemaphoreGive(s_lock);
  }
  next.connected = true;

  /* playing */
  next.playing = cJSON_IsTrue(cJSON_GetObjectItem(root, "playing"));

  /* position */
  const cJSON *pos_item = cJSON_GetObjectItem(root, "position");
  int32_t server_pos = 0;
  if (cJSON_IsNumber(pos_item)) server_pos = pos_item->valueint;
  next.position_ms = server_pos;

  /* 更新本地计时器 */
  if (next.playing) {
    s_timer_base_ms = server_pos;
    s_timer_start_us = esp_timer_get_time();
    s_timer_running = true;
  } else {
    s_timer_running = false;
  }

  /* 更新歌词快照 */
  if (next.lrc_line_count > 0) {
    update_lyric_snapshot(&next);
  }

  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    set_snapshot_locked(next);
    xSemaphoreGive(s_lock);
  }
  media_control_set_playing(next.playing);

  ESP_LOGI(TAG, "Sync received: playing=%d position=%d lrc_lines=%d line='%s' prev='%s' next='%s'",
           next.playing, next.position_ms, next.lrc_line_count,
           next.line, next.prev_line, next.next_line);

  cJSON_Delete(root);
  return true;
}

/* ========== API 调用 ========== */

bool pull_lyrics() {
  HttpResponse resp;
  if (!http_request("GET", "/api/media/lyrics", nullptr, &resp)) return false;
  return parse_lyrics_response(nullptr);
}

bool pull_status() {
  HttpResponse resp;
  if (!http_request("GET", "/api/media/status", nullptr, &resp)) return false;
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
    case MEDIA_CMD_PREVIOUS: return "previous";
    case MEDIA_CMD_TOGGLE_PLAY_PAUSE: return "toggle";
    case MEDIA_CMD_NEXT: return "next";
    default: return nullptr;
  }
}

void process_pending_actions() {
  if (s_pending_start) {
    s_pending_start = false;
    s_cmd_result = MEDIA_CMD_RESULT_SENDING;
    s_last_result_cmd = MEDIA_CMD_START_APP;
    if (!post_start()) {
      ESP_LOGW(TAG, "Start NetEase request failed");
      s_cmd_result = MEDIA_CMD_RESULT_FAILED;
    } else {
      s_cmd_result = MEDIA_CMD_RESULT_SUCCESS;
    }
  }
  if (!s_has_pending_cmd) return;
  media_cmd_t cmd = s_pending_cmd;
  s_has_pending_cmd = false;
  s_cmd_result = MEDIA_CMD_RESULT_SENDING;
  s_last_result_cmd = cmd;
  if (cmd == MEDIA_CMD_START_APP) {
    if (!post_start()) {
      ESP_LOGW(TAG, "Start NetEase request failed");
      s_cmd_result = MEDIA_CMD_RESULT_FAILED;
    } else {
      s_cmd_result = MEDIA_CMD_RESULT_SUCCESS;
    }
    return;
  }
  const char *command = cmd_to_string(cmd);
  if (command == nullptr) { s_cmd_result = MEDIA_CMD_RESULT_FAILED; return; }
  if (!post_control(command)) {
    ESP_LOGW(TAG, "Control command failed: %s", command);
    s_cmd_result = MEDIA_CMD_RESULT_FAILED;
  } else {
    s_cmd_result = MEDIA_CMD_RESULT_SUCCESS;
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

/* ========== 本地计时器回调 ========== */

void timer_cb(void *arg) {
  (void)arg;
  if (!s_timer_running) return;
  int64_t elapsed_us = esp_timer_get_time() - s_timer_start_us;
  int32_t estimated_pos = (int32_t)(s_timer_base_ms + elapsed_us / 1000);

  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(10)) == pdTRUE) {
    if (s_snapshot.playing) {
      s_snapshot.position_ms = estimated_pos;
      update_lyric_snapshot(&s_snapshot);
      s_dirty = true;
    }
    xSemaphoreGive(s_lock);
  }
}

/* ========== 主同步任务 ========== */

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

  /* 启动本地计时器 */
  esp_timer_create_args_t timer_args = {};
  timer_args.callback = timer_cb;
  timer_args.name = "lyric_timer";
  if (esp_timer_create(&timer_args, &s_timer) != ESP_OK) {
    ESP_LOGW(TAG, "Failed to create timer");
  } else {
    esp_timer_start_periodic(s_timer, kTimerIntervalUs);
    ESP_LOGI(TAG, "Lyric timer started (period=%dms)", kTimerIntervalUs / 1000);
  }

  bool initial_connected = false;

  for (;;) {
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
      if (!initial_connected) {
        initial_connected = true;
        ESP_LOGI(TAG, "First successful connection, fetching lyrics...");
        /* 首次连接成功，获取歌词 */
        pull_lyrics();
      }
      s_consecutive_failures = 0;
      s_ws_state = MEDIA_WS_IDLE;

      /* 定期（每 30s）请求歌词，用于切歌检测 */
      int64_t now_ms = esp_timer_get_time() / 1000;
      if (now_ms - s_last_lyrics_pull_ms > 30000) {
        s_last_lyrics_pull_ms = now_ms;
        pull_lyrics();
      }
    } else {
      s_consecutive_failures++;
      ESP_LOGW(TAG, "Media sync failed (%d/%d)", s_consecutive_failures, kMaxRetries);

      MediaSnapshot offline = {};
      if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
        offline = s_snapshot;
        offline.connected = false;
        offline.playing = false;
        set_snapshot_locked(offline);
        xSemaphoreGive(s_lock);
      }
      media_control_set_playing(false);
      s_timer_running = false;

      if (s_consecutive_failures >= kMaxRetries) {
        s_ws_state = MEDIA_WS_GAVE_UP;
        ESP_LOGW(TAG, "Gave up after %d retries, waiting for manual reconnect", kMaxRetries);
        /* 进入等待手动重连模式 */
        while (s_ws_state == MEDIA_WS_GAVE_UP) {
          if (s_reconnect_requested) {
            s_reconnect_requested = false;
            s_consecutive_failures = 0;
            s_ws_state = MEDIA_WS_RETRYING;
            break;
          }
          vTaskDelay(pdMS_TO_TICKS(500));
        }
        continue;
      }

      /* 梯度退避等待 */
      int delay_sec = kRetryDelaysSec[s_consecutive_failures - 1];
      s_ws_state = MEDIA_WS_RETRYING;
      int waited = 0;
      while (waited < delay_sec * 1000) {
        if (s_reconnect_requested) {
          s_reconnect_requested = false;
          s_consecutive_failures = 0;
          break;
        }
        vTaskDelay(pdMS_TO_TICKS(200));
        waited += 200;
      }
    }

    /* 判断下一次轮询间隔 */
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
    if (s_lock == nullptr) return ESP_ERR_NO_MEM;
  }
  BaseType_t ok = xTaskCreate(sync_task, "media_sync", 10240, nullptr, 4, nullptr);
  return ok == pdPASS ? ESP_OK : ESP_FAIL;
#endif
}

bool media_sync_is_connected(void) {
  if (s_lock == nullptr) return false;
  bool connected = false;
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(50)) == pdTRUE) {
    connected = s_snapshot.connected;
    xSemaphoreGive(s_lock);
  }
  return connected;
}

void media_sync_get_snapshot(MediaSnapshot *out) {
  if (out == nullptr || s_lock == nullptr) return;
  if (xSemaphoreTake(s_lock, pdMS_TO_TICKS(100)) == pdTRUE) {
    *out = s_snapshot;
    xSemaphoreGive(s_lock);
  }
}

bool media_sync_consume_dirty(void) {
  if (!s_dirty) return false;
  s_dirty = false;
  return true;
}

void media_sync_queue_command(media_cmd_t cmd) {
  s_pending_cmd = cmd;
  s_has_pending_cmd = true;
  s_cmd_result = MEDIA_CMD_RESULT_PENDING;
  s_last_result_cmd = cmd;
}

void media_sync_queue_start(void) {
  s_pending_start = true;
  s_cmd_result = MEDIA_CMD_RESULT_PENDING;
  s_last_result_cmd = MEDIA_CMD_START_APP;
}

void media_sync_request_reconnect(void) {
  s_reconnect_requested = true;
  if (s_ws_state == MEDIA_WS_GAVE_UP) {
    s_ws_state = MEDIA_WS_RETRYING;
  }
  ESP_LOGI(TAG, "Reconnect requested");
}

media_ws_state_t media_sync_get_ws_state(void) {
  return s_ws_state;
}

media_cmd_result_t media_sync_get_cmd_result(void) {
  return s_cmd_result;
}

media_cmd_result_t media_sync_consume_cmd_result(void) {
  media_cmd_result_t r = s_cmd_result;
  if (r == MEDIA_CMD_RESULT_SUCCESS || r == MEDIA_CMD_RESULT_FAILED) {
    s_cmd_result = MEDIA_CMD_RESULT_IDLE;
  }
  return r;
}

media_cmd_t media_sync_get_pending_cmd(void) {
  return s_last_result_cmd;
}

void media_sync_set_cmd_result(media_cmd_result_t result) {
  s_cmd_result = result;
}

}  // extern "C"