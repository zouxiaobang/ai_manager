#include "assets_loader.h"

#include "assets_seed.h"
#include "panel_config.h"
#include "sd_assets.h"
#include "sd_storage.h"
#include "ui_embed_images.h"

#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "draw/lv_image_decoder.h"
#include "sys/stat.h"

namespace {
constexpr char TAG[] = "assets";
constexpr char kLvDrivePrefix[] = "A:/";

const char *join_mount_path(char *buf, size_t buf_size, const char *relative_path) {
  std::snprintf(buf, buf_size, "%s/%s", SD_MOUNT_POINT, relative_path);
  return buf;
}

bool is_png_file(const char *full_path) {
  FILE *fp = std::fopen(full_path, "rb");
  if (fp == nullptr) {
    return false;
  }
  uint8_t sig[8] = {};
  const bool ok = std::fread(sig, 1, sizeof(sig), fp) == sizeof(sig) && sig[0] == 0x89 && sig[1] == 'P' &&
                  sig[2] == 'N' && sig[3] == 'G' && sig[4] == 0x0D && sig[5] == 0x0A && sig[6] == 0x1A &&
                  sig[7] == 0x0A;
  std::fclose(fp);
  return ok;
}

bool assets_sd_png_usable(const char *relative_path) {
  if (!sd_storage_is_mounted() || relative_path == nullptr) {
    return false;
  }

  char full[128];
  join_mount_path(full, sizeof(full), relative_path);

  struct stat st = {};
  if (stat(full, &st) != 0 || !S_ISREG(st.st_mode)) {
    return false;
  }

  const size_t expected = assets_seed_expected_len(relative_path);
  if (expected > 0 && static_cast<size_t>(st.st_size) != expected) {
    ESP_LOGW(TAG, "SD PNG size mismatch for %s (%ld vs %u)", relative_path, st.st_size,
             static_cast<unsigned>(expected));
    return false;
  }

  if (!is_png_file(full)) {
    ESP_LOGW(TAG, "SD PNG header invalid: %s", relative_path);
    return false;
  }
  return true;
}

bool image_src_decodable(const void *src) {
  if (src == nullptr) {
    return false;
  }
  lv_image_header_t header = {};
  return lv_image_decoder_get_info(src, &header) == LV_RESULT_OK && header.w > 0 && header.h > 0;
}

bool apply_image_src(lv_obj_t *img, const void *src, const char *relative_path, const char *source_label) {
  lv_image_set_src(img, src);
  if (!image_src_decodable(lv_image_get_src(img))) {
    ESP_LOGW(TAG, "%s image not decodable: %s", source_label, relative_path);
    return false;
  }
  ESP_LOGD(TAG, "Image from %s: %s", source_label, relative_path);
  return true;
}
}  // namespace

const char *assets_lv_path(const char *relative_path) {
  static char path[96];
  std::snprintf(path, sizeof(path), "%s%s", kLvDrivePrefix, relative_path);
  return path;
}

bool assets_file_exists(const char *relative_path) {
  if (!sd_storage_is_mounted() || relative_path == nullptr) {
    return false;
  }
  char full[128];
  join_mount_path(full, sizeof(full), relative_path);
  FILE *fp = std::fopen(full, "rb");
  if (fp == nullptr) {
    return false;
  }
  std::fclose(fp);
  return true;
}

bool assets_load_text_file(const char *relative_path, char *out, size_t out_size) {
  if (out == nullptr || out_size == 0) {
    return false;
  }
  out[0] = '\0';
  if (!assets_file_exists(relative_path)) {
    return false;
  }

  char full[128];
  join_mount_path(full, sizeof(full), relative_path);
  FILE *fp = std::fopen(full, "rb");
  if (fp == nullptr) {
    return false;
  }

  size_t n = std::fread(out, 1, out_size - 1, fp);
  std::fclose(fp);
  out[n] = '\0';

  while (n > 0 && (out[n - 1] == '\n' || out[n - 1] == '\r')) {
    out[--n] = '\0';
  }
  return n > 0;
}

bool assets_load_lyrics(char *title, size_t title_size, char *body, size_t body_size) {
  bool ok = false;
  if (title != nullptr && title_size > 0) {
    if (assets_load_text_file(SD_LYRICS_META, title, title_size)) {
      ok = true;
    } else {
      title[0] = '\0';
    }
  }
  if (body != nullptr && body_size > 0) {
    if (assets_load_text_file(SD_LYRICS_BODY, body, body_size)) {
      ok = true;
    } else {
      body[0] = '\0';
    }
  }
  if (ok) {
    ESP_LOGI(TAG, "Lyrics loaded from SD");
  }
  return ok;
}

bool assets_set_image_src(lv_obj_t *img, const char *relative_path) {
  if (img == nullptr || relative_path == nullptr) {
    return false;
  }

  const lv_image_dsc_t *embedded = ui_embed_lookup(relative_path);

  if (sd_storage_is_mounted() && assets_sd_png_usable(relative_path)) {
    if (apply_image_src(img, assets_lv_path(relative_path), relative_path, "SD")) {
      return true;
    }
  }

  if (embedded != nullptr) {
    if (apply_image_src(img, embedded, relative_path, "embed")) {
      if (sd_storage_is_mounted() && assets_file_exists(relative_path)) {
        ESP_LOGW(TAG, "Fell back to embedded image: %s", relative_path);
      }
      return true;
    }
  }

  ESP_LOGW(TAG, "No usable image source: %s", relative_path);
  return false;
}
