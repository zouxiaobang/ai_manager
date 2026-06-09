#!/usr/bin/env python3
"""One-off splitter: migrate monolithic ui.cpp into modules (run once)."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "main"
UI = MAIN / "ui.cpp"


def main() -> None:
    text = UI.read_text(encoding="utf-8")

    pomodoro_header = '''#include "modules/pomodoro_module.h"

#include <cstdio>
#include <cstring>

#include "display.h"
#include "esp_log.h"
#include "panel_config.h"
#include "ui_common.h"
#include <lvgl.h>

extern "C" {
LV_FONT_DECLARE(font_chinese_20);
LV_FONT_DECLARE(lv_font_montserrat_36);
}

namespace {
constexpr char TAG[] = "pomodoro";
'''

    music_header = '''#include "modules/music_module.h"

#include <string>

#include "app_shell.h"
#include "display.h"
#include "esp_log.h"
#include "panel_config.h"
#include "ui_common.h"
#include "websocket_client.h"
#include <lvgl.h>

extern "C" {
LV_FONT_DECLARE(font_chinese_20);
LV_FONT_DECLARE(font_chinese_28);
LV_FONT_DECLARE(lv_font_montserrat_28);
}

namespace {
constexpr char TAG[] = "music";
'''

    start_p = text.index("constexpr uint32_t kPomodoroArcColorWork")
    end_p = text.index("void create_music_page", start_p)
    pomodoro_body = text[start_p:end_p]
    for old, new in [
        ("UI_BG_COLOR", "ui_common::kBgColor"),
        ("make_plain", "ui_common::make_plain"),
        ("disable_obj_click", "ui_common::disable_click"),
        ("void create_pomodoro_page(lv_obj_t *parent)", "void build_page(lv_obj_t *parent)"),
    ]:
        pomodoro_body = pomodoro_body.replace(old, new)

    pomodoro_footer = '''
esp_err_t create(lv_obj_t *tile) {
  if (tile == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  build_page(tile);
  pomodoro_lv_timer = lv_timer_create(pomodoro_timer_cb, 1000, nullptr);
  lv_timer_pause(pomodoro_lv_timer);
  return ESP_OK;
}

void on_show() {}

void on_hide() {
  if (pomodoro_run_state == PomodoroRunState::kRunning && pomodoro_lv_timer != nullptr) {
    display_lock();
    pomodoro_run_state = PomodoroRunState::kPaused;
    lv_timer_pause(pomodoro_lv_timer);
    update_pomodoro_display_locked();
    display_unlock();
  }
}

}  // namespace pomodoro_module
'''
    (MAIN / "modules" / "pomodoro_module.cpp").write_text(
        pomodoro_header + pomodoro_body + pomodoro_footer, encoding="utf-8"
    )

    # Music: constants + control + lyrics + create_music_page
    start_m = text.index("constexpr int LYRIC_LINE_COUNT")
    end_m = text.index("void format_mm_ss", start_m)
    part1 = text[start_m:end_m]

    start_m2 = text.index("void on_control_clicked")
    end_m2 = text.index("void apply_lyrics_to_labels_locked", start_m2)
    part2 = text[start_m2:end_m2]

    start_m3 = text.index("void apply_lyric_label_style_locked")
    end_m3 = text.index("void disable_obj_click", start_m3)
    part3 = text[start_m3:end_m3]

    start_m4 = text.index("void apply_lyrics_to_labels_locked")
    end_m4 = text.index("bool music_page_is_active", start_m4)
    part4 = text[start_m4:end_m4]

    start_m5 = text.index("void refresh_music_page_locked")
    end_m5 = text.index("void ui_update_page_visibility_locked", start_m5)
    part5 = text[start_m5:end_m5]

    start_m6 = text.index("void create_music_page")
    end_m6 = text.index("}  // namespace\n\nvoid ui_handle_swipe_release", start_m6)
    part6 = text[start_m6:end_m6]

    music_body = part1 + part2 + part3 + part4 + part5 + part6
    for old, new in [
        ("UI_BG_COLOR", "ui_common::kBgColor"),
        ("make_plain", "ui_common::make_plain"),
        ("disable_obj_click", "ui_common::disable_click"),
        ("style_home_page(parent)", "ui_common::style_page(parent)"),
        ("gesture_suppress_control_click", "app_shell::gesture_suppress_control_click()"),
        ("if (gesture_suppress_control_click)", "if (app_shell::gesture_suppress_control_click())"),
        ("gesture_suppress_control_click = false", "app_shell::set_gesture_suppress_control_click(false)"),
        ("gesture_suppress_control_click = true", "app_shell::set_gesture_suppress_control_click(true)"),
        ("bool music_page_is_active()", "bool is_active()"),
        ("music_page_is_active()", "music_module::is_active()"),
        ("void create_music_page(lv_obj_t *parent)", "void build_page(lv_obj_t *parent)"),
    ]:
        music_body = music_body.replace(old, new)

    music_footer = '''
constexpr int kMusicBackSwipeStartMinX = (PANEL_WIDTH * 55) / 100;

bool try_handle_swipe(int dx, int dy, int start_x) {
  if (!music_module::is_active()) {
    return false;
  }
  if (LV_ABS(dx) < LV_ABS(dy) || LV_ABS(dx) < kPageSwipeThreshold) {
    return false;
  }
  if (dx > kPageSwipeThreshold && start_x >= kMusicBackSwipeStartMinX) {
    return true;
  }
  return false;
}

esp_err_t create(lv_obj_t *tile) {
  if (tile == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  build_page(tile);
  return ESP_OK;
}

void on_show() {
  display_lock();
  refresh_page_locked();
  display_unlock();
}

void on_hide() {}

void set_lyric_lines(const char *prev2_line,
                     const char *prev_line,
                     const char *line,
                     const char *next_line,
                     const char *next2_line) {
  const char *incoming[LYRIC_LINE_COUNT] = {
    prev2_line != nullptr ? prev2_line : "",
    prev_line != nullptr ? prev_line : "",
    line != nullptr ? line : "",
    next_line != nullptr ? next_line : "",
    next2_line != nullptr ? next2_line : "",
  };

  display_lock();
  bool changed = false;
  for (int i = 0; i < LYRIC_LINE_COUNT; ++i) {
    const std::string next_text = incoming[i];
    if (current_lyric_lines[i] == next_text) {
      continue;
    }
    current_lyric_lines[i] = next_text;
    changed = true;
  }
  if (changed && is_active()) {
    schedule_lyrics_refresh_locked();
  }
  display_unlock();
}

void set_playback_progress(int position_ms, int duration_ms) {
  (void)position_ms;
  (void)duration_ms;
}

}  // namespace music_module
'''

    # Fix function renames in music_body
    music_body = music_body.replace("refresh_music_page_locked", "refresh_page_locked")
    music_body = music_body.replace("schedule_music_lyrics_refresh_locked", "schedule_lyrics_refresh_locked")
    music_body = music_body.replace("async_refresh_music_lyrics_cb", "async_refresh_lyrics_cb")
    music_body = music_body.replace("if (music_module::is_active())", "if (is_active())")

    (MAIN / "modules" / "music_module.cpp").write_text(
        music_header + music_body + music_footer, encoding="utf-8"
    )
    print("Wrote modules/pomodoro_module.cpp and modules/music_module.cpp")


if __name__ == "__main__":
    main()
