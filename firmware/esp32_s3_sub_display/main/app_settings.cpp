#include "app_settings.h"

#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"
#include "panel_config.h"
#include "sd_storage.h"

namespace {
constexpr char TAG[] = "settings";
constexpr char kNvsNs[] = "sub_disp";
constexpr char kNvsKey[] = "cfg";

AppSettings g_settings;

bool read_host_file(const char *path, char *host_out, size_t host_max, uint16_t *port_out,
                    uint16_t default_port) {
  FILE *fp = std::fopen(path, "r");
  if (fp == nullptr) {
    return false;
  }
  char line[80];
  bool got_host = false;
  while (std::fgets(line, sizeof(line), fp) != nullptr) {
    char *p = line;
    while (*p != '\0' && (*p == ' ' || *p == '\t' || *p == '\r' || *p == '\n')) {
      p++;
    }
    if (*p == '\0' || *p == '#') {
      continue;
    }
    if (!got_host) {
      std::strncpy(host_out, p, host_max - 1);
      host_out[host_max - 1] = '\0';
      char *nl = std::strchr(host_out, '\r');
      if (nl != nullptr) *nl = '\0';
      nl = std::strchr(host_out, '\n');
      if (nl != nullptr) *nl = '\0';
      got_host = true;
    } else {
      int port = std::atoi(p);
      if (port > 0 && port < 65536) {
        *port_out = static_cast<uint16_t>(port);
      }
      break;
    }
  }
  std::fclose(fp);
  return got_host && host_out[0] != '\0';
}

void load_hosts_from_sd() {
  if (!sd_storage_is_mounted()) {
    return;
  }
  read_host_file(SD_MOUNT_POINT "/config/pomodoro_host.txt",
                 g_settings.pomodoro_host, APP_SETTINGS_HOST_LEN,
                 &g_settings.pomodoro_port, 8080);
  read_host_file(SD_MOUNT_POINT "/config/pixel_dog_host.txt",
                 g_settings.pixel_dog_host, APP_SETTINGS_HOST_LEN,
                 &g_settings.pixel_dog_port, 8080);
  read_host_file(SD_MOUNT_POINT "/config/media_host.txt",
                 g_settings.media_host, APP_SETTINGS_HOST_LEN,
                 &g_settings.media_port, 8765);
}

void save_hosts_to_sd() {
  if (!sd_storage_is_mounted()) {
    return;
  }
  auto write_file = [](const char *path, const char *host, uint16_t port) {
    FILE *fp = std::fopen(path, "w");
    if (fp == nullptr) return;
    std::fprintf(fp, "%s\n%u\n", host, port);
    std::fclose(fp);
  };
  write_file(SD_MOUNT_POINT "/config/pomodoro_host.txt",
             g_settings.pomodoro_host, g_settings.pomodoro_port);
  write_file(SD_MOUNT_POINT "/config/pixel_dog_host.txt",
             g_settings.pixel_dog_host, g_settings.pixel_dog_port);
  write_file(SD_MOUNT_POINT "/config/media_host.txt",
             g_settings.media_host, g_settings.media_port);
}

}  // namespace

void app_settings_init() {
  nvs_handle_t handle = 0;
  if (nvs_open(kNvsNs, NVS_READONLY, &handle) != ESP_OK) {
    ESP_LOGI(TAG, "Using default settings");
    load_hosts_from_sd();
    return;
  }
  size_t len = sizeof(AppSettings);
  if (nvs_get_blob(handle, kNvsKey, &g_settings, &len) == ESP_OK && len == sizeof(AppSettings)) {
    ESP_LOGI(TAG, "Settings loaded from NVS");
  } else {
    ESP_LOGI(TAG, "NVS settings size mismatch or missing, using defaults");
    g_settings = AppSettings{};
  }
  nvs_close(handle);
  load_hosts_from_sd();
}

const AppSettings &app_settings_get() {
  return g_settings;
}

void app_settings_set(const AppSettings &settings) {
  g_settings = settings;
}

void app_settings_save() {
  nvs_handle_t handle = 0;
  if (nvs_open(kNvsNs, NVS_READWRITE, &handle) != ESP_OK) {
    return;
  }
  nvs_set_blob(handle, kNvsKey, &g_settings, sizeof(AppSettings));
  nvs_commit(handle);
  nvs_close(handle);
  save_hosts_to_sd();
  ESP_LOGI(TAG, "Settings saved to NVS and SD card");
}

bool app_settings_is_night_period(int hour, int min) {
  const AppSettings &s = app_settings_get();
  if (!s.night_dim_enable) {
    return false;
  }
  const int now = hour * 60 + min;
  const int start = s.night_start_hour * 60 + s.night_start_min;
  const int end = s.night_end_hour * 60 + s.night_end_min;
  if (start == end) {
    return false;
  }
  if (start < end) {
    return now >= start && now < end;
  }
  return now >= start || now < end;
}
