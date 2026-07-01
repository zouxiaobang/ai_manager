#include "pomodoro_api_config.h"

#include <cctype>
#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "panel_config.h"
#include "sd_storage.h"

namespace {
constexpr char TAG[] = "pomo_api";
constexpr char kHostFile[] = SD_MOUNT_POINT "/config/pomodoro_host.txt";
constexpr size_t kHostMax = 64;

char s_host[kHostMax] = {};
int s_port = 8080;

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
  if (end < len && line[end - 1] == '\r') {
    line[end - 1] = '\0';
  }
}

bool parse_host_file() {
  FILE *fp = std::fopen(kHostFile, "r");
  if (fp == nullptr) {
    return false;
  }

  char line[kHostMax + 16];
  bool got_host = false;
  int line_no = 0;
  while (std::fgets(line, sizeof(line), fp) != nullptr) {
    line_no++;
    trim_inplace(line);
    if (line[0] == '\0' || line[0] == '#') {
      continue;
    }
    if (!got_host) {
      std::strncpy(s_host, line, sizeof(s_host) - 1);
      s_host[sizeof(s_host) - 1] = '\0';
      got_host = true;
      continue;
    }
    const int port = std::atoi(line);
    if (port > 0 && port < 65536) {
      s_port = port;
    } else {
      ESP_LOGW(TAG, "Ignore invalid port on line %d in %s", line_no, kHostFile);
    }
    break;
  }
  std::fclose(fp);
  return got_host && s_host[0] != '\0';
}

void load_from_kconfig() {
  std::strncpy(s_host, CONFIG_POMO_API_HOST, sizeof(s_host) - 1);
  s_host[sizeof(s_host) - 1] = '\0';
  s_port = CONFIG_POMO_API_PORT;
}
}  // namespace

esp_err_t pomodoro_api_config_load() {
  load_from_kconfig();

  if (sd_storage_is_mounted() && parse_host_file()) {
    ESP_LOGI(TAG, "Backend from SD: %s:%d", s_host, s_port);
    return ESP_OK;
  }

  ESP_LOGI(TAG, "Backend from menuconfig: %s:%d", s_host, s_port);
  ESP_LOGI(TAG, "Tip: create %s (line1=host/IP, line2=port) to avoid reflash when PC IP changes",
           kHostFile);
  return ESP_OK;
}

const char *pomodoro_api_get_host() {
  return s_host;
}

int pomodoro_api_get_port() {
  return s_port;
}
