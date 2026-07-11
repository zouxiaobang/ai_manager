#include "app_ui.h"

#include <cinttypes>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <new>

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
#include "pixel_dog_model.h"
#include "pixel_dog_sprite.h"
#include "pixel_dog_sync.h"
#include "pixel_ui.h"
#include "pomodoro_bar.h"
#include "pomodoro_model.h"
#include "sd_assets.h"
#include "ui_home_static_layout.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

namespace {
constexpr char TAG[] = "app_ui";
constexpr uint32_t kColBg = 0x0a0a18;
constexpr uint32_t kDogAccent = 0xff8a65;
#define COL_DOG kDogAccent

enum class DockId { Home, Pomodoro, PixelDog, Focus, Settings };
enum class MoreId { Weather, Stats, Notes, Settings, Media };

lv_obj_t *scr_home = nullptr;
lv_obj_t *scr_sleep = nullptr;
lv_obj_t *dim_overlay = nullptr;
lv_obj_t *media_toast = nullptr;
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
lv_obj_t *card_dog = nullptr;
lv_obj_t *card_dog_inner = nullptr;
lv_obj_t *card_dog_border = nullptr;
lv_obj_t *dog_sprite = nullptr;
lv_obj_t *dog_sprite_wrap = nullptr;
lv_obj_t *lbl_dog_speech = nullptr;
lv_obj_t *dog_pet_btn = nullptr;
lv_obj_t *dog_greet_btn = nullptr;
lv_obj_t *dog_exit_btn = nullptr;
lv_obj_t *dog_tool_row = nullptr;
lv_obj_t *pomo_exit_btn = nullptr;
lv_obj_t *pomo_lock_btn = nullptr;
lv_obj_t *pomo_lock_btn_lbl = nullptr;
lv_obj_t *pomo_tool_row = nullptr;
lv_obj_t *pomo_touch_blocker = nullptr;
lv_obj_t *focus_layer = nullptr;
lv_obj_t *focus_dog_wrap = nullptr;
lv_obj_t *focus_lbl_time = nullptr;
lv_obj_t *focus_lbl_action = nullptr;
lv_obj_t *focus_bar_pomo = nullptr;
lv_obj_t *focus_exit_hint = nullptr;
lv_obj_t *focus_pomo_tap = nullptr;
lv_obj_t *focus_exit_tap = nullptr;
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
bool dog_fullscreen_mode = false;
bool pomo_touch_locked = false;
lv_coord_t unlock_press_y = 0;

lv_obj_t *dog_fullscreen_layer = nullptr;
lv_obj_t *dog_fullscreen_left = nullptr;
lv_obj_t *dog_fullscreen_center = nullptr;
lv_obj_t *dog_fullscreen_right = nullptr;
lv_obj_t *dog_fullscreen_speech = nullptr;
lv_obj_t *dog_fullscreen_sprite_area = nullptr;
lv_obj_t *lbl_dog_emotion_val = nullptr;
lv_obj_t *lbl_dog_bond_val = nullptr;
lv_obj_t *lbl_fullscreen_lv = nullptr;
lv_obj_t *bar_fullscreen_xp = nullptr;
lv_obj_t *bar_fullscreen_bond = nullptr;
lv_obj_t *bar_dog_emotion = nullptr;
lv_obj_t *dog_hug_btn = nullptr;
lv_obj_t *dog_nuzzle_btn = nullptr;
lv_obj_t *home_dog_nuzzle_btn = nullptr;
lv_obj_t *home_dog_hug_btn = nullptr;
lv_obj_t *lbl_dog_pomo_time = nullptr;
lv_obj_t *lbl_dog_pomo_state = nullptr;
lv_obj_t *bar_dog_pomo_mini = nullptr;
lv_obj_t *dog_fullscreen_tab_bar = nullptr;
lv_obj_t *dog_tab_home_btn = nullptr;
lv_obj_t *dog_tab_items_btn = nullptr;
lv_obj_t *dog_tab_history_btn = nullptr;
lv_obj_t *dog_items_panel = nullptr;
lv_obj_t *dog_history_panel = nullptr;

constexpr int kDockIndexPomodoro = 0;
constexpr int kDockIndexPixelDog = 1;
constexpr int kDockIndexHome = 2;
constexpr uint32_t kDogAccentColor = 0xff8a65;
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
    {DockId::PixelDog},
    {DockId::Home},
    {DockId::Focus},
    {DockId::Settings},
};

int ui_tick_counter = 0;

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

void pomodoro_card_clicked(lv_event_t *e);
void dog_pet_btn_event(lv_event_t *e);
void dog_greet_btn_event(lv_event_t *e);
void dog_nuzzle_btn_event(lv_event_t *e);
void dog_hug_btn_event(lv_event_t *e);
void dock_btn_event(lv_event_t *e);
void spawn_dog_particles(const char *emoji, int count);

void layout_abs(lv_obj_t *obj) {
  if (obj == nullptr) return;
  lv_obj_remove_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_style_layout(obj, LV_LAYOUT_NONE, 0);
}

bool ui_click_debounce(int64_t *last_tap_us, int64_t interval_us = 350000) {
  const int64_t now_us = esp_timer_get_time();
  if (*last_tap_us != 0 && now_us - *last_tap_us < interval_us) {
    return false;
  }
  *last_tap_us = now_us;
  return true;
}

void bind_home_click_targets() {
  if (card_pomo != nullptr) {
    lv_obj_add_flag(card_pomo, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(card_pomo, pomodoro_card_clicked, LV_EVENT_CLICKED, nullptr);
  }
  if (card_dog != nullptr) {
    lv_obj_remove_flag(card_dog, LV_OBJ_FLAG_CLICKABLE);
  }
  if (dog_pet_btn != nullptr) {
    lv_obj_add_flag(dog_pet_btn, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(dog_pet_btn, dog_pet_btn_event, LV_EVENT_CLICKED, nullptr);
  }
  if (dog_greet_btn != nullptr) {
    lv_obj_add_flag(dog_greet_btn, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(dog_greet_btn, dog_greet_btn_event, LV_EVENT_CLICKED, nullptr);
  }
  if (dog_nuzzle_btn != nullptr) {
    lv_obj_add_flag(dog_nuzzle_btn, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(dog_nuzzle_btn, dog_nuzzle_btn_event, LV_EVENT_CLICKED, nullptr);
  }
  if (dog_hug_btn != nullptr) {
    lv_obj_add_flag(dog_hug_btn, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_add_event_cb(dog_hug_btn, dog_hug_btn_event, LV_EVENT_CLICKED, nullptr);
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

void attach_press_target(lv_obj_t *obj, lv_event_cb_t on_press) {
  if (obj == nullptr || on_press == nullptr) {
    return;
  }
  lv_obj_add_flag(obj, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(obj, on_press, LV_EVENT_PRESSED, nullptr);
}

void sync_idle_overlay_touch(bool active) {
  if (dim_overlay == nullptr) {
    return;
  }
  if (active) {
    lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_CLICKABLE);
  } else {
    lv_obj_remove_flag(dim_overlay, LV_OBJ_FLAG_CLICKABLE);
  }
}

void style_card(lv_obj_t *obj, uint32_t border_color) {
  lv_obj_set_style_bg_opa(obj, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(obj, 2, 0);
  lv_obj_set_style_border_color(obj, lv_color_hex(border_color), 0);
  lv_obj_set_style_radius(obj, 8, 0);
  lv_obj_set_style_pad_all(obj, 14, 0);
  layout_abs(obj);
}

void style_pixel_label(lv_obj_t *lbl, const lv_font_t *font, lv_color_t color) {
  if (lbl == nullptr) return;
  lv_obj_set_style_text_font(lbl, font, 0);
  lv_obj_set_style_text_color(lbl, color, 0);
}

void refresh_dog_card() {
  const DogState *state = dog_model_get();
  if (state == nullptr) {
    return;
  }
  
  // --- Update Level (fullscreen) ---
  if (lbl_fullscreen_lv != nullptr) {
    char buf[16];
    std::snprintf(buf, sizeof(buf), "Lv.%" PRIu32, state->level);
    lv_label_set_text(lbl_fullscreen_lv, buf);
  }
  
  // --- Update XP bar (fullscreen) ---
  if (bar_fullscreen_xp != nullptr) {
    int pct = 0;
    if (state->xp_next > 0) {
      pct = (state->xp * 100) / state->xp_next;
    }
    lv_bar_set_value(bar_fullscreen_xp, pct, LV_ANIM_OFF);
  }
  
  // --- Update Bond (fullscreen) ---
  if (bar_fullscreen_bond != nullptr) {
    lv_bar_set_value(bar_fullscreen_bond, state->bond, LV_ANIM_OFF);
  }
  
  if (lbl_dog_bond_val != nullptr) {
    char buf[8];
    std::snprintf(buf, sizeof(buf), "%" PRIu32, state->bond);
    lv_label_set_text(lbl_dog_bond_val, buf);
  }
  
  // --- Update Emotion display ---
  if (bar_dog_emotion != nullptr) {
    int emotion_pct = ((int)state->emotion + 100) * 100 / 200;
    if (emotion_pct < 0) emotion_pct = 0;
    if (emotion_pct > 100) emotion_pct = 100;
    lv_bar_set_value(bar_dog_emotion, emotion_pct, LV_ANIM_OFF);
  }
  
  if (lbl_dog_emotion_val != nullptr) {
    const char *text = "平静";
    uint32_t color = 0x5c9fd4;
    if (state->emotion >= 50) { text = "开心"; color = 0xffb74d; }
    else if (state->emotion >= 20) { text = "愉快"; color = 0x8bc34a; }
    else if (state->emotion >= 0) { text = "平静"; color = 0x5c9fd4; }
    else if (state->emotion >= -30) { text = "无聊"; color = 0x7e57c2; }
    else { text = "低落"; color = 0xef5350; }
    lv_label_set_text(lbl_dog_emotion_val, text);
    style_pixel_label(lbl_dog_emotion_val, &lv_font_cn_gb2312_16_0, lv_color_hex(color));
    
    if (bar_dog_emotion != nullptr) {
      lv_obj_set_style_bg_color(bar_dog_emotion, lv_color_hex(color), LV_PART_INDICATOR);
    }
  }
  
  // --- Update Nuzzle button visibility (intimacy >= 60) ---
  // 使用与PC端一致的亲密度公式: intimacy = (emotion+100)/2 * 0.4 + bond * 0.6
  // 整数形式: intimacy_x10 = (emotion+100)*2 + bond*6
  lv_obj_t *nuzzle_target = dog_fullscreen_mode ? dog_nuzzle_btn : home_dog_nuzzle_btn;
  if (nuzzle_target != nullptr) {
    const int intimacy_x10 = (static_cast<int>(state->emotion) + 100) * 2 + static_cast<int>(state->bond) * 6;
    if (intimacy_x10 >= 600) {
      lv_obj_remove_flag(nuzzle_target, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_add_flag(nuzzle_target, LV_OBJ_FLAG_HIDDEN);
    }
  }

  // --- Update Hug button visibility (intimacy >= 85) ---
  lv_obj_t *hug_target = dog_fullscreen_mode ? dog_hug_btn : home_dog_hug_btn;
  if (hug_target != nullptr) {
    const int intimacy_x10 = (static_cast<int>(state->emotion) + 100) * 2 + static_cast<int>(state->bond) * 6;
    if (intimacy_x10 >= 850) {
      lv_obj_remove_flag(hug_target, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_add_flag(hug_target, LV_OBJ_FLAG_HIDDEN);
    }
  }
  
  // --- Speech bubble (refresh every 8s, matching PC端) ---
  {
    static int64_t last_speech_ts = 0;
    const int64_t now_us = esp_timer_get_time();
    if (now_us - last_speech_ts >= 8000000LL) {
      last_speech_ts = now_us;
      const char *speech = dog_model_get_speech();
      if (speech == nullptr) speech = "\xe4\xbb\x8a\xe5\xa4\xa9\xe4\xb9\x9f\xe8\xa6\x81\xe5\x8a\xa0\xe6\xb2\xb9\xe5\x93\xa6!";
      
      // Style speech bubble based on emotion (matching PC端)
      if (dog_fullscreen_speech != nullptr) {
        lv_label_set_text(dog_fullscreen_speech, speech);
        
        uint32_t bg_color = 0xffffff;
        uint32_t border_color = 0xa1887f;
        uint32_t text_color = 0x333333;
        
        if (state->emotion >= 20) {
          // Happy
          bg_color = 0xfff8e1;
          border_color = 0xffc107;
          text_color = 0xe65100;
        } else if (state->emotion >= 0) {
          // Normal
          bg_color = 0xffffff;
          border_color = 0xa1887f;
          text_color = 0x333333;
        } else if (state->emotion >= -30) {
          // Sad
          bg_color = 0xe3f2fd;
          border_color = 0x64b5f6;
          text_color = 0x1565c0;
        } else {
          // Angry/Depressed
          bg_color = 0xffebee;
          border_color = 0xef5350;
          text_color = 0xc62828;
        }
        
        lv_obj_set_style_bg_color(dog_fullscreen_speech, lv_color_hex(bg_color), LV_PART_MAIN);
        lv_obj_set_style_border_color(dog_fullscreen_speech, lv_color_hex(border_color), LV_PART_MAIN);
        lv_obj_set_style_text_color(dog_fullscreen_speech, lv_color_hex(text_color), 0);
      }
      
      if (lbl_dog_speech != nullptr) {
        lv_label_set_text(lbl_dog_speech, speech);
      }
    }
  }
  
  // --- Update Mini Pomodoro (dog fullscreen left panel) ---
  if (dog_fullscreen_mode) {
    const PomodoroSnapshot p = pomodoro_get();
    
    if (lbl_dog_pomo_time != nullptr) {
      if (p.today_goal_done) {
        lv_label_set_text(lbl_dog_pomo_time, "--:--");
      } else {
        char buf[16];
        std::snprintf(buf, sizeof(buf), "%02d:%02d", p.remaining_sec / 60, p.remaining_sec % 60);
        lv_label_set_text(lbl_dog_pomo_time, buf);
      }
    }
    
    if (lbl_dog_pomo_state != nullptr) {
      const char *state = "空闲";
      uint32_t state_color = 0x8090a8;
      if (p.phase == PomodoroPhase::Focus) {
        state = p.running ? "专注中..." : "已暂停";
        state_color = 0xe86a5a;
      } else if (p.phase == PomodoroPhase::ShortBreak) {
        state = p.running ? "短休息中..." : "已暂停";
        state_color = 0x8fbc7a;
      } else if (p.phase == PomodoroPhase::LongBreak) {
        state = p.running ? "长休息中..." : "已暂停";
        state_color = 0xa88bc4;
      }
      lv_label_set_text(lbl_dog_pomo_state, state);
      style_pixel_label(lbl_dog_pomo_state, &lv_font_cn_gb2312_16_0, lv_color_hex(state_color));
    }
    
    if (bar_dog_pomo_mini != nullptr) {
      int pct = 0;
      if (p.total_sec > 0 && p.phase != PomodoroPhase::Idle) {
        pct = (p.total_sec - p.remaining_sec) * 100 / p.total_sec;
      }
      lv_bar_set_value(bar_dog_pomo_mini, pct, LV_ANIM_OFF);
      
      uint32_t bar_color = 0x8bc34a;
      if (p.phase == PomodoroPhase::Focus) bar_color = 0xe86a5a;
      else if (p.phase == PomodoroPhase::ShortBreak) bar_color = 0x8fbc7a;
      else if (p.phase == PomodoroPhase::LongBreak) bar_color = 0xa88bc4;
      lv_obj_set_style_bg_color(bar_dog_pomo_mini, lv_color_hex(bar_color), LV_PART_INDICATOR);
    }
  }
  
  dog_sprite_set_level(state->level);
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
  if (bar_pomo_border != nullptr) {
    lv_obj_move_foreground(bar_pomo_border);
    lv_obj_remove_flag(bar_pomo_border, LV_OBJ_FLAG_CLICKABLE);
  }
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
void enter_dog_fullscreen();
void exit_dog_fullscreen();
void show_more(bool on);
void show_settings(bool on);
void show_home_page();
void set_dock_selected(int index);
void apply_pomo_card_layout(bool fullscreen);
void layout_pomo_fullscreen_buttons();
void layout_dog_fullscreen_buttons();
void bind_home_click_targets();
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

// ── Dog particle effects ──────────────────────────────────────────
// Spawn colored pixel squares that float upward and fade out

struct ParticleData {
    lv_obj_t *obj;
    int start_x, start_y;
    int end_x, end_y;
    int duration;
    int start_time;
};

static void particle_anim_cb(void *var, int32_t progress) {
    auto *pd = static_cast<ParticleData *>(var);
    if (pd == nullptr || pd->obj == nullptr) return;

    // progress: 0 → 1024
    int x = pd->start_x + (pd->end_x - pd->start_x) * progress / 1024;
    int y = pd->start_y + (pd->end_y - pd->start_y) * progress / 1024;
    lv_obj_set_pos(pd->obj, x, y);

    lv_opa_t opa = LV_OPA_COVER - (LV_OPA_COVER * progress / 1024);
    lv_obj_set_style_bg_opa(pd->obj, opa, 0);
}

static void particle_delete_cb(lv_anim_t *a) {
    auto *pd = static_cast<ParticleData *>(a->var);
    if (pd != nullptr) {
        if (pd->obj != nullptr) lv_obj_delete(pd->obj);
        delete pd;
    }
}

void spawn_dog_particles(const char *emoji, int count) {
    (void)emoji;
    lv_obj_t *parent = dog_fullscreen_center;
    int cx, cy;
    if (parent == nullptr || !dog_fullscreen_mode) {
        parent = lv_scr_act();
        // On home card: spawn particles near the dog sprite wrap
        if (dog_sprite_wrap != nullptr) {
            lv_area_t coords;
            lv_obj_get_coords(dog_sprite_wrap, &coords);
            cx = (coords.x1 + coords.x2) / 2;
            cy = (coords.y1 + coords.y2) / 2 - 20;
        } else {
            cx = lv_obj_get_width(parent) / 2;
            cy = lv_obj_get_height(parent) / 2 - 30;
        }
    } else {
        cx = lv_obj_get_width(parent) / 2;
        cy = lv_obj_get_height(parent) / 2 - 30;
    }
    if (parent == nullptr) return;
    const int spread_x = 60;
    const int spread_y = 25;

    // Larger absolute count = bigger particles
    int pixel_sz = 8;
    int actual_count = count;
    if (count < 0) {
        pixel_sz = 12;
        actual_count = -count;
    }
    // Cap to avoid overload
    if (actual_count > 20) actual_count = 20;

    // Pick color palette based on emoji hint
    uint32_t colors[5];
    int ncolors;
    if (emoji[0] == '\xe2' && emoji[1] == '\x9d') {
        // ❤ -> red/pink
        colors[0] = 0xff5252; colors[1] = 0xff8a80; colors[2] = 0xff1744;
        colors[3] = 0xff80ab; colors[4] = 0xf50057;
        ncolors = 5;
    } else if (emoji[0] == '\xf0' && emoji[1] == '\x9f' && emoji[2] == '\x92') {
        // 💕 -> pink
        colors[0] = 0xff80ab; colors[1] = 0xff5252; colors[2] = 0xec407a;
        colors[3] = 0xf48fb1; colors[4] = 0xff4081;
        ncolors = 5;
    } else if (emoji[0] == '\xe2' && emoji[1] == '\x9c') {
        // ✨ -> gold/yellow
        colors[0] = 0xffd740; colors[1] = 0xffab00; colors[2] = 0xffe082;
        colors[3] = 0xffd54f; colors[4] = 0xffca28;
        ncolors = 5;
    } else {
        // default -> orange
        colors[0] = 0xff8a65; colors[1] = 0xffa270; colors[2] = 0xff6d40;
        colors[3] = 0xff8a65; colors[4] = 0xffa270;
        ncolors = 5;
    }

    for (int i = 0; i < actual_count; i++) {
        auto *pd = new (std::nothrow) ParticleData();
        if (pd == nullptr) break;

        pd->obj = lv_obj_create(parent);
        if (pd->obj == nullptr) { delete pd; break; }
        lv_obj_remove_style_all(pd->obj);
        lv_obj_set_size(pd->obj, pixel_sz, pixel_sz);
        lv_obj_set_style_bg_color(pd->obj, lv_color_hex(colors[rand() % ncolors]), 0);
        lv_obj_set_style_bg_opa(pd->obj, LV_OPA_COVER, 0);
        lv_obj_remove_flag(pd->obj, LV_OBJ_FLAG_CLICKABLE);
        lv_obj_remove_flag(pd->obj, LV_OBJ_FLAG_SCROLLABLE);

        pd->start_x = cx + (rand() % (spread_x * 2 + 1)) - spread_x;
        pd->start_y = cy + (rand() % (spread_y * 2 + 1)) - spread_y;
        lv_obj_set_pos(pd->obj, pd->start_x, pd->start_y);

        pd->end_x = pd->start_x + (rand() % 61) - 30;
        pd->end_y = pd->start_y - (60 + rand() % 61);
        pd->duration = 700 + rand() % 500;

        // Single combined animation for position + fade
        lv_anim_t a;
        lv_anim_init(&a);
        lv_anim_set_var(&a, pd);
        lv_anim_set_exec_cb(&a, particle_anim_cb);
        lv_anim_set_values(&a, 0, 1024);
        lv_anim_set_time(&a, pd->duration);
        lv_anim_set_ready_cb(&a, particle_delete_cb);
        lv_anim_start(&a);
    }
}

void dog_exit_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_PRESSED) {
    return;
  }
  app_ui_notify_activity();
  show_home_page();
}

void dog_pet_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  ESP_LOGI(TAG, "Dog pet button tapped");
  app_ui_notify_activity();
  dog_sync_interact("pet");
  dog_sprite_set_status(DOG_STATUS_PETTING);
  spawn_dog_particles("\xe2\x9d\xa4\xef\xb8\x8f", 3);  // ❤️ 3个红色粒子
  refresh_dog_card();
}

void dog_greet_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  ESP_LOGI(TAG, "Dog greet button tapped");
  app_ui_notify_activity();
  dog_sync_interact("greet");
  dog_sprite_set_status(DOG_STATUS_GREETING);
  spawn_dog_particles("\xf0\x9f\x91\x8b", 6);  // 👋 6个橙色粒子
  refresh_dog_card();
}

void dog_nuzzle_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  ESP_LOGI(TAG, "Dog nuzzle button tapped");
  app_ui_notify_activity();
  dog_sync_interact("nuzzle");
  dog_sprite_set_status(DOG_STATUS_PETTING);
  spawn_dog_particles("\xf0\x9f\x92\x95", 12);  // 💕 12个粉色粒子
  refresh_dog_card();
}

void dog_hug_btn_event(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  ESP_LOGI(TAG, "Dog hug button tapped");
  app_ui_notify_activity();
  dog_sync_interact("hug");
  dog_sprite_set_status(DOG_STATUS_HAPPY);
  spawn_dog_particles("\xe2\x9c\xa8", -20);  // ✨ 20个金色大粒子（负数为大尺寸）
  refresh_dog_card();
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
    lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(pomo_touch_blocker);
    if (pomo_lock_btn != nullptr) {
      lv_obj_move_foreground(pomo_lock_btn);
    }
  } else {
    if (pomo_touch_blocker != nullptr) {
      lv_obj_add_flag(pomo_touch_blocker, LV_OBJ_FLAG_HIDDEN);
      lv_obj_remove_flag(pomo_touch_blocker, LV_OBJ_FLAG_CLICKABLE);
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
  lv_obj_set_style_bg_opa(card_pomo_inner, LV_OPA_TRANSP, 0);
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
  if (card_pomo_border != nullptr) {
    lv_obj_move_foreground(card_pomo_border);
    lv_obj_remove_flag(card_pomo_border, LV_OBJ_FLAG_CLICKABLE);
  }

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

void enter_pomodoro_fullscreen() {
  if (card_pomo == nullptr || pomo_fullscreen_mode) {
    return;
  }
  if (focus_mode) {
    exit_focus_mode();
  }
  if (dog_fullscreen_mode) {
    exit_dog_fullscreen();
  }
  show_settings(false);
  show_more(false);
  pomo_fullscreen_mode = true;
  if (card_dog != nullptr) {
    lv_obj_add_flag(card_dog, LV_OBJ_FLAG_HIDDEN);
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
  if (card_dog != nullptr) {
    lv_obj_remove_flag(card_dog, LV_OBJ_FLAG_HIDDEN);
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

void build_dog_fullscreen_layer() {
  ESP_LOGI(TAG, "build_dog_fullscreen: start");
  dog_fullscreen_layer = lv_obj_create(nullptr);
  lv_obj_remove_style_all(dog_fullscreen_layer);
  lv_obj_set_size(dog_fullscreen_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_style_bg_color(dog_fullscreen_layer, lv_color_hex(0x08081a), 0);
  lv_obj_set_style_bg_opa(dog_fullscreen_layer, LV_OPA_COVER, 0);
  layout_abs(dog_fullscreen_layer);

  // Exit button (top-right)
  lv_obj_t *btn = lv_btn_create(dog_fullscreen_layer);
  lv_obj_set_size(btn, 80, 30);
  lv_obj_align(btn, LV_ALIGN_TOP_RIGHT, -10, 10);
  lv_obj_t *btn_lbl = lv_label_create(btn);
  lv_label_set_text(btn_lbl, "退出");
  style_pixel_label(btn_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(0xffffff));
  lv_obj_center(btn_lbl);
  lv_obj_add_event_cb(btn, [](lv_event_t *e) {
    (void)e;
    exit_dog_fullscreen();
  }, LV_EVENT_CLICKED, nullptr);

  const int panel_y = 55;
  const int panel_h = PANEL_HEIGHT - panel_y - 50;
  const int panel_w = 150;

  // --- Left panel: Status ---
  dog_fullscreen_left = lv_obj_create(dog_fullscreen_layer);
  lv_obj_set_pos(dog_fullscreen_left, 15, panel_y);
  lv_obj_set_size(dog_fullscreen_left, panel_w, panel_h);
  lv_obj_set_style_bg_color(dog_fullscreen_left, lv_color_hex(0x101028), 0);
  lv_obj_set_style_bg_opa(dog_fullscreen_left, LV_OPA_80, 0);
  lv_obj_set_style_border_width(dog_fullscreen_left, 0, 0);
  lv_obj_remove_flag(dog_fullscreen_left, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_layout(dog_fullscreen_left, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(dog_fullscreen_left, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(dog_fullscreen_left, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_all(dog_fullscreen_left, 10, 0);
  lv_obj_set_style_pad_row(dog_fullscreen_left, 6, 0);

  // Level section (title + data on same row)
  lv_obj_t *lv_row = lv_obj_create(dog_fullscreen_left);
  lv_obj_set_size(lv_row, panel_w - 20, 24);
  lv_obj_set_style_bg_opa(lv_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(lv_row, 0, 0);
  lv_obj_remove_flag(lv_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_layout(lv_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(lv_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(lv_row, LV_FLEX_ALIGN_SPACE_BETWEEN, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  lv_obj_t *lv_icon = lv_label_create(lv_row);
  lv_label_set_text(lv_icon, "等级");
  style_pixel_label(lv_icon, &lv_font_cn_gb2312_16_0, lv_color_hex(0xffd700));

  lbl_fullscreen_lv = lv_label_create(lv_row);
  lv_label_set_text(lbl_fullscreen_lv, "Lv.1");
  style_pixel_label(lbl_fullscreen_lv, &lv_font_montserrat_20, lv_color_hex(0x8bc34a));

  bar_fullscreen_xp = lv_bar_create(dog_fullscreen_left);
  lv_obj_set_size(bar_fullscreen_xp, panel_w - 20, 10);
  lv_bar_set_value(bar_fullscreen_xp, 0, LV_ANIM_OFF);
  lv_obj_set_style_bg_color(bar_fullscreen_xp, lv_color_hex(0x1a1a3a), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar_fullscreen_xp, LV_OPA_COVER, LV_PART_MAIN);
  lv_obj_set_style_border_width(bar_fullscreen_xp, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_fullscreen_xp, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_fullscreen_xp, 0, LV_PART_INDICATOR);
  lv_obj_set_style_bg_color(bar_fullscreen_xp, lv_color_hex(kDogAccent), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar_fullscreen_xp, LV_OPA_COVER, LV_PART_INDICATOR);

  // Bond section (title + data on same row)
  lv_obj_t *bond_row = lv_obj_create(dog_fullscreen_left);
  lv_obj_set_size(bond_row, panel_w - 20, 24);
  lv_obj_set_style_bg_opa(bond_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(bond_row, 0, 0);
  lv_obj_remove_flag(bond_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_layout(bond_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(bond_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(bond_row, LV_FLEX_ALIGN_SPACE_BETWEEN, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  lv_obj_t *bond_icon = lv_label_create(bond_row);
  lv_label_set_text(bond_icon, "陪伴值");
  style_pixel_label(bond_icon, &lv_font_cn_gb2312_16_0, lv_color_hex(0xff5252));

  lbl_dog_bond_val = lv_label_create(bond_row);
  lv_label_set_text(lbl_dog_bond_val, "0");
  style_pixel_label(lbl_dog_bond_val, &lv_font_montserrat_14, lv_color_hex(0xff5252));

  bar_fullscreen_bond = lv_bar_create(dog_fullscreen_left);
  lv_obj_set_size(bar_fullscreen_bond, panel_w - 20, 10);
  lv_bar_set_value(bar_fullscreen_bond, 0, LV_ANIM_OFF);
  lv_obj_set_style_bg_color(bar_fullscreen_bond, lv_color_hex(0x1a1a3a), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar_fullscreen_bond, LV_OPA_COVER, LV_PART_MAIN);
  lv_obj_set_style_border_width(bar_fullscreen_bond, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_fullscreen_bond, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_fullscreen_bond, 0, LV_PART_INDICATOR);
  lv_obj_set_style_bg_color(bar_fullscreen_bond, lv_color_hex(0xff5252), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar_fullscreen_bond, LV_OPA_COVER, LV_PART_INDICATOR);

  // Emotion section (title + data on same row)
  lv_obj_t *emotion_row = lv_obj_create(dog_fullscreen_left);
  lv_obj_set_size(emotion_row, panel_w - 20, 24);
  lv_obj_set_style_bg_opa(emotion_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(emotion_row, 0, 0);
  lv_obj_remove_flag(emotion_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_layout(emotion_row, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(emotion_row, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(emotion_row, LV_FLEX_ALIGN_SPACE_BETWEEN, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);

  lv_obj_t *emotion_icon = lv_label_create(emotion_row);
  lv_label_set_text(emotion_icon, "心情");
  style_pixel_label(emotion_icon, &lv_font_cn_gb2312_16_0, lv_color_hex(0x5c9fd4));

  lbl_dog_emotion_val = lv_label_create(emotion_row);
  lv_label_set_text(lbl_dog_emotion_val, "平静");
  style_pixel_label(lbl_dog_emotion_val, &lv_font_cn_gb2312_16_0, lv_color_hex(0x5c9fd4));

  bar_dog_emotion = lv_bar_create(dog_fullscreen_left);
  lv_obj_set_size(bar_dog_emotion, panel_w - 20, 10);
  lv_bar_set_value(bar_dog_emotion, 50, LV_ANIM_OFF);
  lv_obj_set_style_bg_color(bar_dog_emotion, lv_color_hex(0x1a1a3a), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar_dog_emotion, LV_OPA_COVER, LV_PART_MAIN);
  lv_obj_set_style_border_width(bar_dog_emotion, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_dog_emotion, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_dog_emotion, 0, LV_PART_INDICATOR);
  lv_obj_set_style_bg_color(bar_dog_emotion, lv_color_hex(0x5c9fd4), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar_dog_emotion, LV_OPA_COVER, LV_PART_INDICATOR);

  // Flexible spacer to push pomodoro to bottom
  lv_obj_t *spacer = lv_obj_create(dog_fullscreen_left);
  lv_obj_set_size(spacer, panel_w - 20, 1);
  lv_obj_set_style_bg_opa(spacer, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(spacer, 0, 0);
  lv_obj_remove_flag(spacer, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_flex_grow(spacer, 1);

  // --- Mini Pomodoro (read-only, synced) ---
  lv_obj_t *pomo_sep = lv_obj_create(dog_fullscreen_left);
  lv_obj_set_size(pomo_sep, panel_w - 20, 2);
  lv_obj_set_style_bg_color(pomo_sep, lv_color_hex(0x2a2a50), 0);
  lv_obj_set_style_bg_opa(pomo_sep, LV_OPA_50, 0);
  lv_obj_set_style_border_width(pomo_sep, 0, 0);
  lv_obj_remove_flag(pomo_sep, LV_OBJ_FLAG_SCROLLABLE);

  lv_obj_t *pomo_header = lv_label_create(dog_fullscreen_left);
  lv_label_set_text(pomo_header, "番茄钟");
  style_pixel_label(pomo_header, &lv_font_cn_gb2312_16_0, lv_color_hex(0x8bc34a));

  lbl_dog_pomo_time = lv_label_create(dog_fullscreen_left);
  lv_label_set_text(lbl_dog_pomo_time, "--:--");
  style_pixel_label(lbl_dog_pomo_time, &lv_font_montserrat_20, lv_color_hex(0x8bc34a));

  bar_dog_pomo_mini = lv_bar_create(dog_fullscreen_left);
  lv_obj_set_size(bar_dog_pomo_mini, panel_w - 20, 8);
  lv_bar_set_value(bar_dog_pomo_mini, 0, LV_ANIM_OFF);
  lv_obj_set_style_bg_color(bar_dog_pomo_mini, lv_color_hex(0x1a1a3a), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar_dog_pomo_mini, LV_OPA_COVER, LV_PART_MAIN);
  lv_obj_set_style_border_width(bar_dog_pomo_mini, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_dog_pomo_mini, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar_dog_pomo_mini, 0, LV_PART_INDICATOR);
  lv_obj_set_style_bg_color(bar_dog_pomo_mini, lv_color_hex(0x8bc34a), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar_dog_pomo_mini, LV_OPA_COVER, LV_PART_INDICATOR);
  lv_obj_remove_flag(bar_dog_pomo_mini, LV_OBJ_FLAG_CLICKABLE);

  lbl_dog_pomo_state = lv_label_create(dog_fullscreen_left);
  lv_label_set_text(lbl_dog_pomo_state, "空闲");
  style_pixel_label(lbl_dog_pomo_state, &lv_font_cn_gb2312_16_0, lv_color_hex(0x8090a8));

  // --- Center panel ---
  const int center_x = 15 + panel_w + 5;
  const int center_w = PANEL_WIDTH - 15 - panel_w - 5 - 5 - panel_w - 15;
  dog_fullscreen_center = lv_obj_create(dog_fullscreen_layer);
  lv_obj_set_pos(dog_fullscreen_center, center_x, panel_y);
  lv_obj_set_size(dog_fullscreen_center, center_w, panel_h);
  lv_obj_set_style_bg_opa(dog_fullscreen_center, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(dog_fullscreen_center, 0, 0);
  lv_obj_remove_flag(dog_fullscreen_center, LV_OBJ_FLAG_SCROLLABLE);
  layout_abs(dog_fullscreen_center);

  // Speech bubble text (PC style)
  dog_fullscreen_speech = lv_label_create(dog_fullscreen_center);
  lv_label_set_text(dog_fullscreen_speech, dog_model_get_speech());
  lv_obj_align(dog_fullscreen_speech, LV_ALIGN_TOP_MID, 0, 10);
  lv_obj_set_style_text_font(dog_fullscreen_speech, &lv_font_cn_gb2312_16_0, 0);
  lv_obj_set_style_text_color(dog_fullscreen_speech, lv_color_hex(0xffe082), 0);
  lv_obj_set_style_text_align(dog_fullscreen_speech, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_set_style_bg_color(dog_fullscreen_speech, lv_color_hex(0x1a1a3a), 0);
  lv_obj_set_style_bg_opa(dog_fullscreen_speech, LV_OPA_80, 0);
  lv_obj_set_style_border_color(dog_fullscreen_speech, lv_color_hex(0x3949ab), 0);
  lv_obj_set_style_border_width(dog_fullscreen_speech, 2, 0);
  lv_obj_set_style_pad_all(dog_fullscreen_speech, 8, 0);
  lv_obj_set_width(dog_fullscreen_speech, center_w - 40);

  // Sprite container (below speech bubble, centered)
  dog_fullscreen_sprite_area = lv_obj_create(dog_fullscreen_center);
  lv_obj_set_size(dog_fullscreen_sprite_area, 200, 200);
  lv_obj_align(dog_fullscreen_sprite_area, LV_ALIGN_CENTER, 0, 20);
  lv_obj_set_style_bg_opa(dog_fullscreen_sprite_area, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(dog_fullscreen_sprite_area, 0, 0);
  layout_abs(dog_fullscreen_sprite_area);
  lv_obj_remove_flag(dog_fullscreen_sprite_area, LV_OBJ_FLAG_CLICKABLE);

  // --- Right panel: Interaction ---
  const int right_x = 15 + panel_w + 5 + center_w + 5;
  dog_fullscreen_right = lv_obj_create(dog_fullscreen_layer);
  lv_obj_set_pos(dog_fullscreen_right, right_x, panel_y);
  lv_obj_set_size(dog_fullscreen_right, panel_w, panel_h);
  lv_obj_set_style_bg_color(dog_fullscreen_right, lv_color_hex(0x101028), 0);
  lv_obj_set_style_bg_opa(dog_fullscreen_right, LV_OPA_80, 0);
  lv_obj_set_style_border_width(dog_fullscreen_right, 0, 0);
  lv_obj_remove_flag(dog_fullscreen_right, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_set_layout(dog_fullscreen_right, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(dog_fullscreen_right, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(dog_fullscreen_right, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_all(dog_fullscreen_right, 10, 0);
  lv_obj_set_style_pad_row(dog_fullscreen_right, 10, 0);

  // Pet button (always visible)
  lv_obj_t *pet_btn = lv_btn_create(dog_fullscreen_right);
  lv_obj_set_size(pet_btn, panel_w - 20, 40);
  lv_obj_set_style_bg_color(pet_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_bg_opa(pet_btn, LV_OPA_30, 0);
  lv_obj_set_style_border_color(pet_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_border_width(pet_btn, 2, 0);
  lv_obj_set_style_radius(pet_btn, 6, 0);
  lv_obj_t *pet_lbl = lv_label_create(pet_btn);
  lv_label_set_text(pet_lbl, "摸摸头");
  style_pixel_label(pet_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(COL_DOG));
  lv_obj_center(pet_lbl);
  lv_obj_add_event_cb(pet_btn, dog_pet_btn_event, LV_EVENT_CLICKED, nullptr);

  // Greet button (always visible)
  lv_obj_t *greet_btn = lv_btn_create(dog_fullscreen_right);
  lv_obj_set_size(greet_btn, panel_w - 20, 40);
  lv_obj_set_style_bg_color(greet_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_bg_opa(greet_btn, LV_OPA_30, 0);
  lv_obj_set_style_border_color(greet_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_border_width(greet_btn, 2, 0);
  lv_obj_set_style_radius(greet_btn, 6, 0);
  lv_obj_t *greet_lbl = lv_label_create(greet_btn);
  lv_label_set_text(greet_lbl, "问好");
  style_pixel_label(greet_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(COL_DOG));
  lv_obj_center(greet_lbl);
  lv_obj_add_event_cb(greet_btn, dog_greet_btn_event, LV_EVENT_CLICKED, nullptr);

  // Nuzzle button (intimacy >= 60)
  dog_nuzzle_btn = lv_btn_create(dog_fullscreen_right);
  lv_obj_set_size(dog_nuzzle_btn, panel_w - 20, 40);
  lv_obj_set_style_bg_color(dog_nuzzle_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_bg_opa(dog_nuzzle_btn, LV_OPA_30, 0);
  lv_obj_set_style_border_color(dog_nuzzle_btn, lv_color_hex(COL_DOG), 0);
  lv_obj_set_style_border_width(dog_nuzzle_btn, 2, 0);
  lv_obj_set_style_radius(dog_nuzzle_btn, 6, 0);
  lv_obj_t *nuzzle_lbl = lv_label_create(dog_nuzzle_btn);
  lv_label_set_text(nuzzle_lbl, "蹭蹭");
  style_pixel_label(nuzzle_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(COL_DOG));
  lv_obj_center(nuzzle_lbl);
  lv_obj_add_event_cb(dog_nuzzle_btn, dog_nuzzle_btn_event, LV_EVENT_CLICKED, nullptr);
  lv_obj_add_flag(dog_nuzzle_btn, LV_OBJ_FLAG_HIDDEN);

  // Hug button (intimacy >= 85)
  dog_hug_btn = lv_btn_create(dog_fullscreen_right);
  lv_obj_set_size(dog_hug_btn, panel_w - 20, 40);
  lv_obj_set_style_bg_color(dog_hug_btn, lv_color_hex(0xff5252), 0);
  lv_obj_set_style_bg_opa(dog_hug_btn, LV_OPA_30, 0);
  lv_obj_set_style_border_color(dog_hug_btn, lv_color_hex(0xff5252), 0);
  lv_obj_set_style_border_width(dog_hug_btn, 2, 0);
  lv_obj_set_style_radius(dog_hug_btn, 6, 0);
  lv_obj_t *hug_lbl = lv_label_create(dog_hug_btn);
  lv_label_set_text(hug_lbl, "抱抱");
  style_pixel_label(hug_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(0xff5252));
  lv_obj_center(hug_lbl);
  lv_obj_add_event_cb(dog_hug_btn, dog_hug_btn_event, LV_EVENT_CLICKED, nullptr);
  lv_obj_add_flag(dog_hug_btn, LV_OBJ_FLAG_HIDDEN);

  // Null out tab pointers (not used in simple layout)
  dog_fullscreen_tab_bar = nullptr;
  dog_tab_home_btn = nullptr;
  dog_tab_items_btn = nullptr;
  dog_tab_history_btn = nullptr;
  dog_items_panel = nullptr;
  dog_history_panel = nullptr;

  ESP_LOGI(TAG, "build_dog_fullscreen: done");
}

void enter_dog_fullscreen() {
  ESP_LOGI(TAG, "enter_dog_fullscreen: start");
  if (dog_fullscreen_mode) {
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
  dog_fullscreen_mode = true;

  ESP_LOGI(TAG, "enter_dog_fullscreen: building layer");
  if (dog_fullscreen_layer == nullptr) {
    build_dog_fullscreen_layer();
  }

  ESP_LOGI(TAG, "enter_dog_fullscreen: moving sprite");
  if (dog_sprite != nullptr && dog_fullscreen_sprite_area != nullptr) {
    lv_obj_set_parent(dog_sprite, dog_fullscreen_sprite_area);
    lv_obj_center(dog_sprite);
  }

  // Refresh all panels with latest data
  refresh_dog_card();

  ESP_LOGI(TAG, "enter_dog_fullscreen: loading screen");
  lv_scr_load(dog_fullscreen_layer);
  lv_obj_invalidate(lv_scr_act());
  lv_refr_now(nullptr);
  ESP_LOGI(TAG, "enter_dog_fullscreen: done");
}

void exit_dog_fullscreen() {
  ESP_LOGI(TAG, "exit_dog_fullscreen: start");
  dog_fullscreen_mode = false;
  refresh_dog_card();
  if (dog_sprite != nullptr && dog_sprite_wrap != nullptr) {
    lv_obj_set_parent(dog_sprite, dog_sprite_wrap);
    lv_obj_center(dog_sprite);
  }
  if (scr_home != nullptr) {
    lv_scr_load(scr_home);
    lv_obj_invalidate(lv_scr_act());
    lv_refr_now(nullptr);
  }
  ESP_LOGI(TAG, "exit_dog_fullscreen: done");
}

void enter_focus_mode() {
  if (focus_layer == nullptr) {
    return;
  }
  if (pomo_fullscreen_mode) {
    exit_pomodoro_fullscreen();
  }
  if (dog_fullscreen_mode) {
    exit_dog_fullscreen();
  }
  focus_mode = true;
  if (dog_sprite != nullptr && focus_dog_wrap != nullptr) {
    lv_obj_set_parent(dog_sprite, focus_dog_wrap);
    lv_obj_center(dog_sprite);
  }
  if (dock_panel != nullptr) {
    lv_obj_add_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  lv_obj_add_flag(focus_layer, LV_OBJ_FLAG_CLICKABLE);
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
  lv_obj_remove_flag(focus_layer, LV_OBJ_FLAG_CLICKABLE);
  if (dock_panel != nullptr) {
    lv_obj_remove_flag(dock_panel, LV_OBJ_FLAG_HIDDEN);
  }
  if (dog_sprite != nullptr && dog_sprite_wrap != nullptr) {
    lv_obj_set_parent(dog_sprite, dog_sprite_wrap);
    lv_obj_center(dog_sprite);
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
    lv_obj_add_flag(lock_layer, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(lock_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(lock_layer);
  } else {
    lv_obj_add_flag(lock_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(lock_layer, LV_OBJ_FLAG_CLICKABLE);
  }
}

void show_more(bool on) {
  if (more_layer == nullptr) {
    return;
  }
  if (on) {
    lv_obj_add_flag(more_layer, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(more_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(more_layer);
  } else {
    lv_obj_add_flag(more_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(more_layer, LV_OBJ_FLAG_CLICKABLE);
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
    lv_obj_add_flag(settings_layer, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_move_foreground(settings_layer);
  } else {
    lv_obj_add_flag(settings_layer, LV_OBJ_FLAG_HIDDEN);
    lv_obj_remove_flag(settings_layer, LV_OBJ_FLAG_CLICKABLE);
  }
}

void placeholder_toast(const char *name) {
  ESP_LOGI(TAG, "Open placeholder: %s", name);
}

void pomodoro_card_clicked(lv_event_t *e) {
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  static int64_t last_tap_us = 0;
  if (!ui_click_debounce(&last_tap_us)) {
    return;
  }
  if (pomo_touch_locked) {
    return;
  }
  if (pomodoro_is_operation_blocked()) {
    return;
  }
  ESP_LOGI(TAG, "Pomodoro card tapped");
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
  if (dog_fullscreen_mode) {
    exit_dog_fullscreen();
  }
  set_dock_selected(kDockIndexHome);
}

void dock_clicked(DockId id) {
  ESP_LOGI(TAG, "Dock tapped: %d", static_cast<int>(id));
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
    case DockId::PixelDog:
      enter_dog_fullscreen();
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
  if (lv_event_get_code(e) != LV_EVENT_CLICKED) {
    return;
  }
  static int64_t last_tap_us = 0;
  if (!ui_click_debounce(&last_tap_us)) {
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
  ui_tick_counter++;
  pomodoro_tick();
  dog_model_tick();
  refresh_status_bar();
  refresh_pomodoro_card();
  refresh_dog_card();
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
  app_power_tick(locked || focus_mode || pomo_fullscreen_mode || dog_fullscreen_mode || pomo_touch_locked);
}

void set_dock_selected(int index) {
  static const uint32_t kDockColors[] = {0x8bc34a, 0xff8a65, 0xce93d8, 0x42a5f5, 0x42a5f5};
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
  card_dog = w->card_dog;
  card_dog_inner = w->card_dog_inner;
  card_dog_border = w->card_dog_border;
  dog_sprite = w->dog_sprite;
  dog_sprite_wrap = w->dog_sprite_wrap;
  lbl_dog_speech = w->lbl_dog_speech;
  dog_pet_btn = w->dog_pet_btn;
  dog_greet_btn = w->dog_greet_btn;
  dog_nuzzle_btn = w->dog_nuzzle_btn;
  dog_hug_btn = w->dog_hug_btn;
  home_dog_nuzzle_btn = w->dog_nuzzle_btn;
  home_dog_hug_btn = w->dog_hug_btn;
  for (int i = 0; i < 5; i++) {
    dock_slots[i] = w->dock_slots[i];
    dock_borders[i] = w->dock_borders[i];
  }
  dock_panel = w->dock_panel;
  bind_home_click_targets();
}

const char *tool_button_border_asset_path(uint32_t accent_color, lv_coord_t btn_w, lv_coord_t btn_h) {
  if (btn_w != kPomoToolBtnW || btn_h != kPomoToolBtnH) {
    return nullptr;
  }
  if (accent_color == 0x8bc34a) {
    return SD_ASSET_BORDER_TOOL_BTN_GREEN;
  }
  if (accent_color == kDogAccentColor) {
    return SD_ASSET_BORDER_TOOL_BTN_ORANGE;
  }
  return nullptr;
}

lv_obj_t *create_tool_button_border_layer(lv_obj_t *btn, lv_coord_t btn_w, lv_coord_t btn_h, uint32_t accent_color,
                                          lv_event_cb_t on_press) {
  const char *asset = tool_button_border_asset_path(accent_color, btn_w, btn_h);
  if (asset != nullptr) {
    lv_obj_t *img = lv_image_create(btn);
    lv_obj_set_size(img, btn_w, btn_h);
    lv_obj_set_pos(img, 0, 0);
    lv_obj_remove_flag(img, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(img, LV_OBJ_FLAG_SCROLLABLE);
    if (assets_set_image_src(img, asset)) {
      if (on_press != nullptr) {
        attach_press_target(img, on_press);
      }
      lv_obj_move_foreground(img);
      return img;
    }
    lv_obj_delete(img);
  }

  lv_obj_t *border = pixel_create_jagged_border(btn, 0, 0, btn_w, btn_h, lv_color_hex(accent_color), UI_DOCK_BORDER_P,
                                                UI_DOCK_SEL_CORNER_INSET);
  if (border != nullptr) {
    attach_press_target(border, on_press);
    lv_obj_move_foreground(border);
    lv_obj_remove_flag(border, LV_OBJ_FLAG_SCROLLABLE);
  }
  return border;
}

lv_obj_t *create_tool_button(lv_obj_t *parent, const char *text, uint32_t accent_color, lv_event_cb_t on_press,
                             lv_obj_t **lbl_out, lv_coord_t btn_w = kPomoToolBtnW,
                             lv_coord_t btn_h = kPomoToolBtnH) {
  constexpr int kInnerPad = UI_DOCK_SEL_CORNER_INSET + UI_DOCK_BORDER_P;

  lv_obj_t *btn = lv_obj_create(parent);
  lv_obj_set_size(btn, btn_w, btn_h);
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

  const int inner_w = btn_w - kInnerPad * 2;
  const int inner_h = btn_h - kInnerPad * 2;

  lv_obj_t *inner = lv_obj_create(btn);
  lv_obj_set_pos(inner, kInnerPad, kInnerPad);
  lv_obj_set_size(inner, inner_w, inner_h);
  lv_obj_set_style_bg_opa(inner, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(inner, 0, 0);
  lv_obj_set_style_pad_all(inner, 0, 0);
  lv_obj_set_style_radius(inner, 0, 0);
  lv_obj_set_layout(inner, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(inner, LV_FLEX_FLOW_ROW);
  lv_obj_set_flex_align(inner, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  attach_press_target(inner, on_press);
  lv_obj_remove_flag(inner, LV_OBJ_FLAG_SCROLLABLE);

  lv_obj_t *lbl = lv_label_create(inner);
  lv_label_set_text(lbl, text);
  style_pixel_label(lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(accent_color));
  lv_obj_set_style_text_align(lbl, LV_TEXT_ALIGN_CENTER, 0);
  attach_press_target(lbl, on_press);
  if (lbl_out != nullptr) {
    *lbl_out = lbl;
  }

  create_tool_button_border_layer(btn, btn_w, btn_h, accent_color, on_press);
  return btn;
}

void layout_dog_fullscreen_buttons() {
  if (!dog_fullscreen_mode || card_dog == nullptr) {
    return;
  }
  if (dog_tool_row != nullptr) {
    lv_obj_align(dog_tool_row, LV_ALIGN_TOP_RIGHT, -UI_CARD_INNER_PAD, UI_CARD_INNER_PAD);
    lv_obj_move_foreground(dog_tool_row);
  }
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
  lv_obj_remove_flag(pomo_touch_blocker, LV_OBJ_FLAG_CLICKABLE);
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

void build_dog_fullscreen_buttons(lv_obj_t *card_parent) {
  if (card_parent == nullptr) {
    return;
  }
  dog_tool_row = lv_obj_create(card_parent);
  lv_obj_set_size(dog_tool_row, kPomoToolBtnW, kPomoToolBtnH);
  layout_abs(dog_tool_row);
  lv_obj_set_style_bg_opa(dog_tool_row, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(dog_tool_row, 0, 0);
  lv_obj_set_style_pad_all(dog_tool_row, 0, 0);
  lv_obj_remove_flag(dog_tool_row, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_remove_flag(dog_tool_row, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_flag(dog_tool_row, LV_OBJ_FLAG_HIDDEN);

  dog_exit_btn = create_tool_button(dog_tool_row, "\xe9\x80\x80\xe5\x87\xba", kDogAccentColor, dog_exit_btn_event,
                                    nullptr);
}

void build_home_content(lv_obj_t *parent) {
  ui_home_widgets_t widgets = {};
  dog_model_init();
  dog_sprite_init();
  ui_home_static_build(parent, &widgets);
  bind_home_widgets(&widgets);
  build_pomo_fullscreen_buttons(widgets.card_pomo);
  build_dog_fullscreen_buttons(widgets.card_dog);
  set_dock_selected(kDockIndexHome);
  refresh_status_bar();
  refresh_pomodoro_card();
  refresh_dog_card();
}

void build_focus_layer(lv_obj_t *parent) {
  focus_layer = lv_obj_create(parent);
  lv_obj_set_size(focus_layer, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(focus_layer, 0, 0);
  lv_obj_set_style_bg_color(focus_layer, lv_color_hex(0x08081a), 0);
  lv_obj_set_style_bg_opa(focus_layer, LV_OPA_COVER, 0);
  lv_obj_remove_flag(focus_layer, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_add_flag(focus_layer, LV_OBJ_FLAG_HIDDEN);
  lv_obj_remove_flag(focus_layer, LV_OBJ_FLAG_CLICKABLE);
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
  lv_obj_set_style_bg_opa(inner, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(inner, 0, 0);
  lv_obj_set_style_pad_all(inner, 16, 0);
  lv_obj_set_layout(inner, LV_LAYOUT_FLEX);
  lv_obj_set_flex_flow(inner, LV_FLEX_FLOW_COLUMN);
  lv_obj_set_flex_align(inner, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER, LV_FLEX_ALIGN_CENTER);
  lv_obj_set_style_pad_row(inner, 18, 0);
  lv_obj_remove_flag(inner, LV_OBJ_FLAG_SCROLLABLE);

  lv_obj_t *border = pixel_create_jagged_border(card, 0, 0, kFocusW, kFocusH, lv_color_hex(0x8bc34a),
                                                UI_CARD_BORDER_P, UI_CARD_CORNER_INSET);
  if (border != nullptr) {
    lv_obj_move_foreground(border);
    lv_obj_remove_flag(border, LV_OBJ_FLAG_CLICKABLE);
  }

  lv_obj_t *title = lv_label_create(inner);
  lv_label_set_text(title, "番茄钟 · 专注模式");
  style_pixel_label(title, &lv_font_cn_gb2312_16_0, lv_color_hex(0x8bc34a));

  focus_dog_wrap = lv_obj_create(inner);
  lv_obj_set_size(focus_dog_wrap, 140, 140);
  lv_obj_set_style_bg_opa(focus_dog_wrap, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(focus_dog_wrap, 0, 0);

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
  lv_obj_remove_flag(lock_layer, LV_OBJ_FLAG_CLICKABLE);

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
  lv_obj_remove_flag(more_layer, LV_OBJ_FLAG_CLICKABLE);

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
  lv_obj_remove_flag(settings_layer, LV_OBJ_FLAG_CLICKABLE);

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
  lv_obj_set_style_bg_color(scr_home, lv_color_hex(0x08081a), 0);
  lv_obj_set_style_bg_opa(scr_home, LV_OPA_COVER, 0);
  lv_obj_remove_flag(scr_home, LV_OBJ_FLAG_SCROLLABLE);
  lv_scr_load(scr_home);

  ESP_LOGI(TAG, "Building home content");
  build_home_content(scr_home);
  ESP_LOGI(TAG, "Building focus layer");
  build_focus_layer(scr_home);
  ESP_LOGI(TAG, "Building lock layer");
  build_lock_layer(scr_home);
  build_pomo_touch_blocker(scr_home);
  ESP_LOGI(TAG, "Building overlay layers");
  build_more_layer(scr_home);
  build_settings_layer(scr_home);
  ESP_LOGI(TAG, "Building dim overlay");

  dim_overlay = lv_obj_create(scr_home);
  lv_obj_set_size(dim_overlay, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(dim_overlay, 0, 0);
  lv_obj_set_style_bg_color(dim_overlay, lv_color_black(), 0);
  lv_obj_set_style_bg_opa(dim_overlay, LV_OPA_60, 0);
  lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(dim_overlay, global_activity_event, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_flag(dim_overlay, LV_OBJ_FLAG_HIDDEN);
  sync_idle_overlay_touch(false);

  ESP_LOGI(TAG, "Building sleep screen");
  scr_sleep = lv_obj_create(nullptr);
  lv_obj_set_style_bg_color(scr_sleep, lv_color_hex(0x000000), 0);
  lv_obj_add_flag(scr_sleep, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(scr_sleep, sleep_scr_event, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_flag(scr_sleep, LV_OBJ_FLAG_HIDDEN);

  app_power_bind_overlays(dim_overlay, scr_sleep);

  media_toast = lv_obj_create(scr_home);
  lv_obj_set_size(media_toast, 520, 44);
  lv_obj_align(media_toast, LV_ALIGN_BOTTOM_MID, 0, -110);
  layout_abs(media_toast);
  lv_obj_set_style_bg_color(media_toast, lv_color_hex(0x1a1a2e), 0);
  lv_obj_set_style_bg_opa(media_toast, LV_OPA_90, 0);
  lv_obj_set_style_border_color(media_toast, lv_color_hex(0x42a5f5), 0);
  lv_obj_set_style_border_width(media_toast, 2, 0);
  lv_obj_set_style_radius(media_toast, 0, 0);
  lv_obj_set_style_pad_all(media_toast, 8, 0);
  lv_obj_add_flag(media_toast, LV_OBJ_FLAG_HIDDEN);
  lv_obj_remove_flag(media_toast, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_t *media_toast_lbl = lv_label_create(media_toast);
  lv_label_set_text(media_toast_lbl, "");
  style_pixel_label(media_toast_lbl, &lv_font_cn_gb2312_16_0, lv_color_hex(0x42a5f5));
  lv_obj_center(media_toast_lbl);

  lv_timer_create(tick_cb, 1000, nullptr);
  display_unlock();

  refresh_dog_card();

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
