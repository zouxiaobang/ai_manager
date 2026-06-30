/**
 * 800x480 像素风番茄钟 — LVGL 9 静态布局（绝对定位，避免 Grid 在实机错位）
 */
#include "ui_home_static_layout.h"

#include "assets_loader.h"
#include "lv_font_cn_gb2312.h"
#include "panel_config.h"
#include "pixel_ui.h"
#include "sd_assets.h"

#include <string.h>

#define UI_W PANEL_WIDTH
#define UI_H PANEL_HEIGHT

#define COL_BG         0x08081a
#define COL_CARD       0x101028
#define COL_CARD_EDGE  0x1e1e3a
#define COL_GREEN      0x8bc34a
#define COL_BLUE       0x29b6f6
#define COL_LYRIC_DIM  0x90a4ae
#define COL_DOCK_BG    0x0c0c20
#define COL_DOCK_EDGE  0x2a2a50

#define UI_CARD_LEFT_X UI_HOME_CARD_SIDE_MARGIN
#define UI_CARD_CORNER_INSET 5
#define UI_CARD_INNER_PAD    (UI_CARD_CORNER_INSET + 1)
#define UI_DOCK_W      (UI_W - UI_HOME_MARGIN * 2)
#define UI_DOCK_SLOT_W (UI_DOCK_W / 5)
#define UI_DOCK_FRAME_W  52
#define UI_DOCK_FRAME_H  64

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

static void make_non_interactive(lv_obj_t *obj) {
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_CLICKABLE);
  strip_scroll(obj);
}

static void clear_obj(lv_obj_t *obj) {
  lv_obj_remove_style_all(obj);
  make_non_interactive(obj);
}

static lv_obj_t *place_img(lv_obj_t *parent, const char *path, lv_coord_t w, lv_coord_t h) {
  lv_obj_t *img = lv_image_create(parent);
  if (!assets_set_image_src(img, path)) {
    lv_obj_delete(img);
    return NULL;
  }
  if (w > 0 && h > 0) {
    lv_obj_set_size(img, w, h);
  }
  lv_obj_remove_flag(img, LV_OBJ_FLAG_CLICKABLE);
  return img;
}

static void style_label(lv_obj_t *lbl, const lv_font_t *font, uint32_t color_hex) {
  lv_obj_set_style_text_font(lbl, font, 0);
  lv_obj_set_style_text_color(lbl, lv_color_hex(color_hex), 0);
}

static lv_obj_t *make_card(lv_obj_t *parent, int x, int y, uint32_t border_color, lv_obj_t **inner_out) {
  lv_obj_t *card = lv_obj_create(parent);
  lv_obj_set_pos(card, x, y);
  lv_obj_set_size(card, UI_HOME_CARD_W, UI_HOME_CARD_H);
  strip_scroll(card);
  lv_obj_set_style_bg_opa(card, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(card, 0, 0);
  lv_obj_set_style_radius(card, 0, 0);
  lv_obj_set_style_pad_all(card, 0, 0);
  lv_obj_set_style_shadow_width(card, 0, 0);
  lv_obj_set_style_layout(card, LV_LAYOUT_NONE, 0);

  lv_obj_t *inner = lv_obj_create(card);
  lv_obj_set_pos(inner, UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  lv_obj_set_size(inner, UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2, UI_HOME_CARD_H - UI_CARD_INNER_PAD * 2);
  strip_scroll(inner);
  lv_obj_set_style_bg_color(inner, lv_color_hex(COL_CARD), 0);
  lv_obj_set_style_bg_opa(inner, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(inner, 0, 0);
  lv_obj_set_style_radius(inner, 0, 0);
  lv_obj_set_style_pad_all(inner, 10, 0);
  lv_obj_set_layout(inner, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(inner, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(inner, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  lv_obj_t *border = pixel_create_jagged_border(card, 0, 0, UI_HOME_CARD_W, UI_HOME_CARD_H,
                                                lv_color_hex(border_color), 1, UI_CARD_CORNER_INSET);
  lv_obj_move_foreground(border);
  make_non_interactive(border);

  if (inner_out != NULL) {
    *inner_out = inner;
  }
  return card;
}

static void add_card_header(lv_obj_t *inner, const char *title, uint32_t color, const char *diamond) {
  const int inner_w = UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2 - 20;
  lv_obj_t *hdr = lv_obj_create(inner);
  lv_obj_set_size(hdr, inner_w, 26);
  strip_scroll(hdr);
  lv_obj_set_style_bg_opa(hdr, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(hdr, 0, 0);
  lv_obj_set_layout(hdr, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(hdr, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(hdr, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  if (place_img(hdr, diamond, 8, 8) == NULL) {
    lv_obj_t *d = lv_obj_create(hdr);
    clear_obj(d);
    lv_obj_set_size(d, 8, 8);
    lv_obj_set_style_bg_color(d, lv_color_hex(color), 0);
    lv_obj_set_style_bg_opa(d, LV_OPA_COVER, 0);
    lv_obj_set_style_transform_rotation(d, 450, 0);
  }

  lv_obj_t *lbl = lv_label_create(hdr);
  lv_label_set_text(lbl, title);
  style_label(lbl, font_cn(), color);

  if (place_img(hdr, diamond, 8, 8) == NULL) {
    lv_obj_t *d = lv_obj_create(hdr);
    clear_obj(d);
    lv_obj_set_size(d, 8, 8);
    lv_obj_set_style_bg_color(d, lv_color_hex(color), 0);
    lv_obj_set_style_bg_opa(d, LV_OPA_COVER, 0);
    lv_obj_set_style_transform_rotation(d, 450, 0);
  }

  lv_obj_t *line = lv_obj_create(inner);
  lv_obj_set_size(line, inner_w - 12, 2);
  lv_obj_set_style_bg_color(line, lv_color_hex(color), 0);
  lv_obj_set_style_bg_opa(line, LV_OPA_50, 0);
  lv_obj_set_style_border_width(line, 0, 0);
  strip_scroll(line);
}

static void build_pomo_card(lv_obj_t *parent, ui_home_widgets_t *out) {
  const int x = UI_CARD_LEFT_X;
  lv_obj_t *inner = NULL;
  lv_obj_t *card = make_card(parent, x, UI_HOME_CARDS_Y, COL_GREEN, &inner);
  if (out) {
    out->card_pomo = card;
  }
  add_card_header(inner, "番茄钟", COL_GREEN, SD_ASSET_DECO_DIAMOND);

  lv_obj_t *body = lv_obj_create(inner);
  lv_obj_set_width(body, UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2 - 20);
  lv_obj_set_flex_grow(body, 1);
  strip_scroll(body);
  lv_obj_set_style_bg_opa(body, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(body, 0, 0);
  lv_obj_set_layout(body, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(body, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(body, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(body, 10, 0);

  lv_obj_t *tomato_wrap = lv_obj_create(body);
  clear_obj(tomato_wrap);
  lv_obj_set_size(tomato_wrap, 96, 96);
  lv_obj_t *tomato_img = place_img(tomato_wrap, SD_ASSET_TOMATO, 88, 88);
  if (tomato_img == NULL) {
    pixel_create_tomato_sprite(tomato_wrap, 8, 8, 5);
  } else {
    lv_obj_center(tomato_img);
  }

  lv_obj_t *time = lv_label_create(body);
  lv_label_set_text(time, "25:00");
  style_label(time, &lv_font_montserrat_28, COL_GREEN);
  if (out) {
    out->lbl_pomo_time = time;
  }

  lv_obj_t *act = lv_label_create(body);
  lv_label_set_text(act, "\xe2\x96\xb6 \xe5\xbc\x80\xe5\xa7\x8b\xe4\xb8\x93\xe6\xb3\xa8 \xe2\x97\x80");
  style_label(act, font_cn(), COL_GREEN);
  if (out) {
    out->lbl_pomo_action = act;
  }

  lv_obj_t *bar = lv_bar_create(inner);
  lv_obj_set_size(bar, UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2 - 32, 8);
  lv_bar_set_range(bar, 0, 100);
  lv_obj_set_style_bg_color(bar, lv_color_hex(0x252545), LV_PART_MAIN);
  lv_obj_set_style_bg_color(bar, lv_color_hex(COL_GREEN), LV_PART_INDICATOR);
  lv_obj_set_style_radius(bar, 2, LV_PART_MAIN);
  lv_obj_set_style_radius(bar, 2, LV_PART_INDICATOR);
  lv_obj_add_flag(bar, LV_OBJ_FLAG_HIDDEN);
  if (out) {
    out->bar_pomo = bar;
  }
}

static void build_lyric_card(lv_obj_t *parent, ui_home_widgets_t *out) {
  const int x = UI_CARD_LEFT_X + UI_HOME_CARD_W + UI_HOME_CARD_GAP;
  lv_obj_t *inner = NULL;
  lv_obj_t *card = make_card(parent, x, UI_HOME_CARDS_Y, COL_BLUE, &inner);
  add_card_header(inner, "歌词", COL_BLUE, SD_ASSET_DECO_DIAMOND_BLUE);

  lv_obj_t *title_row = lv_obj_create(inner);
  lv_obj_set_size(title_row, UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2 - 20, 24);
  strip_scroll(title_row);
  lv_obj_set_style_bg_opa(title_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(title_row, 0, 0);
  lv_obj_set_layout(title_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(title_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(title_row, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_column(title_row, 6, 0);

  if (place_img(title_row, SD_ASSET_ICON_EQ, 16, 14) == NULL) {
    pixel_create_eq_icon(title_row, 0, 0, 1, lv_color_hex(COL_BLUE));
  }

  lv_obj_t *title = lv_label_create(title_row);
  lv_label_set_text(title, "\xe5\xa4\x9c\xe7\xa9\xba\xe4\xb8\xad\xe6\x9c\x80\xe4\xba\xae\xe7\x9a\x84\xe6\x98\x9f");
  style_label(title, font_cn(), COL_BLUE);
  if (out) {
    out->lbl_lyric_title = title;
  }

  lv_obj_t *body = lv_label_create(inner);
  lv_label_set_text(body,
                    "\xe8\x83\xbd\xe5\x90\xac\xe6\xb8\x85\n"
                    "\xe9\x82\xa3\xe4\xbb\xb0\xe6\x9c\x9b\xe7\x9a\x84\xe4\xba\xba\n"
                    "\xe5\xbf\x83\xe5\xba\x95\xe7\x9a\x84\xe5\xad\xa4\xe7\x8b\xac\xe5\x92\x8c\xe5\x8f\xb9\xe6\x81\xaf\n"
                    "\xe5\xa4\x9c\xe7\xa9\xba\xe4\xb8\xad\xe6\x9c\x80\xe4\xba\xae\xe7\x9a\x84\xe6\x98\x9f\n"
                    "\xe8\x83\xbd\xe5\x90\xa6\xe8\xae\xb0\xe8\xb5\xb7\n"
                    "\xe6\x9b\xbe\xe4\xb8\x8e\xe6\x88\x91\xe5\x90\x8c\xe8\xa1\x8c\n"
                    "\xe6\xb6\x88\xe5\xa4\xb1\xe5\x9c\xa8\xe9\xa3\x8e\xe9\x87\x8c\xe7\x9a\x84\xe8\xba\xab\xe5\xbd\xb1\n"
                    "...");
  style_label(body, font_cn(), COL_LYRIC_DIM);
  lv_obj_set_width(body, UI_HOME_CARD_W - UI_CARD_INNER_PAD * 2 - 28);
  lv_label_set_long_mode(body, LV_LABEL_LONG_WRAP);
  lv_obj_set_style_text_align(body, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_set_style_text_line_space(body, 6, 0);
  if (out) {
    out->lbl_lyric_body = body;
  }
}

static void build_dock_item(lv_obj_t *dock, int index, ui_home_widgets_t *out) {
  const dock_def_t *def = &k_dock[index];
  const int slot_x = index * UI_DOCK_SLOT_W;

  lv_obj_t *slot = lv_obj_create(dock);
  clear_obj(slot);
  lv_obj_set_pos(slot, slot_x, 0);
  lv_obj_set_size(slot, UI_DOCK_SLOT_W, UI_HOME_DOCK_H);
  lv_obj_set_style_bg_opa(slot, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(slot, 0, 0);
  lv_obj_set_style_pad_all(slot, 0, 0);
  if (out) {
    out->dock_slots[index] = slot;
  }

  const int frame_x = (UI_DOCK_SLOT_W - UI_DOCK_FRAME_W) / 2;
  const int frame_y = (UI_HOME_DOCK_H - UI_DOCK_FRAME_H) / 2;

  lv_obj_t *border = pixel_create_dock_jagged_border(slot, frame_x, frame_y, UI_DOCK_FRAME_W, UI_DOCK_FRAME_H,
                                                   lv_color_hex(def->color));
  lv_obj_add_flag(border, LV_OBJ_FLAG_HIDDEN);
  if (out) {
    out->dock_borders[index] = border;
  }

  lv_obj_t *frame = lv_obj_create(slot);
  clear_obj(frame);
  lv_obj_set_size(frame, UI_DOCK_FRAME_W, UI_DOCK_FRAME_H);
  lv_obj_set_pos(frame, frame_x, frame_y);
  lv_obj_set_layout(frame, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(frame, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(frame, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(frame, 2, 0);
  lv_obj_set_style_pad_all(frame, 0, 0);

  lv_obj_t *icon_area = lv_obj_create(frame);
  clear_obj(icon_area);
  lv_obj_set_size(icon_area, 32, 26);
  lv_obj_t *icon = place_img(icon_area, def->icon, 26, 26);
  if (icon != NULL) {
    lv_obj_center(icon);
  } else if (index == 0) {
    lv_obj_t *fallback = place_img(icon_area, SD_ASSET_TOMATO, 24, 24);
    if (fallback != NULL) {
      lv_obj_center(fallback);
    } else {
      pixel_create_tomato_sprite(icon_area, 2, 0, 2);
    }
  }

  lv_obj_t *lbl = lv_label_create(frame);
  lv_label_set_text(lbl, def->label);
  style_label(lbl, font_cn(), def->color);
  lv_obj_set_style_text_align(lbl, LV_TEXT_ALIGN_CENTER, 0);
  make_non_interactive(lbl);
}

static lv_obj_t *build_status_bar(lv_obj_t *parent, ui_home_widgets_t *out) {
  lv_obj_t *status = lv_obj_create(parent);
  lv_obj_set_pos(status, UI_HOME_MARGIN, UI_HOME_STATUS_Y);
  lv_obj_set_size(status, UI_W - UI_HOME_MARGIN * 2, UI_HOME_STATUS_H);
  strip_scroll(status);
  lv_obj_set_style_bg_opa(status, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(status, 0, 0);
  lv_obj_set_layout(status, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(status, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(status, LV_FLEX_ALIGN_SPACE_BETWEEN, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  lv_obj_t *time_lbl = lv_label_create(status);
  lv_label_set_text(time_lbl, "00:00");
  style_label(time_lbl, &lv_font_montserrat_28, 0xffffff);
  if (out) {
    out->lbl_status_time = time_lbl;
  }

  lv_obj_t *icons = lv_obj_create(status);
  clear_obj(icons);
  lv_obj_set_size(icons, 72, UI_HOME_STATUS_H);
  lv_obj_set_layout(icons, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(icons, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(icons, LV_FLEX_ALIGN_END, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_column(icons, 10, 0);

  if (place_img(icons, SD_ASSET_ICON_LOCK, 18, 22) == NULL) {
    lv_obj_t *l = pixel_create_lock_icon(icons, 0, 0, 2);
    (void)l;
  }
  if (place_img(icons, SD_ASSET_ICON_WIFI, 24, 20) == NULL) {
    lv_obj_t *w = pixel_create_wifi_icon(icons, 0, 0, 2);
    (void)w;
  }

  return status;
}

lv_obj_t *ui_home_static_build(lv_obj_t *parent, ui_home_widgets_t *out) {
  if (out) {
    memset(out, 0, sizeof(*out));
  }

  lv_obj_set_size(parent, UI_W, UI_H);
  lv_obj_set_style_bg_color(parent, lv_color_hex(COL_BG), 0);
  lv_obj_set_style_bg_opa(parent, LV_OPA_COVER, 0);
  strip_scroll(parent);
  lv_obj_set_style_pad_all(parent, 0, 0);
  lv_obj_set_style_border_width(parent, 0, 0);

  pixel_bg_create_stars(parent);

  build_status_bar(parent, out);
  build_pomo_card(parent, out);
  build_lyric_card(parent, out);

  lv_obj_t *dots = lv_obj_create(parent);
  lv_obj_set_pos(dots, (UI_W - 80) / 2, UI_HOME_DOTS_Y);
  lv_obj_set_size(dots, 80, 8);
  strip_scroll(dots);
  make_non_interactive(dots);
  lv_obj_set_style_bg_opa(dots, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(dots, 0, 0);
  lv_obj_set_style_pad_all(dots, 0, 0);
  lv_obj_set_layout(dots, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(dots, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(dots, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_column(dots, 10, 0);

  for (int i = 0; i < 4; i++) {
    lv_obj_t *dot = lv_obj_create(dots);
    clear_obj(dot);
    if (i == 0) {
      lv_obj_set_size(dot, 20, 4);
      lv_obj_set_style_radius(dot, 2, 0);
      lv_obj_set_style_bg_color(dot, lv_color_hex(COL_GREEN), 0);
    } else {
      lv_obj_set_size(dot, 8, 8);
      lv_obj_set_style_radius(dot, LV_RADIUS_CIRCLE, 0);
      lv_obj_set_style_bg_color(dot, lv_color_hex(0x404060), 0);
    }
    lv_obj_set_style_bg_opa(dot, LV_OPA_COVER, 0);
    if (out) {
      out->dock_dots[i] = dot;
    }
  }

  lv_obj_t *dock = lv_obj_create(parent);
  lv_obj_set_pos(dock, UI_HOME_MARGIN, UI_HOME_DOCK_Y);
  lv_obj_set_size(dock, UI_DOCK_W, UI_HOME_DOCK_H);
  lv_obj_set_style_bg_color(dock, lv_color_hex(COL_DOCK_BG), 0);
  lv_obj_set_style_bg_opa(dock, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(dock, 2, 0);
  lv_obj_set_style_border_color(dock, lv_color_hex(COL_DOCK_EDGE), 0);
  lv_obj_set_style_radius(dock, 10, 0);
  lv_obj_set_style_pad_all(dock, 0, 0);
  strip_scroll(dock);
  make_non_interactive(dock);
  lv_obj_set_style_layout(dock, LV_LAYOUT_NONE, 0);

  for (int i = 0; i < 5; i++) {
    build_dock_item(dock, i, out);
  }

  return parent;
}
