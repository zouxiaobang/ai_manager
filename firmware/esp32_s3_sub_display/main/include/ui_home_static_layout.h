/**
 * 800x480 像素风番茄钟主界面 — 静态布局（LVGL 9）
 */
#pragma once

#include "lvgl.h"
#include "panel_config.h"

#ifdef __cplusplus
extern "C" {
#endif

#define UI_HOME_MARGIN    16
#define UI_HOME_CARD_SIDE_MARGIN  40
#define UI_HOME_STATUS_Y  6
#define UI_HOME_STATUS_H  36
#define UI_HOME_CARDS_Y   44
#define UI_HOME_CARD_GAP  20
#define UI_HOME_CARD_W    ((PANEL_WIDTH - UI_HOME_CARD_SIDE_MARGIN * 2 - UI_HOME_CARD_GAP) / 2)
#define UI_HOME_CARD_H    296
#define UI_HOME_DOTS_Y    348
#define UI_HOME_DOCK_Y    362
#define UI_HOME_DOCK_H    92
#define UI_HOME_PAGE_DOTS 4

typedef struct {
  lv_obj_t *lbl_status_time;
  lv_obj_t *card_pomo;
  lv_obj_t *lbl_pomo_time;
  lv_obj_t *lbl_pomo_action;
  lv_obj_t *bar_pomo;
  lv_obj_t *lbl_lyric_title;
  lv_obj_t *lbl_lyric_body;
  lv_obj_t *dock_slots[5];
  lv_obj_t *dock_borders[5];
  lv_obj_t *dock_dots[UI_HOME_PAGE_DOTS];
} ui_home_widgets_t;

lv_obj_t *ui_home_static_build(lv_obj_t *parent, ui_home_widgets_t *out);

#ifdef __cplusplus
}
#endif
