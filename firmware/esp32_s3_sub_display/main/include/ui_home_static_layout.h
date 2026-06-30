/**
 * 800x480 像素风番茄钟主界面 — 静态 Flex/Grid 布局（LVGL 9）
 * 仅负责 UI 骨架，业务逻辑在 app_ui.cpp 中绑定。
 */
#pragma once

#include "lvgl.h"
#include "panel_config.h"

#ifdef __cplusplus
extern "C" {
#endif

#define UI_HOME_MARGIN    16
#define UI_HOME_DOCK_H    72
#define UI_HOME_DOTS_H    12
#define UI_HOME_PAGE_DOTS 4
#define UI_HOME_DOCK_Y    (PANEL_HEIGHT - UI_HOME_MARGIN - UI_HOME_DOTS_H - 6 - UI_HOME_DOCK_H)

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
  lv_obj_t *dock_sel_bar;
  lv_obj_t *dock_dots[UI_HOME_PAGE_DOTS];
} ui_home_widgets_t;

/**
 * 在 parent（通常为 screen）上构建完整主界面静态布局。
 * @param parent 父对象，建议 800x480 全屏
 * @param out    输出关键控件指针，可为 NULL
 * @return       内容根容器
 */
lv_obj_t *ui_home_static_build(lv_obj_t *parent, ui_home_widgets_t *out);

#ifdef __cplusplus
}
#endif
