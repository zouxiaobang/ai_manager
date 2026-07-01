#include "pixel_ui.h"

#include <cstdlib>
#include <cstring>

#include "panel_config.h"
#include "pomodoro_bar.h"
#include "esp_heap_caps.h"
#include "widgets/canvas/lv_canvas.h"

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
  int steps = corner_inset / p;
  if (steps < 1) {
    steps = 1;
  }
  const int inset = steps * p;

  auto block = [&](int bx, int by, int bw, int bh) {
    if (bw <= 0 || bh <= 0 || bx >= w || by >= h) {
      return;
    }
    lv_obj_t *px = lv_obj_create(box);
    lv_obj_remove_style_all(px);
    lv_obj_set_size(px, bw, bh);
    lv_obj_set_pos(px, bx, by);
    lv_obj_set_style_bg_color(px, color, 0);
    lv_obj_set_style_bg_opa(px, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(px, 0, 0);
    lv_obj_remove_flag(px, LV_OBJ_FLAG_CLICKABLE);
  };

  if (w > inset * 2) {
    block(inset, 0, w - inset * 2, p);
    block(inset, h - p, w - inset * 2, p);
  }
  if (h > inset * 2) {
    block(0, inset, p, h - inset * 2);
    block(w - p, inset, p, h - inset * 2);
  }

  /* Diagonal stair blocks: each corner gets `steps` visible right-angle turns. */
  for (int s = 0; s < steps; s++) {
    const int dx = (steps - 1 - s) * p;
    const int dy = s * p;
    block(dx, dy, p, p);
    block(w - dx - p, dy, p, p);
    block(dx, h - dy - p, p, p);
    block(w - dx - p, h - dy - p, p, p);
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
  return pixel_create_jagged_border(parent, x, y, w, h, color, 3, 9);
}

void pixel_dock_jagged_border_set_color(lv_obj_t *border, lv_color_t color) {
  pixel_jagged_border_set_color(border, color);
}

namespace {
constexpr int kTimeDigitCols = 7;
constexpr int kTimeDigitRows = 11;

/* Jagged block digits: 7 bits per row, stepped corners on 0/2/3/5/6/8/9. */
const uint8_t kTimeGlyphs[12][kTimeDigitRows] = {
    /* 0 */
    {0x08, 0x14, 0x22, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x22, 0x1C},
    /* 1 */
    {0x08, 0x18, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x1C},
    /* 2 */
    {0x08, 0x14, 0x22, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x41, 0x3E},
    /* 3 */
    {0x08, 0x14, 0x22, 0x02, 0x04, 0x08, 0x04, 0x02, 0x41, 0x22, 0x1C},
    /* 4 */
    {0x04, 0x0C, 0x14, 0x24, 0x44, 0x7F, 0x04, 0x04, 0x04, 0x04, 0x04},
    /* 5 */
    {0x3E, 0x40, 0x40, 0x7C, 0x02, 0x02, 0x02, 0x02, 0x42, 0x24, 0x18},
    /* 6 */
    {0x08, 0x10, 0x20, 0x40, 0x7C, 0x42, 0x41, 0x41, 0x41, 0x22, 0x1C},
    /* 7 */
    {0x7F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x10, 0x10, 0x10, 0x10, 0x10},
    /* 8 */
    {0x08, 0x14, 0x22, 0x41, 0x22, 0x1C, 0x22, 0x41, 0x41, 0x22, 0x1C},
    /* 9 */
    {0x08, 0x14, 0x22, 0x41, 0x41, 0x3F, 0x01, 0x02, 0x04, 0x08, 0x10},
    /* : */
    {0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00},
    /* - */
    {0x00, 0x00, 0x00, 0x00, 0x00, 0x1C, 0x00, 0x00, 0x00, 0x00, 0x00},
};

int time_glyph_index(char ch) {
  if (ch >= '0' && ch <= '9') {
    return ch - '0';
  }
  if (ch == ':') {
    return 10;
  }
  if (ch == '-') {
    return 11;
  }
  return 11;
}

constexpr int kTimeCanvasMaxChars = 5;
constexpr int kTimeCanvasMaxScale = POMO_FULL_TIME_SCALE;
constexpr int kTimeCanvasMaxGap = POMO_FULL_TIME_GAP;
constexpr int kTimeCanvasW =
    kTimeCanvasMaxChars * kTimeDigitCols * kTimeCanvasMaxScale + (kTimeCanvasMaxChars - 1) * kTimeCanvasMaxGap;
constexpr int kTimeCanvasH = kTimeDigitRows * kTimeCanvasMaxScale;
constexpr size_t kTimeCanvasBufBytes = static_cast<size_t>(kTimeCanvasW) * kTimeCanvasH * sizeof(lv_color32_t);

static lv_color32_t *s_time_canvas_buf = nullptr;
static char s_time_canvas_last[16] = "";
static int s_time_canvas_last_scale = 0;
static uint32_t s_time_canvas_last_color = 0;

bool ensure_time_canvas_buf() {
  if (s_time_canvas_buf != nullptr) {
    return true;
  }
  s_time_canvas_buf = static_cast<lv_color32_t *>(heap_caps_malloc(kTimeCanvasBufBytes, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
  if (s_time_canvas_buf == nullptr) {
    s_time_canvas_buf = static_cast<lv_color32_t *>(heap_caps_malloc(kTimeCanvasBufBytes, MALLOC_CAP_8BIT));
  }
  return s_time_canvas_buf != nullptr;
}

void canvas_clear_transparent(lv_obj_t *canvas) {
  lv_draw_buf_t *db = lv_canvas_get_draw_buf(canvas);
  if (db == nullptr || db->data == nullptr || db->data_size == 0) {
    return;
  }
  std::memset(db->data, 0, db->data_size);
}

void canvas_fill_block(lv_obj_t *canvas, int x, int y, int bw, int bh, lv_color_t color, lv_opa_t opa) {
  lv_draw_buf_t *db = lv_canvas_get_draw_buf(canvas);
  if (db == nullptr || db->data == nullptr || bw <= 0 || bh <= 0) {
    return;
  }
  const int canvas_w = static_cast<int>(db->header.w);
  const int canvas_h = static_cast<int>(db->header.h);
  const int stride_bytes = static_cast<int>(db->header.stride);
  if (stride_bytes <= 0) {
    return;
  }
  const lv_color32_t px = lv_color_to_32(color, opa);
  auto *base = static_cast<uint8_t *>(db->data);
  const int x_end = x + bw;
  const int y_end = y + bh;
  if (x >= canvas_w || y >= canvas_h) {
    return;
  }
  const int clip_x_end = x_end > canvas_w ? canvas_w : x_end;
  const int clip_y_end = y_end > canvas_h ? canvas_h : y_end;
  for (int row = y; row < clip_y_end; row++) {
    auto *line = reinterpret_cast<lv_color32_t *>(base + row * stride_bytes);
    for (int col = x; col < clip_x_end; col++) {
      line[col] = px;
    }
  }
}

void draw_time_glyph_on_canvas(lv_obj_t *canvas, int x0, int glyph_idx, int scale, lv_color_t color,
                               lv_color_t shadow) {
  if (glyph_idx < 0 || glyph_idx >= 12) {
    return;
  }
  for (int row = 0; row < kTimeDigitRows; row++) {
    const uint8_t bits = kTimeGlyphs[glyph_idx][row];
    for (int col = 0; col < kTimeDigitCols; col++) {
      if ((bits & (1U << (kTimeDigitCols - 1 - col))) == 0) {
        continue;
      }
      const int px = x0 + col * scale;
      const int py = row * scale;
      canvas_fill_block(canvas, px + 1, py + 1, scale, scale, shadow, LV_OPA_COVER);
      canvas_fill_block(canvas, px, py, scale, scale, color, LV_OPA_COVER);
    }
  }
}

int time_canvas_width_for(const char *text, int scale, int gap) {
  if (text == nullptr || scale < 2 || gap < 0) {
    return 0;
  }
  int chars = 0;
  for (const char *p = text; *p != '\0'; p++) {
    chars++;
  }
  if (chars <= 0) {
    return 0;
  }
  return chars * kTimeDigitCols * scale + (chars - 1) * gap;
}
}  // namespace

lv_obj_t *pixel_create_time_row(lv_obj_t *parent) {
  if (!ensure_time_canvas_buf()) {
    return lv_label_create(parent);
  }
  lv_obj_t *canvas = lv_canvas_create(parent);
  lv_canvas_set_buffer(canvas, s_time_canvas_buf, kTimeCanvasW, kTimeCanvasH, LV_COLOR_FORMAT_ARGB8888);
  lv_obj_remove_flag(canvas, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_remove_flag(canvas, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_set_style_bg_opa(canvas, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(canvas, 0, 0);
  lv_obj_set_style_pad_all(canvas, 0, 0);
  lv_obj_set_size(canvas, kTimeCanvasW, kTimeCanvasH);
  lv_obj_set_flex_grow(canvas, 0);
  s_time_canvas_last[0] = '\0';
  s_time_canvas_last_scale = 0;
  s_time_canvas_last_color = 0;
  return canvas;
}

void pixel_time_row_set(lv_obj_t *canvas, const char *text, int scale, lv_color_t color) {
  if (canvas == nullptr || text == nullptr || scale < 2 || scale > kTimeCanvasMaxScale) {
    return;
  }
  if (s_time_canvas_buf == nullptr || lv_canvas_get_draw_buf(canvas) == nullptr) {
    lv_label_set_text(canvas, text);
    return;
  }
  const uint32_t color_key = lv_color_to_u32(color);
  if (std::strcmp(s_time_canvas_last, text) == 0 && s_time_canvas_last_scale == scale &&
      s_time_canvas_last_color == color_key) {
    return;
  }
  std::strncpy(s_time_canvas_last, text, sizeof(s_time_canvas_last) - 1);
  s_time_canvas_last[sizeof(s_time_canvas_last) - 1] = '\0';
  s_time_canvas_last_scale = scale;
  s_time_canvas_last_color = color_key;

  const int draw_w = time_canvas_width_for(text, scale, POMO_FULL_TIME_GAP);
  const int draw_h = kTimeDigitRows * scale;
  if (draw_w <= 0 || draw_h <= 0 || draw_w > kTimeCanvasW || draw_h > kTimeCanvasH) {
    return;
  }

  lv_canvas_set_buffer(canvas, s_time_canvas_buf, draw_w, draw_h, LV_COLOR_FORMAT_ARGB8888);
  lv_obj_set_size(canvas, draw_w, draw_h);

  canvas_clear_transparent(canvas);

  const lv_color_t shadow = lv_color_hex(0x4a7a32);
  int x = 0;
  for (const char *p = text; *p != '\0'; p++) {
    draw_time_glyph_on_canvas(canvas, x, time_glyph_index(*p), scale, color, shadow);
    x += kTimeDigitCols * scale + POMO_FULL_TIME_GAP;
  }
  lv_obj_invalidate(canvas);
}

void pixel_time_row_clear_cache(void) {
  s_time_canvas_last[0] = '\0';
  s_time_canvas_last_scale = 0;
  s_time_canvas_last_color = 0;
}

}  // extern "C"
