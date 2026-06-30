/**
 * Pixel-style home screen helpers (stars, tomato sprite).
 */
#pragma once

#include "lvgl.h"

#ifdef __cplusplus
extern "C" {
#endif

void pixel_bg_create_stars(lv_obj_t *parent);
lv_obj_t *pixel_create_tomato_sprite(lv_obj_t *parent, int x, int y, int pixel);
lv_obj_t *pixel_create_lock_icon(lv_obj_t *parent, int x, int y, int pixel);
lv_obj_t *pixel_create_wifi_icon(lv_obj_t *parent, int x, int y, int pixel);

/** Pixel-art dock frame with stepped (jagged) corners; hidden until shown by caller. */
lv_obj_t *pixel_create_dock_jagged_border(lv_obj_t *parent, int x, int y, int w, int h, lv_color_t color);
void pixel_dock_jagged_border_set_color(lv_obj_t *border, lv_color_t color);

#ifdef __cplusplus
}
#endif