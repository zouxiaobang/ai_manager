#include "assets_loader.h"

#include "panel_config.h"
#include "sd_assets.h"
#include "sd_storage.h"
#include "ui_embed_images.h"

#include <cstdio>
#include <cstring>

#include "esp_log.h"

namespace {
constexpr char TAG[] = "assets";
constexpr char kLvDriveLetter = 'A';
constexpr char kLvDrivePrefix[] = "A:";

const char *join_mount_path(char *buf, size_t buf_size, const char *relative_path) {
  std::snprintf(buf, buf_size, "%s/%s", SD_MOUNT_POINT, relative_path);
  return buf;
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
  if (assets_file_exists(relative_path)) {
    lv_image_set_src(img, assets_lv_path(relative_path));
    return true;
  }
  const lv_image_dsc_t *embedded = ui_embed_lookup(relative_path);
  if (embedded != nullptr) {
    lv_image_set_src(img, embedded);
    return true;
  }
  return false;
}
