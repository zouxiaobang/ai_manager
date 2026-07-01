#pragma once

#include "lvgl.h"

extern const lv_image_dsc_t ui_img_tomato;
extern const lv_image_dsc_t ui_img_dock_pomo;
extern const lv_image_dsc_t ui_img_dock_home;
extern const lv_image_dsc_t ui_img_icon_wifi;
extern const lv_image_dsc_t ui_img_icon_lock;
extern const lv_image_dsc_t ui_img_icon_unlock;
extern const lv_image_dsc_t ui_img_icon_eq;
extern const lv_image_dsc_t ui_img_deco_diamond;
extern const lv_image_dsc_t ui_img_deco_diamond_blue;
extern const lv_image_dsc_t ui_img_dock_lyrics;
extern const lv_image_dsc_t ui_img_dock_lock;
extern const lv_image_dsc_t ui_img_dock_settings;

const lv_image_dsc_t *ui_embed_lookup(const char *relative_path);
