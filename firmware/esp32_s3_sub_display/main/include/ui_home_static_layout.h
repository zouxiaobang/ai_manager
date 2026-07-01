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
#define UI_HOME_DOCK_Y    362
#define UI_HOME_DOCK_H    92

/** 锯齿边框：线宽 3px，四角各 4 级阶梯 */
#define UI_CARD_BORDER_P        3
#define UI_CARD_CORNER_STEPS    4
#define UI_CARD_CORNER_INSET    (UI_CARD_BORDER_P * UI_CARD_CORNER_STEPS)
#define UI_CARD_INNER_PAD       (UI_CARD_CORNER_INSET + UI_CARD_BORDER_P)
#define UI_DOCK_BORDER_P        UI_CARD_BORDER_P
#define UI_DOCK_CORNER_INSET    UI_CARD_CORNER_INSET
#define UI_DOCK_INNER_PAD       UI_CARD_INNER_PAD
/** Selected dock slot highlight: 3-step jagged corners (vs 4 on cards). */
#define UI_DOCK_SEL_CORNER_STEPS  3
#define UI_DOCK_SEL_CORNER_INSET  (UI_DOCK_BORDER_P * UI_DOCK_SEL_CORNER_STEPS)

#define UI_HOME_POMO_FULL_H (PANEL_HEIGHT - UI_HOME_CARDS_Y - UI_HOME_MARGIN)
#define UI_HOME_POMO_FULL_W (PANEL_WIDTH - UI_HOME_CARD_SIDE_MARGIN * 2)

typedef struct {
  lv_obj_t *lbl_status_time;
  lv_obj_t *card_pomo;
  lv_obj_t *card_pomo_inner;
  lv_obj_t *card_pomo_border;
  lv_obj_t *pomo_body;
  lv_obj_t *lbl_pomo_time;
  lv_obj_t *pomo_time_pixel;
  lv_obj_t *lbl_pomo_action;
  lv_obj_t *bar_pomo;
  lv_obj_t *bar_pomo_wrap;
  lv_obj_t *bar_pomo_border;
  lv_obj_t *card_lyric;
  lv_obj_t *card_lyric_inner;
  lv_obj_t *card_lyric_border;
  lv_obj_t *lbl_lyric_title;
  lv_obj_t *lbl_lyric_body;
  lv_obj_t *dock_panel;
  lv_obj_t *dock_slots[5];
  lv_obj_t *dock_borders[5];
} ui_home_widgets_t;

lv_obj_t *ui_home_static_build(lv_obj_t *parent, ui_home_widgets_t *out);

#ifdef __cplusplus
}
#endif
