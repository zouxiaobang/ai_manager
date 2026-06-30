#include "ui_embed_images.h"

#include <cstring>

namespace {
struct Entry {
  const char *path;
  const lv_image_dsc_t *dsc;
};

const Entry kEntries[] = {
    {"assets/tomato.png", &ui_img_tomato},
    {"assets/dock_pomo.png", &ui_img_dock_pomo},
    {"assets/icon_wifi.png", &ui_img_icon_wifi},
    {"assets/icon_lock.png", &ui_img_icon_lock},
    {"assets/icon_eq.png", &ui_img_icon_eq},
    {"assets/deco_diamond.png", &ui_img_deco_diamond},
    {"assets/deco_diamond_blue.png", &ui_img_deco_diamond_blue},
    {"assets/dock_lyrics.png", &ui_img_dock_lyrics},
    {"assets/dock_sleep.png", &ui_img_dock_sleep},
    {"assets/dock_lock.png", &ui_img_dock_lock},
    {"assets/dock_settings.png", &ui_img_dock_settings},
};
}  // namespace

const lv_image_dsc_t *ui_embed_lookup(const char *relative_path) {
  if (relative_path == nullptr) {
    return nullptr;
  }
  for (const auto &e : kEntries) {
    if (std::strcmp(e.path, relative_path) == 0) {
      return e.dsc;
    }
  }
  return nullptr;
}
