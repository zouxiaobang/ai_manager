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

/** Pixel-art frame; corner_inset should be thickness * step_count (e.g. 3px * 4 steps = 12). */
lv_obj_t *pixel_create_jagged_border(lv_obj_t *parent, int x, int y, int w, int h, lv_color_t color,
                                     int thickness, int corner_inset);
void pixel_jagged_border_set_color(lv_obj_t *border, lv_color_t color);

/** @deprecated Use pixel_create_jagged_border with thickness=3, corner_inset=12 */
lv_obj_t *pixel_create_dock_jagged_border(lv_obj_t *parent, int x, int y, int w, int h, lv_color_t color);
void pixel_dock_jagged_border_set_color(lv_obj_t *border, lv_color_t color);
lv_obj_t *pixel_create_eq_icon(lv_obj_t *parent, int x, int y, int pixel, lv_color_t color);

/** Large pixel-art MM:SS row (digits 0-9, ':', '-'). scale = pixel block size. Uses a single canvas. */
lv_obj_t *pixel_create_time_row(lv_obj_t *parent);
void pixel_time_row_set(lv_obj_t *row, const char *text, int scale, lv_color_t color);
void pixel_time_row_clear_cache(void);

#ifdef __cplusplus
}
#endif