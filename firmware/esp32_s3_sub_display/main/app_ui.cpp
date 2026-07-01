#include "app_ui.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "app_clock.h"
#include "app_power.h"
#include "app_settings.h"
#include "assets_loader.h"
#include "display.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "lvgl.h"
#include "lv_font_cn_16.h"
#include "lv_font_cn_gb2312.h"
#include "panel_config.h"
#include "pixel_ui.h"
#include "pomodoro_bar.h"
#include "pomodoro_model.h"
#include "sd_assets.h"
#include "ui_home_static_layout.h"

namespace {
constexpr char TAG[] = "app_ui";
constexpr uint32_t kColBg = 0x0a0a18;
constexpr uint32_t kColCard = 0x12122a;

enum class DockId { Home, Pomodoro, Lyrics, Focus, Settings };
enum class MoreId { Weather, Stats, Notes, Settings, Media };

lv_obj_t *scr_home = nullptr;
lv_obj_t *scr_sleep = nullptr;
lv_obj_t *dim_overlay = nullptr;
lv_obj_t *lock_layer = nullptr;
lv_obj_t *more_layer = nullptr;
lv_obj_t *settings_layer = nullptr;
lv_obj_t *lbl_pomo_time = nullptr;
lv_obj_t *lbl_pomo_action = nullptr;
lv_obj_t *lbl_status_time = nullptr;
lv_obj_t *dock_panel = nullptr;
lv_obj_t *dock_slots[5] = {};
lv_obj_t *dock_borders[5] = {};
lv_obj_t *bar_pomo = nullptr;
lv_obj_t *bar_pomo_wrap = nullptr;
lv_obj_t *bar_pomo_border = nullptr;
lv_obj_t *pomo_time_pixel = nullptr;
lv_obj_t *card_pomo = nullptr;
lv_obj_t *card_pomo_inner = nullptr;
lv_obj_t *card_pomo_border = nullptr;
lv_obj_t *pomo_body = nullptr;
lv_obj_t *card_lyric = nullptr;
lv_obj_t *card_lyric_inner = nullptr;
lv_obj_t *card_lyric_border = nullptr;
lv_obj_t *lyric_exit_btn = nullptr;
lv_obj_t *lyric_tool_row = nullptr;
lv_obj_t *pomo_exit_btn = nullptr;
lv_obj_t *pomo_lock_btn = nullptr;
lv_obj_t *pomo_lock_btn_lbl = nullptr;
lv_obj_t *pomo_tool_row = nullptr;
lv_obj_t *pomo_touch_blocker = nullptr;
lv_obj_t *focus_layer = nullptr;
lv_obj_t *focus_lbl_time = nullptr;
lv_obj_t *focus_lbl_action = nullptr;
lv_obj_t *focus_bar_pomo = nullptr;
lv_obj_t *focus_exit_hint = nullptr;
lv_obj_t *focus_pomo_tap = nullptr;
lv_obj_t *focus_exit_tap = nullptr;
lv_obj_t *lbl_lyric_title = nullptr;
lv_obj_t *lbl_lyric_line = nullptr;
lv_obj_t *lock_lbl_time = nullptr;
lv_obj_t *lock_pomo_box = nullptr;
lv_obj_t *lock_lbl_pomo = nullptr;
lv_obj_t *lock_bar_pomo = nullptr;
lv_obj_t *unlock_hint = nullptr;
lv_obj_t *unlock_pad = nullptr;
lv_obj_t *lbl_settings_summary = nullptr;

bool locked = false;
bool focus_mode = false;
bool pomo_fullscreen_mode = false;
bool lyric_fullscreen_mode = false;
bool pomo_touch_locked = false;
lv_coord_t unlock_press_y = 0;

constexpr int kDockIndexPomodoro = 0;
constexpr int kDockIndexLyrics = 1;
constexpr int kDockIndexHome = 2;
constexpr uint32_t kLyricAccentColor = 0x29b6f6;
constexpr int kFocusMargin = 12;
constexpr int kPomoToolBtnW = 92;
constexpr int kPomoToolBtnH = 52;
constexpr int kPomoToolBtnGap = 10;
constexpr int kPomoToolRowW = kPomoToolBtnW * 2 + kPomoToolBtnGap;
constexpr int kFocusW = PANEL_WIDTH - kFocusMargin * 2;
constexpr int kFocusH = PANEL_HEIGHT - kFocusMargin * 2;

struct DockItem {
  DockId id;
};

const DockItem kDock[] = {
    {DockId::Pomodoro},
    {DockId::Lyrics},
    {DockId::Home},
    {DockId::Focus},
    {DockId::Settings},
};

char lyric_title_buf[128] = "夜空中最亮的星";
char lyric_body_buf[384] =
    "能否听清\n"
    "那仰望的人\n"
    "心底的孤独和叹息\n"
    "夜空中最亮的星\n"
    "能否记起\n"
    "曾与我同行\n"
    "消失在风里的身影\n"
    "...";

struct MoreItem {
  MoreId id;
  const char *label;
};

const MoreItem kMore[] = {
    {MoreId::Weather, "Weather"},
    {MoreId::Stats, "PC Stats"},
    {MoreId::Notes, "Notes"},
    {MoreId::Settings, "Settings"},
    {MoreId::Media, "Media"},
};

void layout_abs(lv_obj_t *obj) {
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_style_layout(obj, LV_LAYOUT_NONE, 0);
}

void style_card(lv_obj_t *obj, uint32_t border_color) {
  lv_obj_set_style_bg_color(obj, lv_color_hex(kColCard), 0);
  lv_obj_set_style_bg_opa(obj, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(obj, 2, 0);
  lv_obj_set_style_border_color(obj, lv_color_hex(border_color), 0);
  lv_obj_set_style_radius(obj, 8, 0);
  lv_obj_set_style_pad_all(obj, 14, 0);
  layout_abs(obj);
}

void style_pixel_label(lv_obj_t *lbl, const lv_font_t *font, lv_color_t color) {
  lv_obj_set_style_text_font(lbl, font, 0);
  lv_obj_set_style_text_color(lbl, color, 0);
}

void refresh_lyrics_card() {
  char title[128];
  char body[384];
  if (assets_load_lyrics(title, sizeof(title), body, sizeof(body))) {
    std::strncpy(lyric_title_buf, title, sizeof(lyric_title_buf) - 1);
    lyric_title_buf[sizeof(lyric_title_buf) - 1] = '\0';
    std::strncpy(lyric_body_buf, body, sizeof(lyric_body_buf) - 1);
    lyric_body_buf[sizeof(lyric_body_buf) - 1] = '\0';
  }
  if (lbl_lyric_title != nullptr) {
    lv_label_set_text(lbl_lyric_title, lyric_title_buf);
  }
  if (lbl_lyric_line != nullptr) {
    lv_label_set_text(lbl_lyric_line, lyric_body_buf);
  }
}

struct PomodoroUiTarget {
  lv_obj_t *time = nullptr;
  lv_obj_t *action = nullptr;
  lv_obj_t *bar = nullptr;
  const lv_font_t *time_font = nullptr;
};

pomo_bar_phase_t pomodoro_bar_phase_from(const PomodoroSnapshot &p) {
  switch (p.phase) {
    case PomodoroPhase::Focus:
      return POMO_BAR_PHASE_FOCUS;
    case PomodoroPhase::ShortBreak:
      return POMO_BAR_PHASE_SHORT;
    case PomodoroPhase::LongBreak:
      return POMO_BAR_PHASE_LONG;
    default:
      return POMO_BAR_PHASE_IDLE;
  }
}

void set_pomo_bar_visible(lv_obj_t *bar, bool visible) {
  if (bar == nullptr) {
    return;
  }
  lv_obj_t *wrap = lv_obj_get_parent(bar);
  const bool home_shell = wrap != nullptr && wrap == bar_pomo_wrap;
  if (visible) {
    lv_obj_remove_flag(bar, LV_OBJ_FLAG_HIDDEN);
    if (home_shell) {
      lv_obj_remove_flag(wrap, LV_OBJ_FLAG_HIDDEN);
    }
  } else {
    lv_obj_add_flag(bar, LV_OBJ_FLAG_HIDDEN);
    if (home_shell) {
      lv_obj_add_flag(wrap, LV_OBJ_FLAG_HIDDEN);
    }
  }
}

void apply_pomo_time_display(const char *time_str, bool fullscreen) {
  if (lbl_pomo_time == nullptr || time_str == nullptr) {
    return;
  }
  const lv_color_t green = lv_color_hex(0x8bc34a);
  if (fullscreen && pomo_time_pixel != nullptr) {
    pixel_time_row_set(pomo_time_pixel, time_str, POMO_FULL_TIME_SCALE, green);
    lv_obj_add_flag(lbl_pomo_time, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(pomo_time_pixel, LV_OBJ_FLAG_HIDDEN);
  } else {
    lv_label_set_text(lbl_pomo_time, time_str);
    lv_obj_remove_flag(lbl_pomo_time, LV_OBJ_FLAG_HIDDEN);
    if (pomo_time_pixel != nullptr) {
      lv_obj_add_flag(pomo_time_pixel, LV_OBJ_FLAG_HIDDEN);
      pixel_time_row_clear_cache();
    }
  }
}

void apply_pomo_bar_layout(bool fullscreen, int inner_w) {
  if (bar_pomo == nullptr || bar_pomo_wrap == nullptr) {
    return;
  }
  const int bw = fullscreen ? (inner_w - 32) : POMO_HOME_BAR_W;
  const int bh = POMO_HOME_BAR_H;
  lv_obj_set_size(bar_pomo_wrap, bw, bh);
  pomodoro_bar_init_horizontal(bar_pomo, bw, bh);
  if (bar_pomo_border != nullptr) {
    lv_obj_delete(bar_pomo_border);
  }
  bar_pomo_border = pixel_create_jagged_border(bar_pomo_wrap, 0, 0, bw, bh, lv_color_hex(0x0A0A18),
                                                POMO_HOME_BAR_BORDER_P, POMO_HOME_BAR_CORNER_INSET);
  lv_obj_move_foreground(bar_pomo_border);
  lv_obj_remove_flag(bar_pomo_border, LV_OBJ_FLAG_CLICKABLE);
}

void fill_pomodoro_ui(const PomodoroUiTarget &ui) {
  if (ui.time == nullptr || ui.action == nullptr) {
    return;
  }
  const PomodoroSnapshot p = pomodoro_get();
  const lv_font_t *time_font = ui.time_font != nullptr ? ui.time_font : &lv_font_montserrat_28;
  char buf[32];

  if (p.today_goal_done) {
    std::snprintf(buf, sizeof(buf), "--:--");
    if (ui.time == lbl_pomo_time) {
      apply_pomo_time_display(buf, pomo_fullscreen_mode);
      lv_obj_set_style_text_font(lbl_pomo_time, time_font, 0);
    } else {
      lv_label_set_text(ui.time, buf);
      lv_obj_set_style_text_font(ui.time, time_font, 0);
    }
    lv_label_set_text(ui.action, "今日专注任务结束");
    if (ui.bar != nullptr) {
      lv_bar_set_value(ui.bar, 100, LV_ANIM_OFF);
      set_pomo_bar_visible(ui.bar, false);
    }
    return;
  }

  const int mm = p.remaining_sec / 60;
  const int ss = p.remaining_sec % 60;
  std::snprintf(buf, sizeof(buf), "%02d:%02d", mm, ss);
  if (ui.time == lbl_pomo_time) {
    apply_pomo_time_display(buf, pomo_fullscreen_mode);
    lv_obj_set_style_text_font(lbl_pomo_time, time_font, 0);
  } else {
    lv_label_set_text(ui.time, buf);
    lv_obj_set_style_text_font(ui.time, time_font, 0);
  }

  const char *state = "▶ 开始专注 ◀";
  if (!p.running && p.pending != PomodoroPendingPhase::None && p.remaining_sec <= 0) {
    if (p.pending == PomodoroPendingPhase::ShortBreak) {
      state = "▶ 开始短休息 ◀";
    } else if (p.pending == PomodoroPendingPhase::LongBreak) {
      state = "▶ 开始长休息 ◀";
    } else {
      state = "▶ 开始专注 ◀";
    }
  } else if (p.pending != PomodoroPendingPhase::None && p.phase == PomodoroPhase::Idle) {
    if (p.pending == PomodoroPendingPhase::ShortBreak) {
      state = "▶ 开始短休息 ◀";
    } else if (p.pending == PomodoroPendingPhase::LongBreak) {
      state = "▶ 开始长休息 ◀";
    } else {
      state = "▶ 开始专注 ◀";
    }
  } else if (p.phase == PomodoroPhase::Focus) {
    state = p.running ? "专注中..." : "专注已暂停";
  } else if (p.phase == PomodoroPhase::ShortBreak) {
    state = p.running ? "短休息中..." : "短休息已暂停";
  } else if (p.phase == PomodoroPhase::LongBreak) {
    state = p.running ? "长休息中..." : "长休息已暂停";
  }
  lv_label_set_text(ui.action, state);

  if (ui.bar == nullptr) {
    return;
  }
  int pct = 0;
  if (p.total_sec > 0) {
    pct = (p.total_sec - p.remaining_sec) * 100 / p.total_sec;
  }
  pomodoro_bar_set_fill_phase(ui.bar, pomodoro_bar_phase_from(p));
  lv_bar_set_value(ui.bar, pct, LV_ANIM_OFF);
  set_pomo_bar_visible(ui.bar, p.phase != PomodoroPhase::Idle);
}

void refresh_pomodoro_card() {
  fill_pomodoro_ui({lbl_pomo_time, lbl_pomo_action, bar_pomo, &lv_font_montserrat_28});
}

void refresh_focus_mode_ui() {
  if (!focus_mode || focus_layer == nullptr) {
    return;
  }
  fill_pomodoro_ui({focus_lbl_time, focus_lbl_action, focus_bar_pomo, &lv_font_montserrat_28});

  const bool can_exit = pomodoro_is_current_task_complete();
  if (focus_exit_hint != nullptr) {
    if (can_exit) {
      lv_obj_remove_flag(focus_exit_hint, LV_OBJ_FLAG_HIDDEN);
      lv_obj_move_foreground(focus_exit_hint);
    } else {
      lv_obj_add_flag(focus_exit_hint, LV_OBJ_FLAG_HIDDEN);
    }
  }
  if (focus_pomo_tap != nullptr) {
    if (can_exit) {
      lv_obj_add_flag(focus_pomo_tap, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_remove_flag(focus_pomo_tap, LV_OBJ_FLAG_HIDDEN);
    }
  }
  if (focus_exit_tap != nullptr) {
    if (can_exit) {
      lv_obj_remove_flag(focus_exit_tap, LV_OBJ_FLAG_HIDDEN);
      lv_obj_move_foreground(focus_exit_tap);
    } else {
      lv_obj_add_flag(focus_exit_tap, LV_OBJ_FLAG_HIDDEN);
    }
  }
}

void enter_focus_mode();
void exit_focus_mode();
void enter_pomodoro_fullscreen();
void exit_pomodoro_fullscreen();
void enter_lyrics_fullscreen();
void exit_lyrics_fullscreen();
void show_more(bool on);
void show_settings(bool on);
void show_home_page();
void set_dock_selected(int index);
void apply_pomo_card_layout(bool fullscreen);
void apply_lyric_card_layout(bool fullscreen);
void layout_pomo_fullscreen_buttons();
void layout_lyric_fullscreen_buttons();
void apply_pomo_bar_layout(bool fullscreen, int inner_w);
void apply_pomo_time_display(const char *time_str, bool fullscreen);
void set_pomo_touch_locked(bool locked);
void update_pomo_tool_row_for_lock_state();
void refresh_pomo_touch_lock_stack();
bool should_auto_unlock_pomo_touch();

void focus_pomo_tap_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  if (pomodoro_is_current_task_complete()) {
    return;
  }
  app_ui_notify_activity();
  if (pomodoro_is_operation_blocked()) {
    return;
  }
  pomodoro_card_action();
  refresh_pomodoro_card();
  refresh_focus_mode_ui();
}

void focus_exit_tap_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  if (!pomodoro_is_current_task_complete()) {
    return;
  }
  app_ui_notify_activity();
  exit_focus_mode();
}

void pomo_exit_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  if (pomo_touch_locked) {
    return;
  }
  app_ui_notify_activity();
  show_home_page();
}

void lyric_exit_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  app_ui_notify_activity();
  show_home_page();
}

void pomo_touch_blocker_event(lv_event_t *e) {
  (void)e;
}

bool should_auto_unlock_pomo_touch() {
  if (pomodoro_is_today_goal_done()) {
    return true;
  }
  const PomodoroSnapshot snap = pomodoro_get();
  if (snap.today_goal_done) {
    return true;
  }
  if (snap.phase == PomodoroPhase::Focus && snap.running) {
    return false;
  }
  if (snap.phase == PomodoroPhase::ShortBreak && snap.running) {
    return false;
  }
  if (snap.phase == PomodoroPhase::LongBreak && snap.running) {
    return false;
  }
  return pomodoro_is_current_task_complete();
}

void update_pomo_lock_btn_label() {
  if (pomo_lock_btn_lbl == nullptr) {
    return;
  }
  if (pomo_touch_locked) {
    lv_label_set_text(pomo_lock_btn_lbl, "\xe8\xa7\xa3\xe9\x94\x81");
  } else {
    lv_label_set_text(pomo_lock_btn_lbl, "\xe9\x94\x81\xe5\xb1\x8f");
  }
}

void update_pomo_tool_row_for_lock_state() {
  if (pomo_tool_row == nullptr || pomo_lock_btn == nullptr || !pomo_fullscreen_mode || scr_home == nullptr ||
      card_pomo == nullptr) {
    return;
  }

  const int card_x = UI_HOME_CARD_SIDE_MARGIN;
  const int card_y = UI_HOME_CARDS_Y;
  const int card_w = UI_HOME_POMO_FULL_W;
  const int btn_y = card_y + UI_CARD_INNER_PAD;
  const int lock_x = card_x + card_w - UI_CARD_INNER_PAD - kPomoToolBtnW;

  if (pomo_touch_locked) {
    if (pomo_exit_btn != nullptr) {
      lv_obj_add_flag(pomo_exit_btn, LV_OBJ_FLAG_HIDDEN);
    }
    lv_obj_add_flag(pomo_tool_row, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(pomo_lock_btn, LV_OBJ_FLAG_HIDDEN);
    if (lv_obj_get_parent(pomo_lock_btn) != scr_home) {
      lv_obj_set_parent(pomo_lock_btn, scr_home);
    }
    lv_obj_set_pos(pomo_lock_btn, lock_x, btn_y);
    lv_obj_set_size(pomo_lock_btn, kPomoToolBtnW, kPomoToolBtnH);
  } else {
    if (pomo_exit_btn != nullptr) {
      lv_obj_remove_flag(pomo_exit_btn, LV_OBJ_FLAG_HIDDEN);
    }
    lv_obj_remove_flag(pomo_lock_btn, LV_OBJ_FLAG_HIDDEN);
    if (lv_obj_get_parent(pomo_lock_btn) != pomo_tool_row) {
      lv_obj_set_parent(pomo_lock_btn, pomo_tool_row);
    }
    lv_obj_remove_flag(pomo_tool_row, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomo_tool_row, kPomoToolRowW, kPomoToolBtnH);
    if (lv_obj_get_parent(pomo_tool_row) != card_pomo) {
      lv_obj_set_parent(pomo_tool_row, card_pomo);
    }
    lv_obj_align(pomo_tool_row, LV_ALIGN_TOP_RIGHT, -UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  }
}

void refresh_pomo_touch_lock_stack() {
  if (!pomo_fullscreen_mode || scr_home == nullptr) {
    return;
  }
  update_pomo_tool_row_for_lock_state();
  if (pomo_touch_locked && pomo_touch_blocker != nullptr) {
    lv_obj_remove_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(pomo_touch_blocker);
    if (pomo_lock_btn != nullptr) {
      lv_obj_move_foreground(pomo_lock_btn);
    }
  } else {
    if (pomo_touch_blocker != nullptr) {
      lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
    }
    if (pomo_tool_row != nullptr) {
      lv_obj_move_foreground(pomo_tool_row);
    }
  }
}

void set_pomo_touch_locked(bool locked) {
  if (!pomo_fullscreen_mode) {
    pomo_touch_locked = false;
    return;
  }
  if (pomo_touch_locked == locked) {
    return;
  }
  pomo_touch_locked = locked;
  update_pomo_lock_btn_label();
  refresh_pomo_touch_lock_stack();
}

void pomo_lock_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  app_ui_notify_activity();
  set_pomo_touch_locked(!pomo_touch_locked);
}

void apply_pomo_card_shell_style(bool fullscreen) {
  if (card_pomo_inner == nullptr) {
    return;
  }
  lv_obj_set_style_bg_opa(card_pomo_inner, fullscreen ? LV_OPA_TRANSP : LV_OPA_COVER, 0);
  const uint32_t child_cnt = lv_obj_get_child_count(card_pomo_inner);
  for (uint32_t i = 0; i < child_cnt; i++) {
    lv_obj_t *child = lv_obj_get_child(card_pomo_inner, i);
    if (child == pomo_body || child == bar_pomo_wrap) {
      lv_obj_set_style_bg_opa(child, LV_OPA_TRANSP, 0);
      continue;
    }
    if (fullscreen) {
      lv_obj_add_flag(child, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_remove_flag(child, LV_OBJ_FLAG_HIDDEN);
    }
  }
  if (pomo_body != nullptr) {
    const uint32_t body_cnt = lv_obj_get_child_count(pomo_body);
    for (uint32_t i = 0; i < body_cnt; i++) {
      lv_obj_t *child = lv_obj_get_child(pomo_body, i);
      lv_obj_set_style_bg_opa(child, LV_OPA_TRANSP, 0);
    }
  }
}

void apply_pomo_card_layout(bool fullscreen) {
  if (card_pomo == nullptr || card_pomo_inner == nullptr) {
    return;
  }
  const int x = UI_HOME_CARD_SIDE_MARGIN;
  const int y = UI_HOME_CARDS_Y;
  const int w = fullscreen ? UI_HOME_POMO_FULL_W : UI_HOME_CARD_W;
  const int h = fullscreen ? UI_HOME_POMO_FULL_H : UI_HOME_CARD_H;
  const int inner_w = w - UI_CARD_INNER_PAD * 2;
  const int inner_h = h - UI_CARD_INNER_PAD * 2;

  lv_obj_set_pos(card_pomo, x, y);
  lv_obj_set_size(card_pomo, w, h);
  lv_obj_set_pos(card_pomo_inner, UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  lv_obj_set_size(card_pomo_inner, inner_w, inner_h);

  if (card_pomo_border != nullptr) {
    lv_obj_delete(card_pomo_border);
    card_pomo_border = nullptr;
  }
  card_pomo_border = pixel_create_jagged_border(card_pomo, 0, 0, w, h, lv_color_hex(0x8bc34a), UI_CARD_BORDER_P,
                                                UI_CARD_CORNER_INSET);
  lv_obj_move_foreground(card_pomo_border);
  lv_obj_remove_flag(card_pomo_border, LV_OBJ_FLAG_CLICKABLE);

  if (pomo_body != nullptr) {
    lv_obj_set_width(pomo_body, inner_w - 20);
    lv_obj_set_style_pad_row(pomo_body, fullscreen ? 20 : 10, 0);
  }
  apply_pomo_card_shell_style(fullscreen);
  apply_pomo_bar_layout(fullscreen, inner_w);
  if (fullscreen) {
    layout_pomo_fullscreen_buttons();
  }
}

void apply_lyric_card_layout(bool fullscreen) {
  if (card_lyric == nullptr || card_lyric_inner == nullptr) {
    return;
  }
  const int x = fullscreen ? UI_HOME_CARD_SIDE_MARGIN : (UI_HOME_CARD_SIDE_MARGIN + UI_HOME_CARD_W + UI_HOME_CARD_GAP);
  const int y = UI_HOME_CARDS_Y;
  const int w = fullscreen ? UI_HOME_POMO_FULL_W : UI_HOME_CARD_W;
  const int h = fullscreen ? UI_HOME_POMO_FULL_H : UI_HOME_CARD_H;
  const int inner_w = w - UI_CARD_INNER_PAD * 2;
  const int inner_h = h - UI_CARD_INNER_PAD * 2;

  lv_obj_set_pos(card_lyric, x, y);
  lv_obj_set_size(card_lyric, w, h);
  lv_obj_set_pos(card_lyric_inner, UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  lv_obj_set_size(card_lyric_inner, inner_w, inner_h);

  if (card_lyric_border != nullptr) {
    lv_obj_delete(card_lyric_border);
    card_lyric_border = nullptr;
  }
  card_lyric_border = pixel_create_jagged_border(card_lyric, 0, 0, w, h, lv_color_hex(kLyricAccentColor),
                                                 UI_CARD_BORDER_P, UI_CARD_CORNER_INSET);
  lv_obj_move_foreground(card_lyric_border);
  lv_obj_remove_flag(card_lyric_border, LV_OBJ_FLAG_CLICKABLE);

  if (lbl_lyric_title != nullptr) {
    lv_obj_t *title_row = lv_obj_get_parent(lbl_lyric_title);
    if (title_row != nullptr) {
      lv_obj_set_width(title_row, inner_w - 20);
    }
  }
  if (lbl_lyric_line != nullptr) {
    lv_obj_set_width(lbl_lyric_line, inner_w - 28);
    lv_obj_set_style_text_line_space(lbl_lyric_line, fullscreen ? 10 : 6, 0);
    if (fullscreen) {
      lv_obj_set_flex_grow(lbl_lyric_line, 1);
    } else {
      lv_obj_set_flex_grow(lbl_lyric_line, 0);
    }
  }

  if (fullscreen) {
    layout_lyric_fullscreen_buttons();
  }
}

void enter_pomodoro_fullscreen() {
  if (card_pomo == nullptr || pomo_fullscreen_mode) {
    return;
  }
  if (focus_mode) {
    exit_focus_mode();
  }
  if (lyric_fullscreen_mode) {
    exit_lyrics_fullscreen();
  }
  show_settings(false);
  show_more(false);
  pomo_fullscreen_mode = true;
  if (card_lyric != nullptr) {
    lv_obj_add_flag(card_lyric, LV_OBJ_FLAG_HIDDEN);
  }
  if (dock_panel != nullptr) {
    lv_obj_add_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  apply_pomo_card_layout(true);
  lv_obj_move_foreground(card_pomo);
  if (pomo_tool_row != nullptr) {
    lv_obj_remove_flag(pomo_tool_row, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_lock_btn != nullptr) {
    lv_obj_remove_flag(pomo_lock_btn, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_exit_btn != nullptr) {
    lv_obj_remove_flag(pomo_exit_btn, LV_OBJ_FLAG_HIDDEN);
  }
  layout_pomo_fullscreen_buttons();
  refresh_pomo_touch_lock_stack();
  refresh_pomodoro_card();
}

void exit_pomodoro_fullscreen() {
  if (!pomo_fullscreen_mode) {
    return;
  }
  pomo_fullscreen_mode = false;
  pomo_touch_locked = false;
  if (pomo_lock_btn != nullptr && pomo_tool_row != nullptr && lv_obj_get_parent(pomo_lock_btn) != pomo_tool_row) {
    lv_obj_set_parent(pomo_lock_btn, pomo_tool_row);
  }
  apply_pomo_card_layout(false);
  if (card_lyric != nullptr) {
    lv_obj_remove_flag(card_lyric, LV_OBJ_FLAG_HIDDEN);
  }
  if (dock_panel != nullptr) {
    lv_obj_remove_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_lock_btn != nullptr) {
    lv_obj_add_flag(pomo_lock_btn, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_exit_btn != nullptr) {
    lv_obj_add_flag(pomo_exit_btn, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_tool_row != nullptr) {
    lv_obj_add_flag(pomo_tool_row, LV_OBJ_FLAG_HIDDEN);
  }
  if (pomo_touch_blocker != nullptr) {
    lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
  }
  refresh_pomodoro_card();
}

void enter_lyrics_fullscreen() {
  if (card_lyric == nullptr || lyric_fullscreen_mode) {
    return;
  }
  if (focus_mode) {
    exit_focus_mode();
  }
  if (pomo_fullscreen_mode) {
    exit_pomodoro_fullscreen();
  }
  show_settings(false);
  show_more(false);
  lyric_fullscreen_mode = true;
  if (card_pomo != nullptr) {
    lv_obj_add_flag(card_pomo, LV_OBJ_FLAG_HIDDEN);
  }
  if (dock_panel != nullptr) {
    lv_obj_add_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  apply_lyric_card_layout(true);
  lv_obj_move_foreground(card_lyric);
  if (lyric_tool_row != nullptr) {
    lv_obj_remove_flag(lyric_tool_row, LV_OBJ_FLAG_HIDDEN);
  }
  if (lyric_exit_btn != nullptr) {
    lv_obj_remove_flag(lyric_exit_btn, LV_OBJ_FLAG_HIDDEN);
  }
  layout_lyric_fullscreen_buttons();
  refresh_lyrics_card();
}

void exit_lyrics_fullscreen() {
  if (!lyric_fullscreen_mode) {
    return;
  }
  lyric_fullscreen_mode = false;
  apply_lyric_card_layout(false);
  if (card_pomo != nullptr) {
    lv_obj_remove_flag(card_pomo, LV_OBJ_FLAG_HIDDEN);
  }
  if (dock_panel != nullptr) {
    lv_obj_remove_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  if (lyric_exit_btn != nullptr) {
    lv_obj_add_flag(lyric_exit_btn, LV_OBJ_FLAG_HIDDEN);
  }
  if (lyric_tool_row != nullptr) {
    lv_obj_add_flag(lyric_tool_row, LV_OBJ_FLAG_HIDDEN);
  }
}

void enter_focus_mode() {
  if (focus_layer == nullptr) {
    return;
  }
  if (pomo_fullscreen_mode) {
    exit_pomodoro_fullscreen();
  }
  if (lyric_fullscreen_mode) {
    exit_lyrics_fullscreen();
  }
  focus_mode = true;
  if (dock_panel != nullptr) {
    lv_obj_add_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  lv_obj_remove_flag(focus_layer, LV_OBJ_FLAG_HIDDEN);
  lv_obj_move_foreground(focus_layer);
  if (more_layer != nullptr) {
    lv_obj_add_flag(more_layer, LV_OBJ_FLAG_HIDDEN);
  }
  if (settings_layer != nullptr) {
    lv_obj_add_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);
  }
  refresh_focus_mode_ui();
}

void exit_focus_mode() {
  if (focus_layer == nullptr) {
    return;
  }
  focus_mode = false;
  lv_obj_add_flag(focus_layer, LV_OBJ_FLAG_HIDDEN);
  if (dock_panel != nullptr) {
    lv_obj_remove_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  set_dock_selected(kDockIndexHome);
}

void refresh_lock_pomodoro() {
  if (lock_pomo_box == nullptr) {
    return;
  }
  if (pomodoro_is_active_on_lock()) {
    lv_obj_remove_flag(lock_pomo_box, LV_OBJ_FLAG_HIDDEN);
    const PomodoroSnapshot p = pomodoro_get();
    char buf[48];
    std::snprintf(buf, sizeof(buf), "Pomodoro %02d:%02d", p.remaining_sec / 60, p.remaining_sec % 60);
    lv_label_set_text(lock_lbl_pomo, buf);
    const int pct = p.total_sec > 0 ? (p.total_sec - p.remaining_sec) * 100 / p.total_sec : 0;
    pomodoro_bar_set_fill_phase(lock_bar_pomo, pomodoro_bar_phase_from(p));
    lv_bar_set_value(lock_bar_pomo, pct, LV_ANIM_OFF);
  } else {
    lv_obj_add_flag(lock_pomo_box, LV_OBJ_FLAG_HIDDEN);
  }
}

void refresh_status_bar() {
  if (lbl_status_time == nullptr) {
    return;
  }
  char time_buf[16];
  app_clock_format_time(time_buf, sizeof(time_buf));
  lv_label_set_text(lbl_status_time, time_buf);
}

void set_locked(bool value) {
  locked = value;
  if (lock_layer == nullptr) {
    return;
  }
  if (locked) {
    refresh_lock_pomodoro();
    char t[16];
    app_clock_format_time(t, sizeof(t));
    lv_label_set_text(lock_lbl_time, t);
    lv_obj_remove_flag(lock_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(lock_layer);
  } else {
    lv_obj_add_flag(lock_layer, LV_OBJ_FLAG_HIDDEN);
  }
}

void show_more(bool on) {
  if (more_layer == nullptr) {
    return;
  }
  if (on) {
    lv_obj_remove_flag(more_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(more_layer);
  } else {
    lv_obj_add_flag(more_layer, LV_OBJ_FLAG_HIDDEN);
  }
}

void show_settings(bool on) {
  if (settings_layer == nullptr) {
    return;
  }
  if (on) {
    const AppSettings &s = app_settings_get();
    char buf[256];
    std::snprintf(buf, sizeof(buf),
                  "Brightness %u%%  Dim %u%%\nIdle dim %u min\nNight %02u:%02u - %02u:%02u %s\nFont scale %u  (API later)",
                  s.brightness, s.dim_brightness, s.idle_dim_minutes, s.night_start_hour, s.night_start_min,
                  s.night_end_hour, s.night_end_min, s.night_dim_enable ? "ON" : "OFF", s.font_scale);
    lv_label_set_text(lbl_settings_summary, buf);
    lv_obj_remove_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(settings_layer);
  } else {
    lv_obj_add_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);
  }
}

void placeholder_toast(const char *name) {
  ESP_LOGI(TAG, "Open placeholder: %s", name);
}

void pomodoro_card_clicked(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  if (pomo_touch_locked) {
    return;
  }
  if (pomodoro_is_operation_blocked()) {
    return;
  }
  app_ui_notify_activity();
  pomodoro_card_action();
  refresh_pomodoro_card();
}

void show_home_page() {
  show_settings(false);
  show_more(false);
  if (focus_mode) {
    exit_focus_mode();
  }
  if (pomo_fullscreen_mode) {
    exit_pomodoro_fullscreen();
  }
  if (lyric_fullscreen_mode) {
    exit_lyrics_fullscreen();
  }
  set_dock_selected(kDockIndexHome);
}

void dock_clicked(DockId id) {
  app_ui_notify_activity();
  for (int i = 0; i < 5; i++) {
    if (kDock[i].id == id) {
      set_dock_selected(i);
      break;
    }
  }
  switch (id) {
    case DockId::Home:
      show_home_page();
      break;
    case DockId::Pomodoro:
      enter_pomodoro_fullscreen();
      break;
    case DockId::Lyrics:
      enter_lyrics_fullscreen();
      break;
    case DockId::Focus:
      enter_focus_mode();
      break;
    case DockId::Settings:
      show_settings(true);
      break;
  }
}

void dock_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  auto id = static_cast<DockId>(reinterpret_cast<intptr_t>(lv_event_get_user_data(e)));
  dock_clicked(id);
}

void more_btn_event(lv_event_t *e) {
  app_ui_notify_activity();
  auto id = static_cast<MoreId>(reinterpret_cast<intptr_t>(lv_event_get_user_data(e)));
  show_more(false);
  switch (id) {
    case MoreId::Settings:
      show_settings(true);
      break;
    default:
      placeholder_toast(kMore[static_cast<int>(id)].label);
      break;
  }
}

void unlock_pad_event(lv_event_t *e) {
  const lv_event_code_t code = lv_event_get_code(e);
  lv_indev_t *indev = lv_indev_active();
  if (indev == nullptr) {
    return;
  }
  lv_point_t pt = {};
  lv_indev_get_point(indev, &pt);

  if (code == LV_EVENT_PRESSED) {
    unlock_press_y = pt.y;
    app_ui_notify_activity();
  } else if (code == LV_EVENT_PRESSING) {
    const lv_coord_t dy = unlock_press_y - pt.y;
    if (dy > 70) {
      set_locked(false);
      app_ui_notify_activity();
    }
  }
}

void sleep_scr_event(lv_event_t *e) {
  if (lv_event_get_code(e) == LV_EVENT_PRESSED) {
    app_ui_notify_activity();
    app_ui_show_home();
  }
}

void global_activity_event(lv_event_t *e) {
  if (lv_event_get_code(e) == LV_EVENT_PRESSED || lv_event_get_code(e) == LV_EVENT_CLICKED) {
    app_ui_notify_activity();
  }
}

void settings_back_event(lv_event_t *e) {
  (void)e;
  show_settings(false);
}

void settings_night_toggle(lv_event_t *e) {
  (void)e;
  AppSettings s = app_settings_get();
  s.night_dim_enable = !s.night_dim_enable;
  app_settings_set(s);
  app_settings_save();
  show_settings(true);
}

void settings_night_start_inc(lv_event_t *e) {
  (void)e;
  AppSettings s = app_settings_get();
  s.night_start_hour = static_cast<uint8_t>((s.night_start_hour + 1) % 24);
  app_settings_set(s);
  app_settings_save();
  show_settings(true);
}

void settings_night_end_inc(lv_event_t *e) {
  (void)e;
  AppSettings s = app_settings_get();
  s.night_end_hour = static_cast<uint8_t>((s.night_end_hour + 1) % 24);
  app_settings_set(s);
  app_settings_save();
  show_settings(true);
}

void tick_cb(lv_timer_t *t) {
  (void)t;
  pomodoro_tick();
  refresh_status_bar();
  refresh_pomodoro_card();
  if (focus_mode) {
    refresh_focus_mode_ui();
  }
  if (pomo_touch_locked && should_auto_unlock_pomo_touch()) {
    set_pomo_touch_locked(false);
  }
  if (locked) {
    refresh_lock_pomodoro();
    char time_buf[16];
    app_clock_format_time(time_buf, sizeof(time_buf));
    lv_label_set_text(lock_lbl_time, time_buf);
  }
  app_power_tick(locked || focus_mode || pomo_fullscreen_mode || lyric_fullscreen_mode || pomo_touch_locked);
}

void set_dock_selected(int index) {
  static const uint32_t kDockColors[] = {0x8bc34a, 0x29b6f6, 0xce93d8, 0x42a5f5, 0x42a5f5};
  for (int i = 0; i < 5; i++) {
    if (dock_borders[i] == nullptr) {
      continue;
    }
    if (i == index) {
      pixel_dock_jagged_border_set_color(dock_borders[i], lv_color_hex(kDockColors[i]));
      lv_obj_remove_flag(dock_borders[i], LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_add_flag(dock_borders[i], LV_OBJ_FLAG_HIDDEN);
    }
  }
}

void bind_home_widgets(const ui_home_widgets_t *w) {
  lbl_status_time = w->lbl_status_time;
  card_pomo = w->card_pomo;
  card_pomo_inner = w->card_pomo_inner;
  card_pomo_border = w->card_pomo_border;
  pomo_body = w->pomo_body;
  lbl_pomo_time = w->lbl_pomo_time;
  pomo_time_pixel = w->pomo_time_pixel;
  lbl_pomo_action = w->lbl_pomo_action;
  bar_pomo = w->bar_pomo;
  bar_pomo_wrap = w->bar_pomo_wrap;
  bar_pomo_border = w->bar_pomo_border;
  card_lyric = w->card_lyric;
  card_lyric_inner = w->card_lyric_inner;
  card_lyric_border = w->card_lyric_border;
  lbl_lyric_title = w->lbl_lyric_title;
  lbl_lyric_line = w->lbl_lyric_body;
  for (int i = 0; i < 5; i++) {
    dock_slots[i] = w->dock_slots[i];
    dock_borders[i] = w->dock_borders[i];
  }
  dock_panel = w->dock_panel;

  if (w->card_pomo != nullptr) {
    lv_obj_add_flag(w->card_pomo, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(w->card_pomo, pomodoro_card_clicked, LV_EVENT_PRESSED, nullptr);
  }

  for (int i = 0; i < 5; i++) {
    if (dock_slots[i] == nullptr) {
      continue;
    }
    lv_obj_add_flag(dock_slots[i], LV_OBJ_FLAG_CLICKABLE);
    lv_obj_move_foreground(dock_slots[i]);
    lv_obj_add_event_cb(dock_slots[i], dock_btn_event, LV_EVENT_PRESSED,
                        reinterpret_cast<void *>(static_cast<intptr_t>(kDock[i].id)));
  }
}

lv_obj_t *create_tool_button(lv_obj_t *parent, const char *text, uint32_t accent_color, lv_event_cb_t on_press,
                             lv_obj_t **lbl_out) {
  constexpr int kInnerPad = UI_DOCK_SEL_CORNER_INSET + UI_DOCK_BORDER_P;

  lv_obj_t *btn = lv_obj_create(parent);
  lv_obj_set_size(btn, kPomoToolBtnW, kPomoToolBtnH);
  layout_abs(btn);
  lv_obj_set_style_bg_opa(btn, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(btn, 0, 0);
  lv_obj_set_style_pad_all(btn, 0, 0);
  lv_obj_set_style_radius(btn, 0, 0);
  lv_obj_add_flag(btn, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(btn, LV_OBJ_FLAG_SCROLLABLE);
  if (on_press != nullptr) {
    lv_obj_add_event_cb(btn, on_press, LV_EVENT_PRESSED, nullptr);
  }

  const int inner_w = kPomoToolBtnW - kInnerPad * 2;
  const int inner_h = kPomoToolBtnH - kInnerPad * 2;

  lv_obj_t *inner = lv_obj_create(btn);
  lv_obj_set_pos(inner, kInnerPad, kInnerPad);
  lv_obj_set_size(inner, inner_w, inner_h);
  lv_obj_set_style_bg_color(inner, lv_color_hex(0x101028), 0);
  lv_obj_set_style_bg_opa(inner, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(inner, 0, 0);
  lv_obj_set_style_pad_all(inner, 0, 0);
  lv_obj_set_style_radius(inner, 0, 0);
  lv_obj_set_layout(inner, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(inner, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(inner, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_remove_flag(inner, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(inner, LV_OBJ_FLAG_SCROLLABLE);

  lv_obj_t *lbl = lv_label_create(inner);
  lv_label_set_text(lbl, text);
  style_pixel_label(lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(accent_color));
  lv_obj_set_style_text_align(lbl, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_remove_flag(lbl, LV_OBJ_FLAG_CLICKABLE);
  if (lbl_out != nullptr) {
    *lbl_out = lbl;
  }

  lv_obj_t *border = pixel_create_jagged_border(btn, 0, 0, kPomoToolBtnW, kPomoToolBtnH, lv_color_hex(accent_color),
                                                UI_DOCK_BORDER_P, UI_DOCK_SEL_CORNER_INSET);
  lv_obj_move_foreground(border);
  lv_obj_remove_flag(border, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(border, LV_OBJ_FLAG_SCROLLABLE);
  return btn;
}

void layout_pomo_fullscreen_buttons() {
  if (!pomo_fullscreen_mode || card_pomo == nullptr || pomo_tool_row == nullptr) {
    return;
  }
  update_pomo_tool_row_for_lock_state();
  refresh_pomo_touch_lock_stack();
}

void build_pomo_touch_blocker(lv_obj_t *parent) {
  pomo_touch_blocker = lv_obj_create(parent);
  lv_obj_set_size(pomo_touch_blocker, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(pomo_touch_blocker, 0, 0);
  layout_abs(pomo_touch_blocker);
  lv_obj_set_style_bg_opa(pomo_touch_blocker, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(pomo_touch_blocker, 0, 0);
  lv_obj_set_style_pad_all(pomo_touch_blocker, 0, 0);
  lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_remove_flag(pomo_touch_blocker, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_event_cb(pomo_touch_blocker, pomo_touch_blocker_event, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_event_cb(pomo_touch_blocker, pomo_touch_blocker_event, LV_EVENT_CLICKED, nullptr);
  lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
}

void build_pomo_fullscreen_buttons(lv_obj_t *card_parent) {
  if (card_parent == nullptr) {
    return;
  }
  pomo_tool_row = lv_obj_create(card_parent);
  lv_obj_set_size(pomo_tool_row, kPomoToolRowW, kPomoToolBtnH);
  layout_abs(pomo_tool_row);
  lv_obj_set_style_bg_opa(pomo_tool_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(pomo_tool_row, 0, 0);
  lv_obj_set_style_pad_all(pomo_tool_row, 0, 0);
  lv_obj_set_style_pad_column(pomo_tool_row, kPomoToolBtnGap, 0);
  lv_obj_set_layout(pomo_tool_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(pomo_tool_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(pomo_tool_row, LV_FLEX_ALIGN_END, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_remove_flag(pomo_tool_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_remove_flag(pomo_tool_row, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_flag(pomo_tool_row, LV_OBJ_FLAG_HIDDEN);

  pomo_exit_btn = create_tool_button(pomo_tool_row, "\xe9\x80\x80\xe5\x87\xba", 0x8bc34a, pomo_exit_btn_event, nullptr);
  pomo_lock_btn = create_tool_button(pomo_tool_row, "\xe9\x94\x81\xe5\xb1\x8f", 0x8bc34a, pomo_lock_btn_event, &pomo_lock_btn_lbl);
  lv_obj_set_flex_grow(pomo_exit_btn, 0);
  lv_obj_set_flex_grow(pomo_lock_btn, 0);
}

void layout_lyric_fullscreen_buttons() {
  if (!lyric_fullscreen_mode || card_lyric == nullptr || lyric_tool_row == nullptr) {
    return;
  }
  lv_obj_align(lyric_tool_row, LV_ALIGN_TOP_RIGHT, -UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  lv_obj_move_foreground(lyric_tool_row);
}

void build_lyric_fullscreen_buttons(lv_obj_t *card_parent) {
  if (card_parent == nullptr) {
    return;
  }
  lyric_tool_row = lv_obj_create(card_parent);
  lv_obj_set_size(lyric_tool_row, kPomoToolBtnW, kPomoToolBtnH);
  layout_abs(lyric_tool_row);
  lv_obj_set_style_bg_opa(lyric_tool_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(lyric_tool_row, 0, 0);
  lv_obj_set_style_pad_all(lyric_tool_row, 0, 0);
  lv_obj_remove_flag(lyric_tool_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_remove_flag(lyric_tool_row, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_flag(lyric_tool_row, LV_OBJ_FLAG_HIDDEN);

  lyric_exit_btn = create_tool_button(lyric_tool_row, "\xe9\x80\x80\xe5\x87\xba", kLyricAccentColor, lyric_exit_btn_event,
                                      nullptr);
}

void build_home_content(lv_obj_t *parent) {
  ui_home_widgets_t widgets = {};
  ui_home_static_build(parent, &widgets);
  bind_home_widgets(&widgets);
  build_pomo_fullscreen_buttons(widgets.card_pomo);
  build_lyric_fullscreen_buttons(widgets.card_lyric);
  set_dock_selected(kDockIndexHome);
  refresh_status_bar();
  refresh_pomodoro_card();
  refresh_lyrics_card();
}

void build_focus_layer(lv_obj_t *parent) {
  focus_layer = lv_obj_create(parent);
  lv_obj_set_size(focus_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(focus_layer, 0, 0);
  lv_obj_set_style_bg_color(focus_layer, lv_color_hex(0x08081a), 0);
  lv_obj_set_style_bg_opa(focus_layer, LV_OPA_COVER, 0);
  lv_obj_remove_flag(focus_layer, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(focus_layer, LV_OBJ_FLAG_HIDDEN);
  layout_abs(focus_layer);

  lv_obj_t *card = lv_obj_create(focus_layer);
  lv_obj_set_pos(card, kFocusMargin, kFocusMargin);
  lv_obj_set_size(card, kFocusW, kFocusH);
  lv_obj_remove_flag(card, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_style_bg_opa(card, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(card, 0, 0);
  lv_obj_set_style_pad_all(card, 0, 0);
  layout_abs(card);

  lv_obj_t *inner = lv_obj_create(card);
  lv_obj_set_pos(inner, UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
  lv_obj_set_size(inner, kFocusW - UI_CARD_INNER_PAD * 2, kFocusH - UI_CARD_INNER_PAD * 2);
  lv_obj_set_style_bg_color(inner, lv_color_hex(0x101028), 0);
  lv_obj_set_style_bg_opa(inner, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(inner, 0, 0);
  lv_obj_set_style_pad_all(inner, 16, 0);
  lv_obj_set_layout(inner, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(inner, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(inner, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(inner, 18, 0);
  layout_abs(inner);

  lv_obj_t *border = pixel_create_jagged_border(card, 0, 0, kFocusW, kFocusH, lv_color_hex(0x8bc34a),
                                                UI_CARD_BORDER_P, UI_CARD_CORNER_INSET);
  lv_obj_move_foreground(border);
  lv_obj_remove_flag(border, LV_OBJ_FLAG_CLICKABLE);

  lv_obj_t *title = lv_label_create(inner);
  lv_label_set_text(title, "番茄钟 · 专注模式");
  style_pixel_label(title, &lv_font_cn_gb2312_16_0, lv_color_hex(0x8bc34a));

  lv_obj_t *tomato_wrap = lv_obj_create(inner);
  layout_abs(tomato_wrap);
  lv_obj_set_size(tomato_wrap, 160, 160);
  lv_obj_set_style_bg_opa(tomato_wrap, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(tomato_wrap, 0, 0);
  lv_obj_t *tomato_img = lv_image_create(tomato_wrap);
  if (!assets_set_image_src(tomato_img, SD_ASSET_TOMATO)) {
    lv_obj_delete(tomato_img);
    pixel_create_tomato_sprite(tomato_wrap, 24, 24, 8);
  } else {
    lv_obj_set_size(tomato_img, 140, 140);
    lv_obj_center(tomato_img);
  }

  focus_lbl_time = lv_label_create(inner);
  lv_label_set_text(focus_lbl_time, "25:00");
  style_pixel_label(focus_lbl_time, &lv_font_montserrat_28, lv_color_hex(0x8bc34a));

  focus_lbl_action = lv_label_create(inner);
  lv_label_set_text(focus_lbl_action, "▶ 开始专注 ◀");
  style_pixel_label(focus_lbl_action, &lv_font_cn_gb2312_16_0, lv_color_hex(0x8bc34a));

  focus_bar_pomo = lv_bar_create(inner);
  pomodoro_bar_init(focus_bar_pomo, POMO_BAR_W, 220);
  lv_obj_align(focus_bar_pomo, LV_ALIGN_RIGHT_MID, -2, 0);
  lv_obj_add_flag(focus_bar_pomo, LV_OBJ_FLAG_HIDDEN);

  focus_exit_hint = lv_label_create(focus_layer);
  lv_label_set_text(focus_exit_hint, "点击可退出专注模式");
  style_pixel_label(focus_exit_hint, &lv_font_cn_gb2312_16_0, lv_palette_main(LV_PALETTE_CYAN));
  lv_obj_align(focus_exit_hint, LV_ALIGN_BOTTOM_MID, 0, -24);
  lv_obj_add_flag(focus_exit_hint, LV_OBJ_FLAG_HIDDEN);

  focus_pomo_tap = lv_obj_create(focus_layer);
  lv_obj_set_size(focus_pomo_tap, kFocusW - 40, kFocusH - 80);
  lv_obj_align(focus_pomo_tap, LV_ALIGN_CENTER, 0, -20);
  lv_obj_set_style_bg_opa(focus_pomo_tap, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(focus_pomo_tap, 0, 0);
  lv_obj_add_flag(focus_pomo_tap, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(focus_pomo_tap, focus_pomo_tap_event, LV_EVENT_PRESSED, nullptr);

  focus_exit_tap = lv_obj_create(focus_layer);
  lv_obj_set_size(focus_exit_tap, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(focus_exit_tap, 0, 0);
  lv_obj_set_style_bg_opa(focus_exit_tap, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(focus_exit_tap, 0, 0);
  lv_obj_add_flag(focus_exit_tap, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_flag(focus_exit_tap, LV_OBJ_FLAG_HIDDEN);
  lv_obj_add_event_cb(focus_exit_tap, focus_exit_tap_event, LV_EVENT_PRESSED, nullptr);
}

void build_lock_layer(lv_obj_t *parent) {
  lock_layer = lv_obj_create(parent);
  lv_obj_set_size(lock_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(lock_layer, 0, 0);
  lv_obj_set_style_bg_color(lock_layer, lv_color_hex(0x080812), 0);
  lv_obj_set_style_bg_opa(lock_layer, LV_OPA_90, 0);
  lv_obj_remove_flag(lock_layer, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(lock_layer, LV_OBJ_FLAG_HIDDEN);

  lock_lbl_time = lv_label_create(lock_layer);
  style_pixel_label(lock_lbl_time, &lv_font_montserrat_28, lv_color_white());
  lv_obj_align(lock_lbl_time, LV_ALIGN_TOP_MID, 0, 40);

  lock_pomo_box = lv_obj_create(lock_layer);
  lv_obj_set_size(lock_pomo_box, PANEL_WIDTH - 80, 90);
  lv_obj_align(lock_pomo_box, LV_ALIGN_CENTER, 0, -20);
  style_card(lock_pomo_box, 0x4caf50);

  lv_obj_t *pomo_title = lv_label_create(lock_pomo_box);
  lv_label_set_text(pomo_title, "Focus");
  style_pixel_label(pomo_title, &lv_font_montserrat_20, lv_palette_main(LV_PALETTE_RED));
  lv_obj_align(pomo_title, LV_ALIGN_TOP_MID, 0, 0);

  lock_lbl_pomo = lv_label_create(lock_pomo_box);
  style_pixel_label(lock_lbl_pomo, &lv_font_montserrat_28, lv_color_white());
  lv_obj_align(lock_lbl_pomo, LV_ALIGN_CENTER, 0, -4);

  lock_bar_pomo = lv_bar_create(lock_pomo_box);
  pomodoro_bar_init(lock_bar_pomo, POMO_BAR_W, 56);
  lv_obj_align(lock_bar_pomo, LV_ALIGN_RIGHT_MID, -6, 0);

  unlock_hint = lv_label_create(lock_layer);
  lv_label_set_text(unlock_hint, "Slide up to unlock");
  style_pixel_label(unlock_hint, &lv_font_montserrat_20, lv_palette_main(LV_PALETTE_CYAN));
  lv_obj_align(unlock_hint, LV_ALIGN_BOTTOM_MID, 0, -70);

  unlock_pad = lv_obj_create(lock_layer);
  lv_obj_set_size(unlock_pad, PANEL_WIDTH - 60, 90);
  lv_obj_align(unlock_pad, LV_ALIGN_BOTTOM_MID, 0, -10);
  lv_obj_set_style_bg_color(unlock_pad, lv_color_hex(0x2d6cdf), 0);
  lv_obj_set_style_bg_opa(unlock_pad, LV_OPA_40, 0);
  lv_obj_set_style_radius(unlock_pad, 8, 0);
  lv_obj_add_event_cb(unlock_pad, unlock_pad_event, LV_EVENT_ALL, nullptr);
}

void build_more_layer(lv_obj_t *parent) {
  more_layer = lv_obj_create(parent);
  lv_obj_set_size(more_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(more_layer, 0, 0);
  lv_obj_set_style_bg_color(more_layer, lv_color_hex(0x101020), 0);
  lv_obj_set_style_bg_opa(more_layer, LV_OPA_COVER, 0);
  lv_obj_add_flag(more_layer, LV_OBJ_FLAG_HIDDEN);

  lv_obj_t *title = lv_label_create(more_layer);
  lv_label_set_text(title, "More");
  style_pixel_label(title, &lv_font_montserrat_28, lv_color_white());
  lv_obj_align(title, LV_ALIGN_TOP_MID, 0, 16);

  const int cols = 3;
  const int btn_w = 150;
  const int btn_h = 64;
  for (int i = 0; i < static_cast<int>(sizeof(kMore) / sizeof(kMore[0])); i++) {
    const int row = i / cols;
    const int col = i % cols;
    lv_obj_t *btn = lv_button_create(more_layer);
    lv_obj_set_size(btn, btn_w, btn_h);
    lv_obj_set_pos(btn, 40 + col * (btn_w + 20), 70 + row * (btn_h + 16));
    lv_obj_add_event_cb(btn, more_btn_event, LV_EVENT_CLICKED,
                        reinterpret_cast<void *>(static_cast<intptr_t>(kMore[i].id)));
    lv_obj_t *lbl = lv_label_create(btn);
    lv_label_set_text(lbl, kMore[i].label);
    style_pixel_label(lbl, &lv_font_montserrat_20, lv_color_white());
    lv_obj_center(lbl);
  }

  lv_obj_t *close = lv_button_create(more_layer);
  lv_obj_set_size(close, 120, 40);
  lv_obj_align(close, LV_ALIGN_BOTTOM_MID, 0, -12);
  lv_obj_add_event_cb(close, [](lv_event_t *e) {
    (void)e;
    show_more(false);
  }, LV_EVENT_CLICKED, nullptr);
  lv_obj_t *close_lbl = lv_label_create(close);
  lv_label_set_text(close_lbl, "Back");
  lv_obj_center(close_lbl);
}

void build_settings_layer(lv_obj_t *parent) {
  settings_layer = lv_obj_create(parent);
  lv_obj_set_size(settings_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_style_bg_color(settings_layer, lv_color_hex(0x101020), 0);
  lv_obj_add_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);

  lv_obj_t *title = lv_label_create(settings_layer);
  lv_label_set_text(title, "Settings");
  style_pixel_label(title, &lv_font_montserrat_28, lv_color_white());
  lv_obj_align(title, LV_ALIGN_TOP_MID, 0, 12);

  lbl_settings_summary = lv_label_create(settings_layer);
  lv_obj_set_width(lbl_settings_summary, PANEL_WIDTH - 32);
  lv_label_set_long_mode(lbl_settings_summary, LV_LABEL_LONG_WRAP);
  style_pixel_label(lbl_settings_summary, &lv_font_montserrat_14, lv_color_white());
  lv_obj_align(lbl_settings_summary, LV_ALIGN_TOP_LEFT, 16, 56);

  lv_obj_t *night_btn = lv_button_create(settings_layer);
  lv_obj_set_size(night_btn, 180, 36);
  lv_obj_align(night_btn, LV_ALIGN_BOTTOM_MID, 0, -96);
  lv_obj_add_event_cb(night_btn, settings_night_toggle, LV_EVENT_CLICKED, nullptr);
  lv_obj_t *night_lbl = lv_label_create(night_btn);
  lv_label_set_text(night_lbl, "Night Dim On/Off");
  lv_obj_center(night_lbl);

  lv_obj_t *ns_btn = lv_button_create(settings_layer);
  lv_obj_set_size(ns_btn, 160, 36);
  lv_obj_set_pos(ns_btn, 40, PANEL_HEIGHT - 150);
  lv_obj_add_event_cb(ns_btn, settings_night_start_inc, LV_EVENT_CLICKED, nullptr);
  lv_obj_t *ns_lbl = lv_label_create(ns_btn);
  lv_label_set_text(ns_lbl, "Night Start +1h");
  lv_obj_center(ns_lbl);

  lv_obj_t *ne_btn = lv_button_create(settings_layer);
  lv_obj_set_size(ne_btn, 160, 36);
  lv_obj_set_pos(ne_btn, PANEL_WIDTH - 200, PANEL_HEIGHT - 150);
  lv_obj_add_event_cb(ne_btn, settings_night_end_inc, LV_EVENT_CLICKED, nullptr);
  lv_obj_t *ne_lbl = lv_label_create(ne_btn);
  lv_label_set_text(ne_lbl, "Night End +1h");
  lv_obj_center(ne_lbl);

  lv_obj_t *back = lv_button_create(settings_layer);
  lv_obj_set_size(back, 120, 40);
  lv_obj_align(back, LV_ALIGN_BOTTOM_MID, 0, -10);
  lv_obj_add_event_cb(back, settings_back_event, LV_EVENT_CLICKED, nullptr);
  lv_obj_t *back_lbl = lv_label_create(back);
  lv_label_set_text(back_lbl, "Back");
  lv_obj_center(back_lbl);
}
}  // namespace

esp_err_t app_ui_init() {
  display_lock();
  srand(static_cast<unsigned>(esp_timer_get_time()));
  app_settings_init();
  app_clock_init();
  pomodoro_init();
  app_power_init();

  scr_home = lv_obj_create(nullptr);
  lv_obj_remove_style_all(scr_home);
  lv_obj_set_size(scr_home, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_remove_flag(scr_home, LV_OBJ_FLAG_SCROLLABLE);
  lv_scr_load(scr_home);

  build_home_content(scr_home);
  build_focus_layer(scr_home);
  build_lock_layer(scr_home);
  build_pomo_touch_blocker(scr_home);
  build_more_layer(scr_home);
  build_settings_layer(scr_home);

  dim_overlay = lv_obj_create(scr_home);
  lv_obj_set_size(dim_overlay, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(dim_overlay, 0, 0);
  lv_obj_set_style_bg_color(dim_overlay, lv_color_black(), 0);
  lv_obj_set_style_bg_opa(dim_overlay, LV_OPA_60, 0);
  lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(dim_overlay, global_activity_event, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_HIDDEN);

  scr_sleep = lv_obj_create(nullptr);
  lv_obj_set_style_bg_color(scr_sleep, lv_color_hex(0x000000), 0);
  lv_obj_add_flag(scr_sleep, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(scr_sleep, sleep_scr_event, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_flag(scr_sleep, LV_OBJ_FLAG_HIDDEN);

  app_power_bind_overlays(dim_overlay, scr_sleep);

  lv_timer_create(tick_cb, 1000, nullptr);
  display_unlock();

  ESP_LOGI(TAG, "Home UI ready (ui_home_static_layout)");
  return ESP_OK;
}

void app_ui_notify_activity() {
  app_power_notify_activity();
}

void app_ui_show_home() {
  display_lock();
  if (scr_sleep != nullptr) {
    lv_obj_add_flag(scr_sleep, LV_OBJ_FLAG_HIDDEN);
  }
  if (scr_home != nullptr) {
    lv_scr_load(scr_home);
  }
  display_unlock();
  if (app_power_get_visual() != PowerVisualState::Bright) {
    app_power_set_visual(PowerVisualState::Bright);
  }
}
