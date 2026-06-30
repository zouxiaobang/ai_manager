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
#include "panel_config.h"
#include "pixel_ui.h"
#include "pomodoro_model.h"
#include "ui_home_static_layout.h"

namespace {
constexpr char TAG[] = "app_ui";
constexpr uint32_t kColBg = 0x0a0a18;
constexpr uint32_t kColCard = 0x12122a;

enum class DockId { Pomodoro, Lyrics, Sleep, Lock, Settings };
enum class MoreId { Weather, Stats, Notes, Settings, Media };

lv_obj_t *scr_home = nullptr;
lv_obj_t *scr_sleep = nullptr;
lv_obj_t *dim_overlay = nullptr;
lv_obj_t *lock_layer = nullptr;
lv_obj_t *more_layer = nullptr;
lv_obj_t *settings_layer = nullptr;
lv_obj_t *lbl_pomo_time = nullptr;
lv_obj_t *lbl_pomo_action = nullptr;
lv_obj_t *dock_slots[5] = {};
lv_obj_t *dock_borders[5] = {};
lv_obj_t *dock_dots[UI_HOME_PAGE_DOTS] = {};
lv_obj_t *dock_sel_bar = nullptr;
lv_obj_t *bar_pomo = nullptr;
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
lv_coord_t unlock_press_y = 0;

struct DockItem {
  DockId id;
};

const DockItem kDock[] = {
    {DockId::Pomodoro},
    {DockId::Lyrics},
    {DockId::Sleep},
    {DockId::Lock},
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

void refresh_pomodoro_card() {
  if (lbl_pomo_time == nullptr || lbl_pomo_action == nullptr) {
    return;
  }
  const PomodoroSnapshot p = pomodoro_get();
  char buf[32];
  const int mm = p.remaining_sec / 60;
  const int ss = p.remaining_sec % 60;
  std::snprintf(buf, sizeof(buf), "%02d:%02d", mm, ss);
  lv_label_set_text(lbl_pomo_time, buf);

  const char *state = "▶ 开始专注 ◀";
  if (p.phase != PomodoroPhase::Idle) {
    state = p.running ? "专注中..." : "已暂停";
  }
  lv_label_set_text(lbl_pomo_action, state);

  if (bar_pomo == nullptr) {
    return;
  }
  int pct = 0;
  if (p.total_sec > 0) {
    pct = (p.total_sec - p.remaining_sec) * 100 / p.total_sec;
  }
  lv_bar_set_value(bar_pomo, pct, LV_ANIM_OFF);
  if (p.phase != PomodoroPhase::Idle && p.running) {
    lv_obj_remove_flag(bar_pomo, LV_OBJ_FLAG_HIDDEN);
  } else {
    lv_obj_add_flag(bar_pomo, LV_OBJ_FLAG_HIDDEN);
  }
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
    lv_bar_set_value(lock_bar_pomo, pct, LV_ANIM_OFF);
  } else {
    lv_obj_add_flag(lock_pomo_box, LV_OBJ_FLAG_HIDDEN);
  }
}

void refresh_status_bar() {}

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

void set_dock_selected(int index);

void pomodoro_card_clicked(lv_event_t *e) {
  (void)e;
  app_ui_notify_activity();
  pomodoro_toggle_start();
  refresh_pomodoro_card();
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
    case DockId::Pomodoro:
      pomodoro_toggle_start();
      refresh_pomodoro_card();
      break;
    case DockId::Lyrics:
      placeholder_toast("Lyrics");
      break;
    case DockId::Sleep:
      app_power_enter_sleep();
      break;
    case DockId::Lock:
      set_locked(true);
      break;
    case DockId::Settings:
      show_settings(true);
      break;
  }
}

void dock_btn_event(lv_event_t *e) {
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
  if (locked) {
    refresh_lock_pomodoro();
    char time_buf[16];
    app_clock_format_time(time_buf, sizeof(time_buf));
    lv_label_set_text(lock_lbl_time, time_buf);
  }
  app_power_tick(locked);
}

void set_dock_selected(int index) {
  for (int i = 0; i < 5; i++) {
    if (dock_borders[i] != nullptr) {
      if (i == index) {
        pixel_dock_jagged_border_set_color(dock_borders[i], lv_color_hex(0x8bc34a));
        lv_obj_remove_flag(dock_borders[i], LV_OBJ_FLAG_HIDDEN);
      } else {
        lv_obj_add_flag(dock_borders[i], LV_OBJ_FLAG_HIDDEN);
      }
    }
  }
  if (dock_sel_bar != nullptr) {
    const int slot_w = (PANEL_WIDTH - UI_HOME_MARGIN * 2) / 5;
    lv_obj_set_pos(dock_sel_bar, UI_HOME_MARGIN + index * slot_w + 10, UI_HOME_DOCK_Y + UI_HOME_DOCK_H - 4);
    lv_obj_set_size(dock_sel_bar, slot_w - 20, 3);
    lv_obj_remove_flag(dock_sel_bar, LV_OBJ_FLAG_HIDDEN);
  }
  for (int i = 0; i < UI_HOME_PAGE_DOTS; i++) {
    if (dock_dots[i] == nullptr) {
      continue;
    }
    const bool active = i == 0;
    if (i == 0) {
      lv_obj_set_size(dock_dots[i], active ? 18 : 8, active ? 4 : 8);
      lv_obj_set_style_radius(dock_dots[i], active ? 2 : LV_RADIUS_CIRCLE, 0);
    }
    lv_obj_set_style_bg_color(dock_dots[i],
                              active ? lv_color_hex(0x8bc34a) : lv_color_hex(0x404060), 0);
  }
}

void bind_home_widgets(const ui_home_widgets_t *w) {
  lbl_pomo_time = w->lbl_pomo_time;
  lbl_pomo_action = w->lbl_pomo_action;
  bar_pomo = w->bar_pomo;
  lbl_lyric_title = w->lbl_lyric_title;
  lbl_lyric_line = w->lbl_lyric_body;
  dock_sel_bar = w->dock_sel_bar;
  for (int i = 0; i < 5; i++) {
    dock_slots[i] = w->dock_slots[i];
    dock_borders[i] = w->dock_borders[i];
  }
  for (int i = 0; i < UI_HOME_PAGE_DOTS; i++) {
    dock_dots[i] = w->dock_dots[i];
  }

  if (w->card_pomo != nullptr) {
    lv_obj_add_event_cb(w->card_pomo, pomodoro_card_clicked, LV_EVENT_CLICKED, nullptr);
    lv_obj_add_event_cb(w->card_pomo, global_activity_event, LV_EVENT_PRESSED, nullptr);
  }

  for (int i = 0; i < 5; i++) {
    if (dock_slots[i] == nullptr) {
      continue;
    }
    lv_obj_add_flag(dock_slots[i], LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(dock_slots[i], dock_btn_event, LV_EVENT_CLICKED,
                        reinterpret_cast<void *>(static_cast<intptr_t>(kDock[i].id)));
  }
}

void build_home_content(lv_obj_t *parent) {
  ui_home_widgets_t widgets = {};
  ui_home_static_build(parent, &widgets);
  bind_home_widgets(&widgets);
  set_dock_selected(0);
  refresh_pomodoro_card();
  refresh_lyrics_card();
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
  lv_obj_set_size(lock_bar_pomo, PANEL_WIDTH - 120, 12);
  lv_obj_align(lock_bar_pomo, LV_ALIGN_BOTTOM_MID, 0, -4);
  lv_bar_set_range(lock_bar_pomo, 0, 100);
  lv_obj_set_style_bg_color(lock_bar_pomo, lv_color_hex(0x303060), LV_PART_MAIN);
  lv_obj_set_style_bg_color(lock_bar_pomo, lv_palette_main(LV_PALETTE_RED), LV_PART_INDICATOR);

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
  lv_obj_set_style_bg_color(scr_home, lv_color_hex(kColBg), 0);
  layout_abs(scr_home);
  lv_obj_set_size(scr_home, PANEL_WIDTH, PANEL_HEIGHT);
  lv_scr_load(scr_home);

  build_home_content(scr_home);
  build_lock_layer(scr_home);
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

  ESP_LOGI(TAG, "Home UI ready (static layout)");
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
