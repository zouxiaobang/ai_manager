#include "modules/music_module.h"

#include <algorithm>
#include <cstring>
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
constexpr uint32_t kSongArtistColor = 0xB8B8B8;
constexpr uint32_t kProgressTrackColor = 0x4A4A4A;
constexpr uint32_t kProgressIndicatorColor = 0xFFD54F;
constexpr int kLyricSwipeThreshold = 70;

lv_obj_t *song_title_label = nullptr;
lv_obj_t *song_artist_label = nullptr;
lv_obj_t *pause_icon_label = nullptr;
lv_obj_t *progress_bar = nullptr;
lv_obj_t *lyric_labels[LYRIC_LINE_COUNT] = {nullptr};

std::string song_title_cached = "\xe6\x9c\xaa\xe7\x9f\xa5\xe6\xad\x8c\xe6\x9b\xb2";
std::string song_artist_cached = "\xe6\x9c\xaa\xe7\x9f\xa5\xe6\xad\x8c\xe6\x89\x8b";
std::string current_lyric_lines[LYRIC_LINE_COUNT];
bool playback_is_paused = false;
bool lyrics_refresh_pending = false;

int playback_position_ms = 0;
int playback_duration_ms = 0;
int progress_bar_value_ms = -1;
int progress_bar_range_ms = -1;

lv_point_t lyric_area_press_point = {};
bool lyric_area_press_valid = false;

bool state_is_paused(const char *state) {
  if (state == nullptr || state[0] == '\0') {
    return false;
  }
  return std::strstr(state, "paused") != nullptr || std::strstr(state, "pause") != nullptr
      || std::strstr(state, "stopped") != nullptr || std::strstr(state, "stop") != nullptr;
}

void update_song_header_locked() {
  if (song_title_label != nullptr) {
    lv_label_set_text(song_title_label, song_title_cached.c_str());
  }
  if (song_artist_label != nullptr) {
    lv_label_set_text(song_artist_label, song_artist_cached.c_str());
  }
}

void update_pause_icon_locked() {
  if (pause_icon_label == nullptr) {
    return;
  }
  if (playback_is_paused) {
    lv_obj_clear_flag(pause_icon_label, LV_OBJ_FLAG_HIDDEN);
  } else {
    lv_obj_add_flag(pause_icon_label, LV_OBJ_FLAG_HIDDEN);
  }
}

void update_progress_bar_locked() {
  if (progress_bar == nullptr) {
    return;
  }

  const int range_ms = playback_duration_ms > 0 ? playback_duration_ms : 1;
  const int value_ms =
      playback_duration_ms > 0 ? std::min(std::max(playback_position_ms, 0), playback_duration_ms) : 0;

  if (range_ms == progress_bar_range_ms && value_ms == progress_bar_value_ms) {
    return;
  }

  if (progress_bar_range_ms != range_ms) {
    lv_bar_set_range(progress_bar, 0, range_ms);
    progress_bar_range_ms = range_ms;
  }
  lv_bar_set_value(progress_bar, value_ms, LV_ANIM_OFF);
  progress_bar_value_ms = value_ms;
}

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

void on_lyric_area_gesture(lv_event_t *event) {
  const lv_event_code_t code = lv_event_get_code(event);
  lv_indev_t *indev = lv_indev_active();
  if (indev == nullptr) {
    return;
  }

  if (code == LV_EVENT_PRESSED) {
    lv_indev_get_point(indev, &lyric_area_press_point);
    lyric_area_press_valid = true;
    return;
  }

  if (code != LV_EVENT_RELEASED || !lyric_area_press_valid) {
    return;
  }

  lyric_area_press_valid = false;
  lv_point_t release_point = {};
  lv_indev_get_point(indev, &release_point);
  const int dx = static_cast<int>(release_point.x) - static_cast<int>(lyric_area_press_point.x);
  const int dy = static_cast<int>(release_point.y) - static_cast<int>(lyric_area_press_point.y);

  if (LV_ABS(dx) < LV_ABS(dy) || LV_ABS(dx) < kLyricSwipeThreshold) {
    return;
  }

  app_shell::set_gesture_suppress_control_click(true);

  if (dx > kLyricSwipeThreshold) {
    ESP_LOGI(TAG, "Lyric area swipe right -> pomodoro");
    app_shell::switch_to_module(app_shell::kModulePomodoro);
    return;
  }

  if (dx < -kLyricSwipeThreshold) {
    // reserved
  }
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

void style_progress_bar(lv_obj_t *bar) {
  lv_obj_set_height(bar, 8);
  lv_obj_set_style_pad_all(bar, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar, 4, LV_PART_MAIN);
  lv_obj_set_style_bg_color(bar, lv_color_hex(kProgressTrackColor), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar, LV_OPA_COVER, LV_PART_MAIN);

  lv_obj_set_style_radius(bar, 4, LV_PART_INDICATOR);
  lv_obj_set_style_bg_color(bar, lv_color_hex(kProgressIndicatorColor), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar, LV_OPA_COVER, LV_PART_INDICATOR);
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
    const char *text = current_lyric_lines[i].empty() ? " " : current_lyric_lines[i].c_str();
    const char *shown = lv_label_get_text(lyric_labels[i]);
    if (shown != nullptr && std::strcmp(shown, text) == 0) {
      continue;
    }
    apply_lyric_label_style_locked(i);
    lv_label_set_text(lyric_labels[i], text);
  }
}

void refresh_page_locked() {
  update_song_header_locked();
  update_pause_icon_locked();
  update_progress_bar_locked();
  apply_lyrics_to_labels_locked();
}

void async_refresh_lyrics_cb(void *user_data) {
  (void)user_data;
  display_lock();
  lyrics_refresh_pending = false;
  if (music_module::is_active()) {
    apply_lyrics_to_labels_locked();
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

  lv_obj_t *lyric_box = lv_obj_create(layout);
  lv_obj_set_height(lyric_box, PANEL_HEIGHT);
  lv_obj_set_flex_grow(lyric_box, 1);
  lv_obj_clear_flag(lyric_box, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_clear_flag(lyric_box, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_set_style_radius(lyric_box, 24, 0);
  lv_obj_set_style_bg_color(lyric_box, lv_color_hex(0x2A2A2A), 0);
  lv_obj_set_style_bg_opa(lyric_box, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(lyric_box, 0, 0);
  lv_obj_set_style_shadow_width(lyric_box, 0, 0);
  lv_obj_set_style_pad_all(lyric_box, 20, 0);
  lv_obj_set_flex_flow(lyric_box, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(lyric_box, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_START);

  lv_obj_t *song_header = lv_obj_create(lyric_box);
  ui_common::make_plain(song_header);
  ui_common::disable_click(song_header);
  lv_obj_set_width(song_header, LV_PCT(100));
  lv_obj_set_height(song_header, LV_SIZE_CONTENT);
  lv_obj_set_flex_flow(song_header, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(song_header, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_START);
  lv_obj_set_style_pad_row(song_header, 6, 0);
  lv_obj_set_style_pad_bottom(song_header, 12, 0);

  song_title_label = lv_label_create(song_header);
  lv_label_set_text(song_title_label, song_title_cached.c_str());
  lv_obj_set_width(song_title_label, LV_PCT(100));
  lv_label_set_long_mode(song_title_label, LV_LABEL_LONG_MODE_DOTS);
  lv_obj_set_style_text_font(song_title_label, &font_chinese_28, 0);
  lv_obj_set_style_text_color(song_title_label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_set_style_text_align(song_title_label, LV_TEXT_ALIGN_LEFT, 0);
  ui_common::disable_click(song_title_label);

  song_artist_label = lv_label_create(song_header);
  lv_label_set_text(song_artist_label, song_artist_cached.c_str());
  lv_obj_set_width(song_artist_label, LV_PCT(100));
  lv_label_set_long_mode(song_artist_label, LV_LABEL_LONG_MODE_DOTS);
  lv_obj_set_style_text_font(song_artist_label, &font_chinese_20, 0);
  lv_obj_set_style_text_color(song_artist_label, lv_color_hex(kSongArtistColor), 0);
  lv_obj_set_style_text_align(song_artist_label, LV_TEXT_ALIGN_LEFT, 0);
  ui_common::disable_click(song_artist_label);

  lv_obj_t *lyric_area = lv_obj_create(lyric_box);
  ui_common::make_plain(lyric_area);
  lv_obj_set_width(lyric_area, LV_PCT(100));
  lv_obj_set_flex_grow(lyric_area, 1);
  lv_obj_clear_flag(lyric_area, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(lyric_area, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(lyric_area, on_lyric_area_gesture, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_event_cb(lyric_area, on_lyric_area_gesture, LV_EVENT_RELEASED, nullptr);
  lv_obj_add_event_cb(lyric_area, on_control_clicked, LV_EVENT_SHORT_CLICKED, const_cast<char *>("toggle"));

  lv_obj_t *lyric_stack = lv_obj_create(lyric_area);
  ui_common::make_plain(lyric_stack);
  ui_common::disable_click(lyric_stack);
  lv_obj_set_size(lyric_stack, LV_PCT(100), LV_PCT(100));
  lv_obj_set_flex_flow(lyric_stack, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(lyric_stack, LV_FLEX_ALIGN_SPACE_EVENLY, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(lyric_stack, 4, 0);

  for (int i = 0; i < LYRIC_LINE_COUNT; ++i) {
    lyric_labels[i] = lv_label_create(lyric_stack);
    current_lyric_lines[i] = (i == LYRIC_CURRENT_INDEX) ? "\xe7\xad\x89\xe5\xbe\x85\xe6\xad\x8c\xe8\xaf\x8d" : " ";
    lv_label_set_text(lyric_labels[i], current_lyric_lines[i].c_str());
    lv_obj_set_width(lyric_labels[i], LV_PCT(100));
    lv_label_set_long_mode(lyric_labels[i], LV_LABEL_LONG_MODE_DOTS);
    lv_obj_set_style_text_align(lyric_labels[i], LV_TEXT_ALIGN_CENTER, 0);
    apply_lyric_label_style_locked(i);
    ui_common::disable_click(lyric_labels[i]);
  }

  progress_bar = lv_bar_create(lyric_box);
  lv_obj_set_width(progress_bar, LV_PCT(100));
  lv_obj_set_height(progress_bar, 8);
  lv_bar_set_range(progress_bar, 0, 1);
  lv_bar_set_value(progress_bar, 0, LV_ANIM_OFF);
  style_progress_bar(progress_bar);
  ui_common::disable_click(progress_bar);
  lv_obj_set_style_margin_top(progress_bar, 8, 0);
  lv_obj_set_style_margin_bottom(progress_bar, 4, 0);

  pause_icon_label = lv_label_create(lyric_box);
  lv_label_set_text(pause_icon_label, LV_SYMBOL_PAUSE);
  lv_obj_set_style_text_font(pause_icon_label, &lv_font_montserrat_28, 0);
  lv_obj_set_style_text_color(pause_icon_label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_set_style_text_opa(pause_icon_label, LV_OPA_70, 0);
  ui_common::disable_click(pause_icon_label);
  lv_obj_align(pause_icon_label, LV_ALIGN_BOTTOM_RIGHT, -4, -20);
  lv_obj_add_flag(pause_icon_label, LV_OBJ_FLAG_HIDDEN);

  create_side_button(layout, LV_SYMBOL_NEXT, "next");
}

}  // namespace

namespace music_module {

bool is_active() {
  return app_shell::is_module_active(app_shell::kModuleMusic);
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

void set_song(const char *title, const char *artist) {
  const std::string next_title =
      (title != nullptr && title[0] != '\0') ? title : "\xe6\x9c\xaa\xe7\x9f\xa5\xe6\xad\x8c\xe6\x9b\xb2";
  const std::string next_artist =
      (artist != nullptr && artist[0] != '\0') ? artist : "\xe6\x9c\xaa\xe7\x9f\xa5\xe6\xad\x8c\xe6\x89\x8b";

  display_lock();
  bool changed = song_title_cached != next_title || song_artist_cached != next_artist;
  song_title_cached = next_title;
  song_artist_cached = next_artist;
  if (changed) {
    update_song_header_locked();
  }
  display_unlock();
}

void set_playback_state(const char *state) {
  const bool paused = state_is_paused(state);

  display_lock();
  if (playback_is_paused != paused) {
    playback_is_paused = paused;
    update_pause_icon_locked();
  }
  display_unlock();
}

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
  display_lock();
  bool duration_changed = false;
  if (duration_ms > 0 && duration_ms != playback_duration_ms) {
    playback_duration_ms = duration_ms;
    duration_changed = true;
  }
  if (position_ms >= 0 && position_ms != playback_position_ms) {
    playback_position_ms = position_ms;
  }
  if (duration_changed || position_ms >= 0) {
    update_progress_bar_locked();
  }
  display_unlock();
}

}  // namespace music_module
