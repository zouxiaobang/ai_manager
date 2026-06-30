#pragma once

#include "lvgl.h"

#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/** LVGL drive letter path, e.g. "A:assets/tomato.png". */
const char *assets_lv_path(const char *relative_path);

bool assets_file_exists(const char *relative_path);
bool assets_load_text_file(const char *relative_path, char *out, size_t out_size);

bool assets_load_lyrics(char *title, size_t title_size, char *body, size_t body_size);

/** Set image src from SD or embedded fallback. */
bool assets_set_image_src(lv_obj_t *img, const char *relative_path);

#ifdef __cplusplus
}
#endif
