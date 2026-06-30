#include "pixel_ui.h"

#include <cstdlib>

#include "panel_config.h"

namespace {
constexpr int kTomatoCols = 10;
constexpr int kTomatoRows = 10;

const uint8_t kTomatoMask[kTomatoRows][kTomatoCols] = {
    {0, 0, 0, 1, 1, 1, 1, 0, 0, 0},
    {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
    {0, 1, 1, 2, 1, 1, 2, 1, 1, 0},
    {0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
    {0, 0, 1, 1, 3, 3, 1, 1, 0, 0},
    {0, 0, 0, 1, 1, 1, 1, 0, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
    {0, 1, 0, 0, 0, 0, 0, 0, 1, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
};

lv_color_t tomato_color(uint8_t id) {
  switch (id) {
    case 1:
      return lv_palette_main(LV_PALETTE_RED);
    case 2:
      return lv_color_hex(0x2d5a1e);
    case 3:
      return lv_palette_main(LV_PALETTE_LIGHT_GREEN);
    default:
      return lv_color_hex(0x000000);
  }
}

lv_color_t star_palette_color(int idx) {
  static const uint32_t kPalette[] = {
      0x7eb8e8,  // light sky blue
      0x5a8aaa,  // muted blue-grey
      0x90caf9,  // pale blue
      0x4fc3f7,  // bright cyan
      0x78909c,  // grey-blue
      0xffca28,  // yellow
      0xffab40,  // orange
  };
  return lv_color_hex(kPalette[idx % (sizeof(kPalette) / sizeof(kPalette[0]))]);
}

void style_star_fill(lv_obj_t *obj, lv_color_t color, lv_opa_t opa) {
  lv_obj_remove_style_all(obj);
  lv_obj_set_style_bg_color(obj, color, 0);
  lv_obj_set_style_bg_opa(obj, opa, 0);
  lv_obj_set_style_radius(obj, 0, 0);
  lv_obj_set_style_border_width(obj, 0, 0);
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
}

void create_star_dot(lv_obj_t *parent, int x, int y, int size, lv_color_t color, lv_opa_t opa) {
  lv_obj_t *dot = lv_obj_create(parent);
  style_star_fill(dot, color, opa);
  lv_obj_set_size(dot, size, size);
  lv_obj_set_pos(dot, x, y);
}

void create_star_plus(lv_obj_t *parent, int x, int y, int arm, int thick, lv_color_t color, lv_opa_t opa) {
  const int span = arm * 2 + thick;
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, span, span);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  lv_obj_t *hbar = lv_obj_create(box);
  style_star_fill(hbar, color, opa);
  lv_obj_set_size(hbar, span, thick);
  lv_obj_set_pos(hbar, 0, (span - thick) / 2);

  lv_obj_t *vbar = lv_obj_create(box);
  style_star_fill(vbar, color, opa);
  lv_obj_set_size(vbar, thick, span);
  lv_obj_set_pos(vbar, (span - thick) / 2, 0);
}

/** Four pixels in a diamond around a hollow center. */
void create_star_diamond(lv_obj_t *parent, int x, int y, int pixel, lv_color_t color, lv_opa_t opa) {
  const int span = pixel * 3;
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, span, span);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  create_star_dot(box, pixel, 0, pixel, color, opa);
  create_star_dot(box, 0, pixel, pixel, color, opa);
  create_star_dot(box, pixel * 2, pixel, pixel, color, opa);
  create_star_dot(box, pixel, pixel * 2, pixel, color, opa);
}

void create_star_twinkle(lv_obj_t *parent, int x, int y, lv_color_t color, lv_opa_t opa) {
  create_star_dot(parent, x, y, 2, color, opa);
  create_star_dot(parent, x + 3, y + 1, 1, color, static_cast<lv_opa_t>(opa / 2));
  create_star_dot(parent, x + 1, y + 4, 1, color, static_cast<lv_opa_t>(opa / 2));
}

constexpr int kLockCols = 10;
constexpr int kLockRows = 12;
const uint8_t kLockMask[kLockRows][kLockCols] = {
    {0, 0, 0, 1, 1, 1, 1, 1, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 0, 1, 0},
    {0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
    {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
    {0, 0, 1, 1, 2, 2, 1, 1, 0, 0},
    {0, 0, 1, 1, 2, 2, 1, 1, 0, 0},
    {0, 0, 1, 1, 1, 2, 1, 1, 0, 0},
    {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
    {0, 0, 1, 1, 1, 1, 1, 1, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
};

constexpr int kWifiCols = 14;
constexpr int kWifiRows = 12;
const uint8_t kWifiMask[kWifiRows][kWifiCols] = {
    {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
    {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
    {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
    {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
    {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
};

lv_color_t status_icon_color() {
  return lv_palette_main(LV_PALETTE_LIGHT_GREEN);
}

lv_obj_t *pixel_create_mono_sprite(lv_obj_t *parent, int x, int y, int pixel, const uint8_t *mask, int cols,
                                   int rows, lv_color_t color) {
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, cols * pixel, rows * pixel);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  for (int r = 0; r < rows; r++) {
    for (int c = 0; c < cols; c++) {
      if (mask[r * cols + c] != 1) {
        continue;
      }
      lv_obj_t *px = lv_obj_create(box);
      style_star_fill(px, color, LV_OPA_COVER);
      lv_obj_set_size(px, pixel, pixel);
      lv_obj_set_pos(px, c * pixel, r * pixel);
    }
  }
  return box;
}
}  // namespace

extern "C" {

void pixel_bg_create_stars(lv_obj_t *parent) {
  const int max_y = PANEL_HEIGHT - 96;
  constexpr int kStarCount = 88;

  for (int i = 0; i < kStarCount; i++) {
    const int roll = rand() % 100;
    const lv_color_t color = star_palette_color(rand() % 5);
    const lv_opa_t opa = static_cast<lv_opa_t>(100 + rand() % 120);

    int x = 4 + rand() % (PANEL_WIDTH - 12);
    int y = 4 + rand() % (max_y - 4);

    if (roll < 55) {
      const int sz = 1 + rand() % 2;
      create_star_dot(parent, x, y, sz, color, opa);
    } else if (roll < 78) {
      const int sz = 2;
      create_star_dot(parent, x, y, sz, color, opa);
    } else if (roll < 90) {
      create_star_twinkle(parent, x, y, color, opa);
    } else if (roll < 98) {
      create_star_diamond(parent, x, y, 2, color, opa);
    } else {
      const int arm = 2;
      const lv_color_t plus_color = star_palette_color(rand() % 4);
      create_star_plus(parent, x, y, arm, 1, plus_color, static_cast<lv_opa_t>(opa / 2));
    }
  }
}

lv_obj_t *pixel_create_tomato_sprite(lv_obj_t *parent, int x, int y, int pixel) {
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, kTomatoCols * pixel, kTomatoRows * pixel);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  for (int r = 0; r < kTomatoRows; r++) {
    for (int c = 0; c < kTomatoCols; c++) {
      const uint8_t v = kTomatoMask[r][c];
      if (v == 0) {
        continue;
      }
      lv_obj_t *px = lv_obj_create(box);
      lv_obj_remove_style_all(px);
      lv_obj_set_size(px, pixel, pixel);
      lv_obj_set_pos(px, c * pixel, r * pixel);
      lv_obj_set_style_bg_color(px, tomato_color(v), 0);
      lv_obj_set_style_bg_opa(px, LV_OPA_COVER, 0);
      lv_obj_set_style_radius(px, 0, 0);
      lv_obj_remove_flag(px, LV_OBJ_FLAG_CLICKABLE);
    }
  }
  return box;
}

lv_obj_t *pixel_create_lock_icon(lv_obj_t *parent, int x, int y, int pixel) {
  return pixel_create_mono_sprite(parent, x, y, pixel, &kLockMask[0][0], kLockCols, kLockRows, status_icon_color());
}

lv_obj_t *pixel_create_wifi_icon(lv_obj_t *parent, int x, int y, int pixel) {
  return pixel_create_mono_sprite(parent, x, y, pixel, &kWifiMask[0][0], kWifiCols, kWifiRows, status_icon_color());
}

lv_obj_t *pixel_create_eq_icon(lv_obj_t *parent, int x, int y, int pixel, lv_color_t color) {
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, 14 * pixel, 10 * pixel);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  const int bars[][2] = {{1, 8}, {5, 5}, {9, 9}};
  for (const auto &bar : bars) {
    lv_obj_t *px = lv_obj_create(box);
    style_star_fill(px, color, LV_OPA_COVER);
    lv_obj_set_size(px, pixel * 2, bar[1] * pixel);
    lv_obj_set_pos(px, bar[0] * pixel, (10 - bar[1]) * pixel);
  }
  return box;
}

lv_obj_t *pixel_create_jagged_border(lv_obj_t *parent, int x, int y, int w, int h, lv_color_t color,
                                     int thickness, int corner_inset) {
  lv_obj_t *box = lv_obj_create(parent);
  lv_obj_remove_style_all(box);
  lv_obj_set_size(box, w, h);
  lv_obj_set_pos(box, x, y);
  lv_obj_set_style_bg_opa(box, LV_OPA_TRANSP, 0);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(box, LV_OBJ_FLAG_SCROLLABLE);

  const int p = thickness > 0 ? thickness : 1;
  const int inset = corner_inset > p ? corner_inset : p * 2;

  auto block = [&](int bx, int by, int bw, int bh) {
    lv_obj_t *px = lv_obj_create(box);
    lv_obj_remove_style_all(px);
    lv_obj_set_size(px, bw, bh);
    lv_obj_set_pos(px, bx, by);
    lv_obj_set_style_bg_color(px, color, 0);
    lv_obj_set_style_bg_opa(px, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(px, 0, 0);
    lv_obj_remove_flag(px, LV_OBJ_FLAG_CLICKABLE);
  };

  block(inset, 0, w - inset * 2, p);
  block(inset, h - p, w - inset * 2, p);
  block(0, inset, p, h - inset * 2);
  block(w - p, inset, p, h - inset * 2);

  for (int i = p; i < inset; i += p) {
    block(i, 0, p, p);
    block(0, i, p, p);
    block(w - i - p, 0, p, p);
    block(w - p, i, p, p);
    block(i, h - p, p, p);
    block(0, h - i - p, p, p);
    block(w - i - p, h - p, p, p);
    block(w - p, h - i - p, p, p);
  }
  return box;
}

void pixel_jagged_border_set_color(lv_obj_t *border, lv_color_t color) {
  if (border == nullptr) {
    return;
  }
  const uint32_t cnt = lv_obj_get_child_count(border);
  for (uint32_t i = 0; i < cnt; i++) {
    lv_obj_t *child = lv_obj_get_child(border, i);
    lv_obj_set_style_bg_color(child, color, 0);
  }
}

lv_obj_t *pixel_create_dock_jagged_border(lv_obj_t *parent, int x, int y, int w, int h, lv_color_t color) {
  return pixel_create_jagged_border(parent, x, y, w, h, color, 2, 4);
}

void pixel_dock_jagged_border_set_color(lv_obj_t *border, lv_color_t color) {
  pixel_jagged_border_set_color(border, color);
}

}  // extern "C"
