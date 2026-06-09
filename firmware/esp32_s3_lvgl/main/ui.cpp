#include "ui.h"

#include "app_shell.h"
#include "display.h"
#include "modules/music_module.h"
#include <lvgl.h>

esp_err_t ui_init() {
  display_lock();
  const esp_err_t err = app_shell::init(lv_scr_act());
  display_unlock();
  return err;
}

void ui_set_status(const char *text) {
  app_shell::set_status(text);
}

void ui_set_playback_state(const char *state) {
  music_module::set_playback_state(state);
}

void ui_set_song(const char *title, const char *artist) {
  music_module::set_song(title, artist);
}

void ui_set_lyric(const char *line) {
  ui_set_lyric_lines(nullptr, nullptr, line, nullptr, nullptr);
}

void ui_set_lyric_context(const char *prev_line, const char *line, const char *next_line) {
  ui_set_lyric_lines(nullptr, prev_line, line, next_line, nullptr);
}

void ui_set_lyric_lines(const char *prev2_line,
                        const char *prev_line,
                        const char *line,
                        const char *next_line,
                        const char *next2_line) {
  music_module::set_lyric_lines(prev2_line, prev_line, line, next_line, next2_line);
}

void ui_set_playback_progress(int position_ms, int duration_ms) {
  music_module::set_playback_progress(position_ms, duration_ms);
}

void ui_set_cover(const char *track_key, const char *data_b64, int width, int height) {
  (void)track_key;
  (void)data_b64;
  (void)width;
  (void)height;
}

void ui_set_touch_debug(const char *text) {
  app_shell::set_touch_debug(text);
}

void ui_show_touch_point(int x, int y, bool pressed) {
  app_shell::show_touch_point(x, y, pressed);
}

void ui_handle_swipe_release(int dx, int dy) {
  app_shell::handle_swipe_release(dx, dy);
}

void ui_note_touch_press(int x, int y) {
  app_shell::note_touch_press(x, y);
}
