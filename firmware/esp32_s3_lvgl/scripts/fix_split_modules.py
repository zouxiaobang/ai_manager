#!/usr/bin/env python3
from pathlib import Path

MAIN = Path(__file__).resolve().parents[1] / "main"
UI = MAIN / "ui_legacy.cpp"
if not UI.exists():
    UI = MAIN / "ui.cpp"
text = UI.read_text(encoding="utf-8")

# --- pomodoro: lines 51-738 from original (pomodoro vars start at 51 in ui.cpp)
pom_head = '''#include "modules/pomodoro_module.h"

#include <cstdio>
#include <cstring>
#include <string>

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

vars_start = text.index("constexpr uint32_t kPomodoroArcColorWork")
vars_end = text.index("std::string current_lyric_lines", vars_start)
pom_vars = text[vars_start:vars_end]
pom_labels = (
    "lv_obj_t *pomodoro_time_label = nullptr;\n"
    "lv_obj_t *pomodoro_status_label = nullptr;\n"
    "lv_obj_t *pomodoro_face = nullptr;\n\n"
)
start = text.index("void format_mm_ss(int total_sec")
mid = text.index("bool music_page_is_active()", start)
page_start = text.index("void create_pomodoro_page(lv_obj_t *parent)", mid)
page_end = text.index("void create_music_page", page_start)
body = text[start:mid] + text[page_start:page_end]
body = body.replace("UI_BG_COLOR", "ui_common::kBgColor")
body = body.replace("make_plain", "ui_common::make_plain")
body = body.replace("void create_pomodoro_page(lv_obj_t *parent)", "void build_page(lv_obj_t *parent)")
# drop duplicate page styling (tiles styled in app_shell)
import re
body = re.sub(
    r"void style_home_page\(lv_obj_t \*page\) \{.*?\n\}\n\n",
    "",
    body,
    count=1,
    flags=re.DOTALL,
)

pom_tail = '''
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
    pom_head + pom_labels + pom_vars + body + pom_tail, encoding="utf-8"
)

# --- music: assemble clean file
mus_head = '''#include "modules/music_module.h"

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
constexpr int LYRIC_LINE_COUNT = 5;
constexpr int LYRIC_CURRENT_INDEX = 2;
constexpr uint32_t LYRIC_COLOR_CURRENT = 0xFFD54F;
constexpr uint32_t LYRIC_COLOR_OTHER = 0x526D98;
constexpr int kPageSwipeThreshold = 70;
constexpr int kMusicBackSwipeStartMinX = (PANEL_WIDTH * 55) / 100;

lv_obj_t *lyric_labels[LYRIC_LINE_COUNT] = {nullptr};
std::string current_lyric_lines[LYRIC_LINE_COUNT];
bool lyrics_refresh_pending = false;

void on_control_clicked(lv_event_t *event) {
  lv_event_stop_bubbling(event);
  if (app_shell::gesture_suppress_control_click()) {
    app_shell::set_gesture_suppress_control_click(false);
    return;
  }
  lv_indev_t *indev = lv_indev_active();
  if (indev != nullptr) {
    lv_indev_wait_release(indev);
  }
  const char *command = static_cast<const char *>(lv_event_get_user_data(event));
  if (command == nullptr || command[0] == '\0') {
    return;
  }
  ESP_LOGI(TAG, "Music control: %s", command);
  websocket_send_control_command(command);
}

constexpr lv_coord_t kMusicSideBtnWidth = 72;

lv_obj_t *create_side_button(lv_obj_t *parent, const char *symbol, const char *command) {
  lv_obj_t *button = lv_obj_create(parent);
  lv_obj_set_width(button, kMusicSideBtnWidth);
  lv_obj_set_height(button, PANEL_HEIGHT);
  lv_obj_clear_flag(button, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(button, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_set_style_radius(button, 0, 0);
  lv_obj_set_style_pad_all(button, 0, 0);
  lv_obj_set_style_shadow_width(button, 0, 0);
  lv_obj_set_style_border_width(button, 0, 0);
  lv_obj_set_style_bg_color(button, lv_color_hex(ui_common::kBgColor), 0);
  lv_obj_set_style_bg_opa(button, LV_OPA_COVER, 0);
  lv_obj_set_style_bg_color(button, lv_color_hex(0x101010), LV_STATE_PRESSED);
  lv_obj_set_style_bg_opa(button, LV_OPA_COVER, LV_STATE_PRESSED);
  lv_obj_add_event_cb(button, on_control_clicked, LV_EVENT_SHORT_CLICKED, const_cast<char *>(command));

  lv_obj_t *label = lv_label_create(button);
  lv_label_set_text(label, symbol);
  lv_obj_set_style_text_font(label, &lv_font_montserrat_28, 0);
  lv_obj_set_style_text_color(label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_clear_flag(label, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_center(label);
  return button;
}

void apply_lyric_label_style_locked(int index) {
  if (lyric_labels[index] == nullptr) {
    return;
  }
  const bool is_current = index == LYRIC_CURRENT_INDEX;
  lv_obj_set_style_text_font(lyric_labels[index], is_current ? &font_chinese_28 : &font_chinese_20, 0);
  lv_obj_set_style_text_color(
      lyric_labels[index],
      lv_color_hex(is_current ? LYRIC_COLOR_CURRENT : LYRIC_COLOR_OTHER),
      0);
  lv_obj_set_style_text_opa(lyric_labels[index], is_current ? LV_OPA_COVER : LV_OPA_60, 0);
  lv_obj_set_style_text_letter_space(lyric_labels[index], is_current ? 1 : 0, 0);
}

void apply_lyrics_to_labels_locked() {
  for (int i = 0; i < LYRIC_LINE_COUNT; ++i) {
    if (lyric_labels[i] == nullptr) {
      continue;
    }
    apply_lyric_label_style_locked(i);
    const char *text = current_lyric_lines[i].empty() ? " " : current_lyric_lines[i].c_str();
    lv_label_set_text(lyric_labels[i], text);
    lv_obj_update_layout(lyric_labels[i]);
  }
}

void refresh_page_locked() {
  lv_obj_update_layout(lv_obj_get_parent(lyric_labels[0]));
  apply_lyrics_to_labels_locked();
}

void async_refresh_lyrics_cb(void *user_data) {
  (void)user_data;
  display_lock();
  lyrics_refresh_pending = false;
  if (music_module::is_active()) {
    refresh_page_locked();
  }
  display_unlock();
}

void schedule_lyrics_refresh_locked() {
  if (lyrics_refresh_pending) {
    return;
  }
  lyrics_refresh_pending = true;
  lv_async_call(async_refresh_lyrics_cb, nullptr);
}

void build_page(lv_obj_t *parent) {
  ui_common::style_page(parent);

  lv_obj_t *layout = lv_obj_create(parent);
  ui_common::make_plain(layout);
  lv_obj_set_size(layout, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(layout, 0, 0);
  lv_obj_set_flex_flow(layout, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(layout, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_hor(layout, 16, 0);
  lv_obj_set_style_pad_ver(layout, 0, 0);
  lv_obj_set_style_pad_column(layout, 12, 0);

  create_side_button(layout, LV_SYMBOL_PREV, "previous");

  lv_obj_t *content = lv_obj_create(layout);
  lv_obj_set_height(content, PANEL_HEIGHT);
  lv_obj_set_flex_grow(content, 1);
  lv_obj_clear_flag(content, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(content, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_set_style_radius(content, 24, 0);
  lv_obj_set_style_bg_color(content, lv_color_hex(0x2A2A2A), 0);
  lv_obj_set_style_bg_opa(content, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(content, 0, 0);
  lv_obj_set_style_shadow_width(content, 0, 0);
  lv_obj_set_style_pad_all(content, 20, 0);
  lv_obj_add_event_cb(content, on_control_clicked, LV_EVENT_SHORT_CLICKED, const_cast<char *>("toggle"));

  lv_obj_t *lyric_stack = lv_obj_create(content);
  ui_common::make_plain(lyric_stack);
  ui_common::disable_click(lyric_stack);
  lv_obj_set_size(lyric_stack, LV_PCT(100), LV_PCT(100));
  lv_obj_set_flex_flow(lyric_stack, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(lyric_stack, LV_FLEX_ALIGN_SPACE_EVENLY, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(lyric_stack, 4, 0);

  for (int i = 0; i < LYRIC_LINE_COUNT; ++i) {
    lyric_labels[i] = lv_label_create(lyric_stack);
    current_lyric_lines[i] = (i == LYRIC_CURRENT_INDEX) ? "等待歌词" : " ";
    lv_label_set_text(lyric_labels[i], current_lyric_lines[i].c_str());
    lv_obj_set_width(lyric_labels[i], LV_PCT(100));
    lv_label_set_long_mode(lyric_labels[i], LV_LABEL_LONG_MODE_DOTS);
    lv_obj_set_style_text_align(lyric_labels[i], LV_TEXT_ALIGN_CENTER, 0);
    apply_lyric_label_style_locked(i);
    ui_common::disable_click(lyric_labels[i]);
  }

  create_side_button(layout, LV_SYMBOL_NEXT, "next");
}

'''

mus_tail = '''
bool is_active() {
  return app_shell::is_module_active(app_shell::kModuleMusic);
}

bool try_handle_swipe(int dx, int dy, int start_x) {
  if (!is_active()) {
    return false;
  }
  if (LV_ABS(dx) < LV_ABS(dy) || LV_ABS(dx) < kPageSwipeThreshold) {
    return false;
  }
  return dx > kPageSwipeThreshold && start_x >= kMusicBackSwipeStartMinX;
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

(MAIN / "modules" / "music_module.cpp").write_text(mus_head + mus_tail, encoding="utf-8")
print("fixed module sources")
