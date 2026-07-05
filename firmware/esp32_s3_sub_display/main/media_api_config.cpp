#include "media_api_config.h"

#include <cctype>
#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "panel_config.h"
#include "pomodoro_api_config.h"
#include "sd_storage.h"

namespace {
constexpr char TAG[] = "media_api";
constexpr char kHostFile[] = SD_MOUNT_POINT "/config/media_host.txt";
constexpr size_t kHostMax = 64;

char s_host[kHostMax] = {};
int s_port = 8765;

void trim_inplace(char *line) {
  if (line == nullptr) {
    return;
  }
  char *start = line;
  while (*start != '\0' && std::isspace(static_cast<unsigned char>(*start))) {
    start++;
  }
  if (start != line) {
    std::memmove(line, start, std::strlen(start) + 1);
  }
  const size_t len = std::strlen(line);
  size_t end = len;
  while (end > 0 && std::isspace(static_cast<unsigned char>(line[end - 1]))) {
    end--;
  }
  line[end] = '\0';
  if (end > 0 && line[end - 1] == '\r') {
    line[end - 1] = '\0';
  }
}

bool parse_host_file() {
  FILE *fp = std::fopen(kHostFile, "r");
  if (fp == nullptr) {
    ESP_LOGW(TAG, "Cannot open %s", kHostFile);
    return false;
  }

  char line[kHostMax + 16];
  bool got_host = false;
  int line_no = 0;
  while (std::fgets(line, sizeof(line), fp) != nullptr) {
    line_no++;
    trim_inplace(line);
    ESP_LOGI(TAG, "File line %d: '%s'", line_no, line);
    if (line[0] == '\0' || line[0] == '#') {
      continue;
    }
    if (!got_host) {
      std::strncpy(s_host, line, sizeof(s_host) - 1);
      s_host[sizeof(s_host) - 1] = '\0';
      got_host = true;
      ESP_LOGI(TAG, "Parsed host: '%s'", s_host);
      continue;
    }
    const int port = std::atoi(line);
    if (port > 0 && port < 65536) {
      s_port = port;
      ESP_LOGI(TAG, "Parsed port: %d", s_port);
    } else {
      ESP_LOGW(TAG, "Ignore invalid port on line %d: '%s'", line_no, line);
    }
    break;
  }
  std::fclose(fp);
  ESP_LOGI(TAG, "parse_host_file result: got_host=%s host='%s'", got_host ? "true" : "false", s_host);
  return got_host && s_host[0] != '\0';
}

void load_from_kconfig() {
#if CONFIG_MEDIA_SYNC_ENABLE
  std::strncpy(s_host, CONFIG_MEDIA_API_HOST, sizeof(s_host) - 1);
  s_host[sizeof(s_host) - 1] = '\0';
  s_port = CONFIG_MEDIA_API_PORT;
#else
  s_host[0] = '\0';
  s_port = 8765;
#endif
}

void load_from_pomodoro_fallback() {
  pomodoro_api_config_load();
  std::strncpy(s_host, pomodoro_api_get_host(), sizeof(s_host) - 1);
  s_host[sizeof(s_host) - 1] = '\0';
  s_port = 8765;
}
}  // namespace

esp_err_t media_api_config_load() {
  load_from_kconfig();
  ESP_LOGI(TAG, "After kconfig: host=%s port=%d", s_host, s_port);

  const bool sd_mounted = sd_storage_is_mounted();
  ESP_LOGI(TAG, "SD mounted: %s", sd_mounted ? "yes" : "no");

  if (sd_mounted && parse_host_file()) {
    ESP_LOGI(TAG, "Media bridge from SD: %s:%d", s_host, s_port);
    return ESP_OK;
  }

  ESP_LOGI(TAG, "SD parse result: host=%s port=%d", s_host, s_port);

  if (s_host[0] == '\0' || std::strcmp(s_host, "192.168.1.100") == 0) {
    load_from_pomodoro_fallback();
    ESP_LOGI(TAG, "Media bridge fallback to pomodoro host: %s:%d", s_host, s_port);
    return ESP_OK;
  }

  ESP_LOGI(TAG, "Media bridge from menuconfig: %s:%d", s_host, s_port);
  ESP_LOGI(TAG, "Tip: create %s (line1=PC IP, line2=port)", kHostFile);
  return ESP_OK;
}

const char *media_api_get_host() {
  return s_host;
}

int media_api_get_port() {
  return s_port;
}
