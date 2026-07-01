#include "assets_seed.h"

#include "panel_config.h"
#include "sd_storage.h"

#include <cerrno>
#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "sys/stat.h"
#include "unistd.h"

namespace {
constexpr char TAG[] = "assets_seed";
constexpr int kWriteRetries = 4;
constexpr int kRetryDelayMs = 40;

struct SeedEntry {
  const char *relative_path;
  const uint8_t *data;
  size_t data_len;
};

const SeedEntry kSeedFiles[] = {
    {"assets/tomato.png", k_seed_assets_tomato_png, k_seed_assets_tomato_png_len},
    {"assets/icon_wifi.png", k_seed_assets_icon_wifi_png, k_seed_assets_icon_wifi_png_len},
    {"assets/icon_lock.png", k_seed_assets_icon_lock_png, k_seed_assets_icon_lock_png_len},
    {"assets/icon_unlock.png", k_seed_assets_icon_unlock_png, k_seed_assets_icon_unlock_png_len},
    {"assets/icon_eq.png", k_seed_assets_icon_eq_png, k_seed_assets_icon_eq_png_len},
    {"assets/deco_diamond.png", k_seed_assets_deco_diamond_png, k_seed_assets_deco_diamond_png_len},
    {"assets/deco_diamond_blue.png", k_seed_assets_deco_diamond_blue_png, k_seed_assets_deco_diamond_blue_png_len},
    {"assets/dock_pomo.png", k_seed_assets_dock_pomo_png, k_seed_assets_dock_pomo_png_len},
    {"assets/dock_home.png", k_seed_assets_dock_home_png, k_seed_assets_dock_home_png_len},
    {"assets/dock_lyrics.png", k_seed_assets_dock_lyrics_png, k_seed_assets_dock_lyrics_png_len},
    {"assets/dock_lock.png", k_seed_assets_dock_lock_png, k_seed_assets_dock_lock_png_len},
    {"assets/dock_settings.png", k_seed_assets_dock_settings_png, k_seed_assets_dock_settings_png_len},
    {"lyrics/current.meta", k_seed_lyrics_current_meta, k_seed_lyrics_current_meta_len},
    {"lyrics/current.txt", k_seed_lyrics_current_txt, k_seed_lyrics_current_txt_len},
};

bool ensure_dir(const char *path) {
  struct stat st = {};
  if (stat(path, &st) == 0 && S_ISDIR(st.st_mode)) {
    return true;
  }
  if (mkdir(path, 0755) == 0) {
    return true;
  }
  return stat(path, &st) == 0 && S_ISDIR(st.st_mode);
}

bool seed_file_size_ok(const char *full_path, size_t expected_len) {
  struct stat st = {};
  if (stat(full_path, &st) != 0 || !S_ISREG(st.st_mode)) {
    return false;
  }
  return static_cast<size_t>(st.st_size) == expected_len;
}

bool write_seed_file(const char *full_path, const uint8_t *data, size_t data_len) {
  for (int attempt = 0; attempt < kWriteRetries; ++attempt) {
    if (attempt > 0) {
      vTaskDelay(pdMS_TO_TICKS(kRetryDelayMs));
      unlink(full_path);
    }

    FILE *fp = std::fopen(full_path, "wb");
    if (fp == nullptr) {
      ESP_LOGW(TAG, "fopen %s failed (errno=%d %s), attempt %d/%d", full_path, errno, std::strerror(errno),
               attempt + 1, kWriteRetries);
      continue;
    }

    const size_t n = std::fwrite(data, 1, data_len, fp);
    std::fflush(fp);
    const int fd = fileno(fp);
    if (fd >= 0) {
      fsync(fd);
    }
    std::fclose(fp);

    if (n != data_len) {
      ESP_LOGW(TAG, "Short write %s (%u/%u), attempt %d/%d", full_path, static_cast<unsigned>(n),
               static_cast<unsigned>(data_len), attempt + 1, kWriteRetries);
      continue;
    }
    if (!seed_file_size_ok(full_path, data_len)) {
      ESP_LOGW(TAG, "Size verify failed %s, attempt %d/%d", full_path, attempt + 1, kWriteRetries);
      continue;
    }
    return true;
  }
  return false;
}
}  // namespace

size_t assets_seed_expected_len(const char *relative_path) {
  if (relative_path == nullptr) {
    return 0;
  }
  for (const auto &entry : kSeedFiles) {
    if (std::strcmp(entry.relative_path, relative_path) == 0) {
      return entry.data_len;
    }
  }
  return 0;
}

esp_err_t assets_seed_sdcard() {
  if (!sd_storage_is_mounted()) {
    return ESP_ERR_INVALID_STATE;
  }

  if (!ensure_dir(SD_MOUNT_POINT "/assets") || !ensure_dir(SD_MOUNT_POINT "/lyrics")) {
    ESP_LOGE(TAG, "Failed to create SD asset directories");
    return ESP_FAIL;
  }

  int written = 0;
  int failed = 0;
  for (const auto &entry : kSeedFiles) {
    char full_path[160];
    std::snprintf(full_path, sizeof(full_path), "%s/%s", SD_MOUNT_POINT, entry.relative_path);
    if (seed_file_size_ok(full_path, entry.data_len)) {
      continue;
    }

    if (write_seed_file(full_path, entry.data, entry.data_len)) {
      ++written;
      ESP_LOGI(TAG, "Wrote SD asset: %s (%u bytes)", entry.relative_path,
               static_cast<unsigned>(entry.data_len));
    } else {
      ++failed;
      ESP_LOGE(TAG, "Cannot write %s after %d attempts", full_path, kWriteRetries);
    }

    vTaskDelay(pdMS_TO_TICKS(10));
  }

  if (written > 0) {
    ESP_LOGI(TAG, "Seeded %d file(s) to SD card", written);
  }
  if (failed > 0) {
    ESP_LOGE(TAG, "Failed to seed %d file(s) on SD card", failed);
    return ESP_FAIL;
  }
  if (written == 0) {
    ESP_LOGI(TAG, "SD card assets already present");
  }
  return ESP_OK;
}
