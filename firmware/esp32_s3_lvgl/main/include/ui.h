#pragma once

#include "esp_err.h"

esp_err_t ui_init();
void ui_set_status(const char *text);
void ui_set_playback_state(const char *state);
void ui_set_song(const char *title, const char *artist);
void ui_set_lyric(const char *line);
void ui_set_lyric_context(const char *prev_line, const char *line, const char *next_line);
void ui_set_lyric_lines(const char *prev2_line,
                        const char *prev_line,
                        const char *line,
                        const char *next_line,
                        const char *next2_line);
void ui_set_playback_progress(int position_ms, int duration_ms);
void ui_set_cover(const char *track_key, const char *data_b64, int width, int height);
void ui_set_touch_debug(const char *text);
void ui_show_touch_point(int x, int y, bool pressed);
void ui_handle_swipe_release(int dx, int dy);
void ui_note_touch_press(int x, int y);
