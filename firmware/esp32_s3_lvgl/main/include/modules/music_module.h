#pragma once

#include "esp_err.h"
#include <lvgl.h>

namespace music_module {

esp_err_t create(lv_obj_t *tile);
void on_show();
void on_hide();

bool is_active();

void set_song(const char *title, const char *artist);
void set_playback_state(const char *state);

void set_lyric_lines(const char *prev2_line,
                     const char *prev_line,
                     const char *line,
                     const char *next_line,
                     const char *next2_line);
void set_playback_progress(int position_ms, int duration_ms);

}  // namespace music_module
