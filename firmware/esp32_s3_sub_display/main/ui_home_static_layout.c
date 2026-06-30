/**
 * 800x480 像素风番茄钟 — LVGL 9 静态布局
 *
 * 布局树（Flex + Grid）:
 *   screen
 *   ├─ stars (pixel 背景)
 *   ├─ root [GRID]  行: status | cards | dock | dots
 *   │   ├─ status_bar
 *   │   ├─ cards_row [FLEX row]  番茄钟卡片 | 歌词卡片
 *   │   ├─ dock_bar  [FLEX row]  5×(图标+标题)
 *   │   └─ dots_row  [FLEX row]  分页指示点
 */
#include "ui_home_static_layout.h"

#include "assets_loader.h"
#include "lv_font_cn_gb2312.h"
#include "panel_config.h"
#include "pixel_ui.h"
#include "sd_assets.h"

#include <string.h>

/* ---- 尺寸与配色（对齐设计稿） ---- */
#define UI_W           PANEL_WIDTH
#define UI_H           PANEL_HEIGHT
#define UI_MARGIN      16
#define UI_CARD_W      310
#define UI_CARD_GAP    20
#define UI_CARD_H      318
#define UI_DOCK_H      72
#define UI_STATUS_H    36
#define UI_DOTS_H      12

#define COL_BG         0x0a0a18
#define COL_CARD       0x12122a
#define COL_GREEN      0x8bc34a
#define COL_BLUE       0x29b6f6
#define COL_LYRIC_DIM  0x78909c
#define COL_DOCK_BG    0x0e0e1e
#define COL_DOCK_EDGE  0x2a2a48

typedef struct {
  uint32_t color;
  const char *label;
  const char *icon;
} dock_def_t;

static const dock_def_t k_dock[] = {
    {COL_GREEN, "番茄钟", SD_ASSET_DOCK_POMO},
    {COL_BLUE, "歌词", SD_ASSET_DOCK_LYRICS},
    {0xff9800, "息屏", SD_ASSET_DOCK_SLEEP},
    {0x42a5f5, "锁屏", SD_ASSET_DOCK_LOCK},
    {0x42a5f5, "设置", SD_ASSET_DOCK_SETTINGS},
};

static const lv_font_t *font_cn(void) {
  return &lv_font_cn_gb2312_16_0;
}

static void strip_scroll(lv_obj_t *obj) {
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
}

static lv_obj_t *place_img(lv_obj_t *parent, const char *path, lv_align_t align, lv_coord_t x, lv_coord_t y) {
  lv_obj_t *img = lv_image_create(parent);
  if (!assets_set_image_src(img, path)) {
    lv_obj_delete(img);
    return NULL;
  }
  lv_obj_align(img, align, x, y);
  lv_obj_remove_flag(img, LV_OBJ_FLAG_CLICKABLE);
  return img;
}

static void style_label(lv_obj_t *lbl, const lv_font_t *font, uint32_t color_hex) {
  lv_obj_set_style_text_font(lbl, font, 0);
  lv_obj_set_style_text_color(lbl, lv_color_hex(color_hex), 0);
}

static lv_obj_t *make_card(lv_obj_t *parent, uint32_t border_color) {
  lv_obj_t *card = lv_obj_create(parent);
  lv_obj_set_size(card, UI_CARD_W, UI_CARD_H);
  strip_scroll(card);
  lv_obj_set_style_bg_color(card, lv_color_hex(COL_CARD), 0);
  lv_obj_set_style_bg_opa(card, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(card, 2, 0);
  lv_obj_set_style_border_color(card, lv_color_hex(border_color), 0);
  lv_obj_set_style_radius(card, 8, 0);
  lv_obj_set_style_pad_all(card, 12, 0);
  lv_obj_set_layout(card, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(card, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(card, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  return card;
}

static void add_card_header(lv_obj_t *card, const char *title, uint32_t color, const char *diamond) {
  lv_obj_t *hdr = lv_obj_create(card);
  lv_obj_set_size(hdr, UI_CARD_W - 24, 24);
  strip_scroll(hdr);
  lv_obj_set_style_bg_opa(hdr, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(hdr, 0, 0);
  lv_obj_set_layout(hdr, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(hdr, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(hdr, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  if (place_img(hdr, diamond, LV_ALIGN_LEFT_MID, 0, 0) == NULL) {
    lv_obj_t *d = lv_obj_create(hdr);
    lv_obj_remove_style_all(d);
    lv_obj_set_size(d, 8, 8);
    lv_obj_set_style_bg_color(d, lv_color_hex(color), 0);
    lv_obj_set_style_bg_opa(d, LV_OPA_COVER, 0);
    lv_obj_set_style_transform_rotation(d, 450, 0);
  }

  lv_obj_t *lbl = lv_label_create(hdr);
  lv_label_set_text(lbl, title);
  style_label(lbl, font_cn(), color);

  if (place_img(hdr, diamond, LV_ALIGN_RIGHT_MID, 0, 0) == NULL) {
    lv_obj_t *d = lv_obj_create(hdr);
    lv_obj_remove_style_all(d);
    lv_obj_set_size(d, 8, 8);
    lv_obj_set_style_bg_color(d, lv_color_hex(color), 0);
    lv_obj_set_style_bg_opa(d, LV_OPA_COVER, 0);
    lv_obj_set_style_transform_rotation(d, 450, 0);
  }

  lv_obj_t *line = lv_obj_create(card);
  lv_obj_set_size(line, UI_CARD_W - 36, 2);
  lv_obj_set_style_bg_color(line, lv_color_hex(color), 0);
  lv_obj_set_style_bg_opa(line, LV_OPA_60, 0);
  lv_obj_set_style_border_width(line, 0, 0);
  lv_obj_set_style_radius(line, 0, 0);
  strip_scroll(line);
}

static lv_obj_t *build_pomo_card(lv_obj_t *parent, ui_home_widgets_t *out) {
  lv_obj_t *card = make_card(parent, COL_GREEN);
  if (out) {
    out->card_pomo = card;
  }
  add_card_header(card, "番茄钟", COL_GREEN, SD_ASSET_DECO_DIAMOND);

  lv_obj_t *body = lv_obj_create(card);
  lv_obj_set_width(body, UI_CARD_W - 24);
  lv_obj_set_flex_grow(body, 1);
  strip_scroll(body);
  lv_obj_set_style_bg_opa(body, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(body, 0, 0);
  lv_obj_set_layout(body, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(body, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(body, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(body, 8, 0);

  if (place_img(body, SD_ASSET_TOMATO, LV_ALIGN_CENTER, 0, 0) == NULL) {
    pixel_create_tomato_sprite(body, 0, 0, 4);
  }

  lv_obj_t *time = lv_label_create(body);
  lv_label_set_text(time, "25:00");
  style_label(time, &lv_font_montserrat_28, COL_GREEN);
  if (out) {
    out->lbl_pomo_time = time;
  }

  lv_obj_t *act = lv_label_create(body);
  lv_label_set_text(act, "\xe2\x96\xb6 \xe5\xbc\x80\xe5\xa7\x8b\xe4\xb8\x93\xe6\xb3\xa8 \xe2\x97\x80"); /* ▶ 开始专注 ◀ */
  style_label(act, font_cn(), COL_GREEN);
  if (out) {
    out->lbl_pomo_action = act;
  }

  lv_obj_t *bar = lv_bar_create(card);
  lv_obj_set_size(bar, UI_CARD_W - 40, 8);
  lv_bar_set_range(bar, 0, 100);
  lv_obj_set_style_bg_color(bar, lv_color_hex(0x303060), LV_PART_MAIN);
  lv_obj_set_style_bg_color(bar, lv_color_hex(COL_GREEN), LV_PART_INDICATOR);
  lv_obj_add_flag(bar, LV_OBJ_FLAG_HIDDEN);
  if (out) {
    out->bar_pomo = bar;
  }
  return card;
}

static lv_obj_t *build_lyric_card(lv_obj_t *parent, ui_home_widgets_t *out) {
  lv_obj_t *card = make_card(parent, COL_BLUE);
  add_card_header(card, "歌词", COL_BLUE, SD_ASSET_DECO_DIAMOND_BLUE);

  lv_obj_t *title_row = lv_obj_create(card);
  lv_obj_set_size(title_row, UI_CARD_W - 24, 22);
  strip_scroll(title_row);
  lv_obj_set_style_bg_opa(title_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(title_row, 0, 0);
  lv_obj_set_layout(title_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(title_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(title_row, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  place_img(title_row, SD_ASSET_ICON_EQ, LV_ALIGN_LEFT_MID, 0, 0);

  lv_obj_t *title = lv_label_create(title_row);
  lv_label_set_text(title, "\xe5\xa4\x9c\xe7\xa9\xba\xe4\xb8\xad\xe6\x9c\x80\xe4\xba\xae\xe7\x9a\x84\xe6\x98\x9f"); /* 夜空中最亮的星 */
  style_label(title, font_cn(), COL_BLUE);
  if (out) {
    out->lbl_lyric_title = title;
  }

  lv_obj_t *body = lv_label_create(card);
  lv_label_set_text(body,
                    "\xe8\x83\xbd\xe5\x90\xac\xe6\xb8\x85\n"           /* 能否听清 */
                    "\xe9\x82\xa3\xe4\xbb\xb0\xe6\x9c\x9b\xe7\x9a\x84\xe4\xba\xba\n" /* 那仰望的人 */
                    "\xe5\xbf\x83\xe5\xba\x95\xe7\x9a\x84\xe5\xad\xa4\xe7\x8b\xac\xe5\x92\x8c\xe5\x8f\xb9\xe6\x81\xaf\n"
                    "\xe5\xa4\x9c\xe7\xa9\xba\xe4\xb8\xad\xe6\x9c\x80\xe4\xba\xae\xe7\x9a\x84\xe6\x98\x9f\n"
                    "\xe8\x83\xbd\xe5\x90\xa6\xe8\xae\xb0\xe8\xb5\xb7\n"
                    "\xe6\x9b\xbe\xe4\xb8\x8e\xe6\x88\x91\xe5\x90\x8c\xe8\xa1\x8c\n"
                    "\xe6\xb6\x88\xe5\xa4\xb1\xe5\x9c\xa8\xe9\xa3\x8e\xe9\x87\x8c\xe7\x9a\x84\xe8\xba\xab\xe5\xbd\xb1\n"
                    "...");
  style_label(body, font_cn(), COL_LYRIC_DIM);
  lv_obj_set_width(body, UI_CARD_W - 32);
  lv_label_set_long_mode(body, LV_LABEL_LONG_WRAP);
  lv_obj_set_style_text_align(body, LV_TEXT_ALIGN_CENTER, 0);
  if (out) {
    out->lbl_lyric_body = body;
  }
  return card;
}

#define UI_DOCK_FRAME_W  56
#define UI_DOCK_FRAME_H  54

static lv_obj_t *build_dock_item(lv_obj_t *dock, int index, ui_home_widgets_t *out) {
  const dock_def_t *def = &k_dock[index];
  const int slot_w = (UI_W - UI_MARGIN * 2) / 5;

  lv_obj_t *slot = lv_obj_create(dock);
  strip_scroll(slot);
  lv_obj_set_size(slot, slot_w, UI_DOCK_H - 4);
  lv_obj_set_style_bg_opa(slot, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(slot, 0, 0);
  lv_obj_set_style_layout(slot, LV_LAYOUT_NONE, 0);
  if (out) {
    out->dock_slots[index] = slot;
  }

  const int frame_x = (slot_w - UI_DOCK_FRAME_W) / 2;
  lv_obj_t *border = pixel_create_dock_jagged_border(slot, frame_x, 2, UI_DOCK_FRAME_W, UI_DOCK_FRAME_H,
                                                   lv_color_hex(def->color));
  lv_obj_add_flag(border, LV_OBJ_FLAG_HIDDEN);
  if (out) {
    out->dock_borders[index] = border;
  }

  lv_obj_t *frame = lv_obj_create(slot);
  lv_obj_remove_style_all(frame);
  lv_obj_set_size(frame, UI_DOCK_FRAME_W, UI_DOCK_FRAME_H);
  lv_obj_set_pos(frame, frame_x, 2);
  lv_obj_set_style_bg_opa(frame, LV_OPA_TRANSP, 0);
  lv_obj_set_layout(frame, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(frame, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(frame, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_top(frame, 6, 0);
  lv_obj_set_style_pad_row(frame, 2, 0);
  lv_obj_remove_flag(frame, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(frame, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_move_foreground(frame);

  lv_obj_t *icon_area = lv_obj_create(frame);
  lv_obj_remove_style_all(icon_area);
  lv_obj_set_size(icon_area, 28, 26);
  lv_obj_set_style_bg_opa(icon_area, LV_OPA_TRANSP, 0);
  strip_scroll(icon_area);
  if (place_img(icon_area, def->icon, LV_ALIGN_CENTER, 0, 0) == NULL && index == 0) {
    pixel_create_tomato_sprite(icon_area, 4, 2, 2);
  }

  lv_obj_t *lbl = lv_label_create(frame);
  lv_label_set_text(lbl, def->label);
  style_label(lbl, font_cn(), def->color);
  lv_obj_set_style_text_align(lbl, LV_TEXT_ALIGN_CENTER, 0);

  if (index < 4) {
    lv_obj_t *div = lv_obj_create(dock);
    lv_obj_remove_style_all(div);
    lv_obj_set_size(div, 1, UI_DOCK_H - 20);
    lv_obj_set_style_bg_color(div, lv_color_hex(0x404060), 0);
    lv_obj_set_style_bg_opa(div, LV_OPA_40, 0);
    lv_obj_remove_flag(div, LV_OBJ_FLAG_CLICKABLE);
  }
  return slot;
}

lv_obj_t *ui_home_static_build(lv_obj_t *parent, ui_home_widgets_t *out) {
  if (out) {
    memset(out, 0, sizeof(*out));
  }

  lv_obj_set_size(parent, UI_W, UI_H);
  lv_obj_set_style_bg_color(parent, lv_color_hex(COL_BG), 0);
  lv_obj_set_style_bg_opa(parent, LV_OPA_COVER, 0);
  strip_scroll(parent);

  pixel_bg_create_stars(parent);

  /* 根 Grid：状态栏 / 双卡片 / Dock / 分页点 */
  static int32_t col_dsc[] = {LV_GRID_FR(1), LV_GRID_TEMPLATE_LAST};
  static int32_t row_dsc[] = {UI_STATUS_H, UI_CARD_H + 16, UI_DOCK_H + 8, UI_DOTS_H + 8, LV_GRID_TEMPLATE_LAST};

  lv_obj_t *root = lv_obj_create(parent);
  lv_obj_set_size(root, UI_W, UI_H);
  lv_obj_set_pos(root, 0, 0);
  lv_obj_set_style_bg_opa(root, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(root, 0, 0);
  strip_scroll(root);
  lv_obj_set_layout(root, LV_LAYOUT_GRID);
  lv_obj_set_grid_dsc_array(root, col_dsc, row_dsc);
  lv_obj_set_style_pad_row(root, 4, 0);
  lv_obj_set_style_pad_all(root, 0, 0);

  /* 状态栏 */
  lv_obj_t *status = lv_obj_create(root);
  lv_obj_set_height(status, UI_STATUS_H);
  lv_obj_set_width(status, UI_W);
  lv_obj_set_style_bg_opa(status, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(status, 0, 0);
  lv_obj_set_style_pad_hor(status, UI_MARGIN, 0);
  strip_scroll(status);
  lv_obj_set_grid_cell(status, LV_GRID_ALIGN_STRETCH, 0, 1, LV_GRID_ALIGN_CENTER, 0, 1);

  lv_obj_t *time_lbl = lv_label_create(status);
  lv_label_set_text(time_lbl, "00:00");
  style_label(time_lbl, &lv_font_montserrat_28, 0xffffff);
  lv_obj_align(time_lbl, LV_ALIGN_LEFT_MID, 0, 0);
  if (out) {
    out->lbl_status_time = time_lbl;
  }

  if (place_img(status, SD_ASSET_ICON_WIFI, LV_ALIGN_RIGHT_MID, -10, 0) == NULL) {
    lv_obj_t *w = pixel_create_wifi_icon(status, 0, 0, 2);
    lv_obj_align(w, LV_ALIGN_RIGHT_MID, -10, 0);
  }
  if (place_img(status, SD_ASSET_ICON_LOCK, LV_ALIGN_RIGHT_MID, -42, 0) == NULL) {
    lv_obj_t *l = pixel_create_lock_icon(status, 0, 0, 2);
    lv_obj_align(l, LV_ALIGN_RIGHT_MID, -42, 0);
  }

  /* 双卡片行 — Flex 水平居中 */
  lv_obj_t *cards = lv_obj_create(root);
  lv_obj_set_size(cards, UI_W, UI_CARD_H);
  lv_obj_set_style_bg_opa(cards, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(cards, 0, 0);
  strip_scroll(cards);
  lv_obj_set_layout(cards, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(cards, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(cards, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_column(cards, UI_CARD_GAP, 0);
  lv_obj_set_grid_cell(cards, LV_GRID_ALIGN_STRETCH, 0, 1, LV_GRID_ALIGN_CENTER, 1, 1);

  build_pomo_card(cards, out);
  build_lyric_card(cards, out);

  /* Dock */
  lv_obj_t *dock = lv_obj_create(root);
  lv_obj_set_size(dock, UI_W - UI_MARGIN * 2, UI_DOCK_H);
  lv_obj_set_style_bg_color(dock, lv_color_hex(COL_DOCK_BG), 0);
  lv_obj_set_style_bg_opa(dock, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(dock, 2, 0);
  lv_obj_set_style_border_color(dock, lv_color_hex(COL_DOCK_EDGE), 0);
  lv_obj_set_style_radius(dock, 8, 0);
  strip_scroll(dock);
  lv_obj_set_layout(dock, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(dock, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(dock, LV_FLEX_ALIGN_SPACE_EVENLY, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_grid_cell(dock, LV_GRID_ALIGN_CENTER, 0, 1, LV_GRID_ALIGN_CENTER, 2, 1);

  for (int i = 0; i < 5; i++) {
    build_dock_item(dock, i, out);
  }

  if (out) {
    out->dock_sel_bar = lv_obj_create(parent);
    lv_obj_remove_style_all(out->dock_sel_bar);
    const int slot_w = (UI_W - UI_MARGIN * 2) / 5;
    lv_obj_set_size(out->dock_sel_bar, slot_w - 20, 3);
    lv_obj_set_pos(out->dock_sel_bar, UI_MARGIN + 10, UI_H - UI_MARGIN - UI_DOTS_H - 6);
    lv_obj_set_style_bg_color(out->dock_sel_bar, lv_color_hex(COL_GREEN), 0);
    lv_obj_set_style_bg_opa(out->dock_sel_bar, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(out->dock_sel_bar, 1, 0);
    lv_obj_remove_flag(out->dock_sel_bar, LV_OBJ_FLAG_CLICKABLE);
  }

  /* 分页点（4 个，首项为绿色长条） */
  lv_obj_t *dots = lv_obj_create(root);
  lv_obj_set_size(dots, 80, UI_DOTS_H);
  lv_obj_set_style_bg_opa(dots, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(dots, 0, 0);
  strip_scroll(dots);
  lv_obj_set_layout(dots, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(dots, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(dots, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_column(dots, 10, 0);
  lv_obj_set_grid_cell(dots, LV_GRID_ALIGN_CENTER, 0, 1, LV_GRID_ALIGN_CENTER, 3, 1);

  for (int i = 0; i < 4; i++) {
    lv_obj_t *dot = lv_obj_create(dots);
    lv_obj_remove_style_all(dot);
    if (i == 0) {
      lv_obj_set_size(dot, 18, 4);
      lv_obj_set_style_radius(dot, 2, 0);
      lv_obj_set_style_bg_color(dot, lv_color_hex(COL_GREEN), 0);
    } else {
      lv_obj_set_size(dot, 8, 8);
      lv_obj_set_style_radius(dot, LV_RADIUS_CIRCLE, 0);
      lv_obj_set_style_bg_color(dot, lv_color_hex(0x404060), 0);
    }
    lv_obj_set_style_bg_opa(dot, LV_OPA_COVER, 0);
    lv_obj_remove_flag(dot, LV_OBJ_FLAG_CLICKABLE);
    if (out) {
      out->dock_dots[i] = dot;
    }
  }

  return root;
}
