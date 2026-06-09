#include "modules/pomodoro_module.h"

#include <cstdio>
#include <cstring>
#include <string>

#include "display.h"
#include "esp_check.h"
#include "esp_log.h"
#include "app_shell.h"
#include "panel_config.h"
#include "pomodoro_api_client.h"
#include "ui_common.h"
#include "wifi_manager.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <lvgl.h>

extern "C" {
LV_FONT_DECLARE(font_chinese_20);
LV_FONT_DECLARE(font_chinese_28);
LV_FONT_DECLARE(lv_font_montserrat_36);
}

namespace {
constexpr char TAG[] = "pomodoro";
lv_obj_t *pomodoro_time_label = nullptr;
lv_obj_t *pomodoro_start_icon_label = nullptr;
lv_obj_t *pomodoro_status_label = nullptr;
lv_obj_t *pomodoro_face = nullptr;

constexpr uint32_t kPomodoroArcColorWork = 0xFF6B4A;
constexpr uint32_t kPomodoroArcColorBreak = 0x4A9EFF;
constexpr lv_coord_t kPomodoroRingThickness = 14;
constexpr lv_coord_t kPomodoroRingCornerRadius = 24;
constexpr lv_coord_t kPomodoroRingNominal = 360;
constexpr lv_coord_t kPomodoroCenterHitNominal = 240;
lv_obj_t *pomodoro_ring_clip = nullptr;
lv_obj_t *pomodoro_prog_top = nullptr;
lv_obj_t *pomodoro_prog_top_l = nullptr;
lv_obj_t *pomodoro_prog_right = nullptr;
lv_obj_t *pomodoro_prog_bottom = nullptr;
lv_obj_t *pomodoro_prog_left = nullptr;
lv_timer_t *pomodoro_lv_timer = nullptr;

lv_point_t pomodoro_press_point = {};
bool pomodoro_press_valid = false;

char pomodoro_time_cached[8] = "25:00";
char pomodoro_status_cached[64] = "";
int pomodoro_arc_pct_cached = -1;
int pomodoro_ring_render_pct = -1;
bool pomodoro_show_start_ui_cached = false;
uint32_t pomodoro_accent_cached = 0;

int pomodoro_progress_pct = 0;

constexpr int POMODORO_DEFAULT_WORK_SEC = 25 * 60;
constexpr int POMODORO_DEFAULT_SHORT_BREAK_SEC = 5 * 60;
constexpr int POMODORO_DEFAULT_LONG_BREAK_SEC = 15 * 60;

enum class PomodoroPhase { kWork, kShortBreak, kLongBreak };
enum class PomodoroRunState { kIdle, kRunning, kPaused };

int pomodoro_work_sec = POMODORO_DEFAULT_WORK_SEC;
int pomodoro_short_break_sec = POMODORO_DEFAULT_SHORT_BREAK_SEC;
int pomodoro_long_break_sec = POMODORO_DEFAULT_LONG_BREAK_SEC;
int pomodoro_rounds_before_long = 4;
int pomodoro_daily_goal_rounds = 8;
int pomodoro_daily_goal_minutes = 200;
int pomodoro_today_work_rounds = 0;
bool pomodoro_today_plan_done = false;
bool pomodoro_today_goal_notified = false;
int64_t pomodoro_plan_id = 0;
bool pomodoro_plan_loaded = false;

PomodoroPhase pomodoro_phase = PomodoroPhase::kWork;
PomodoroRunState pomodoro_run_state = PomodoroRunState::kIdle;
int pomodoro_remaining_sec = POMODORO_DEFAULT_WORK_SEC;
int pomodoro_phase_total_sec = POMODORO_DEFAULT_WORK_SEC;
int pomodoro_session_work_rounds = 0;
int pomodoro_sync_tick_counter = 0;
bool pomodoro_has_pending_phase = false;
PomodoroPhase pomodoro_pending_phase = PomodoroPhase::kShortBreak;

const char *pomodoro_phase_str_locked() {
  switch (pomodoro_phase) {
    case PomodoroPhase::kShortBreak:
      return "SHORT_BREAK";
    case PomodoroPhase::kLongBreak:
      return "LONG_BREAK";
    default:
      return "WORK";
  }
}

const char *pomodoro_run_state_str_locked() {
  switch (pomodoro_run_state) {
    case PomodoroRunState::kRunning:
      return "RUNNING";
    case PomodoroRunState::kPaused:
      return "PAUSED";
    default:
      return "IDLE";
  }
}

const char *pomodoro_pending_phase_str_locked() {
  switch (pomodoro_pending_phase) {
    case PomodoroPhase::kLongBreak:
      return "LONG_BREAK";
    case PomodoroPhase::kShortBreak:
      return "SHORT_BREAK";
    default:
      return "WORK";
  }
}

int pomodoro_phase_duration_sec_locked(PomodoroPhase phase) {
  switch (phase) {
    case PomodoroPhase::kLongBreak:
      return pomodoro_long_break_sec;
    case PomodoroPhase::kShortBreak:
      return pomodoro_short_break_sec;
    default:
      return pomodoro_work_sec;
  }
}

PomodoroPhase pomodoro_next_break_phase_locked() {
  if (pomodoro_rounds_before_long > 0 && pomodoro_session_work_rounds > 0 &&
      pomodoro_session_work_rounds % pomodoro_rounds_before_long == 0) {
    return PomodoroPhase::kLongBreak;
  }
  return PomodoroPhase::kShortBreak;
}

void pomodoro_apply_plan_config_locked(const PomodoroPlanConfig *cfg) {
  if (cfg == nullptr || !cfg->valid) {
    return;
  }
  pomodoro_plan_loaded = true;
  pomodoro_plan_id = cfg->plan_id;
  pomodoro_work_sec = cfg->work_sec;
  pomodoro_short_break_sec = cfg->short_break_sec;
  pomodoro_long_break_sec = cfg->long_break_sec;
  pomodoro_rounds_before_long = cfg->rounds_before_long_break;
  pomodoro_daily_goal_rounds = cfg->daily_goal_rounds;
  pomodoro_daily_goal_minutes = cfg->daily_goal_minutes;

  if (pomodoro_run_state == PomodoroRunState::kIdle && !pomodoro_has_pending_phase) {
    const int duration = pomodoro_phase_duration_sec_locked(pomodoro_phase);
    pomodoro_remaining_sec = duration;
    pomodoro_phase_total_sec = duration;
    pomodoro_progress_pct = 0;
    pomodoro_arc_pct_cached = -1;
  }
}

void pomodoro_apply_plan_durations_locked(const PomodoroPlanConfig *cfg) {
  if (cfg == nullptr || !cfg->valid) {
    return;
  }
  pomodoro_plan_loaded = true;
  pomodoro_plan_id = cfg->plan_id;
  pomodoro_work_sec = cfg->work_sec;
  pomodoro_short_break_sec = cfg->short_break_sec;
  pomodoro_long_break_sec = cfg->long_break_sec;
  pomodoro_rounds_before_long = cfg->rounds_before_long_break;
  pomodoro_daily_goal_rounds = cfg->daily_goal_rounds;
  pomodoro_daily_goal_minutes = cfg->daily_goal_minutes;
}

const char *pomodoro_status_text_locked();

bool pomodoro_today_rounds_goal_reached_locked() {
  return pomodoro_daily_goal_rounds > 0 &&
         pomodoro_today_work_rounds >= pomodoro_daily_goal_rounds;
}

/** 轮次已达标时禁止再开始新的专注/休息（进行中的暂停/继续不受影响） */
bool pomodoro_rounds_goal_blocks_new_phase_locked() {
  return pomodoro_today_rounds_goal_reached_locked() &&
         pomodoro_run_state == PomodoroRunState::kIdle;
}

void pomodoro_apply_today_plan_done_locked(bool done) {
  if (pomodoro_today_plan_done == done) {
    return;
  }
  pomodoro_today_plan_done = done;
  pomodoro_status_cached[0] = '\0';
  if (pomodoro_status_label != nullptr) {
    const char *status_text = pomodoro_status_text_locked();
    lv_label_set_text(pomodoro_status_label, status_text);
    std::strncpy(pomodoro_status_cached, status_text, sizeof(pomodoro_status_cached) - 1);
    pomodoro_status_cached[sizeof(pomodoro_status_cached) - 1] = '\0';
  }
}

void pomodoro_refresh_progress_from_state_locked() {
  if (pomodoro_has_pending_phase) {
    pomodoro_progress_pct = 100;
    pomodoro_arc_pct_cached = 100;
    pomodoro_ring_render_pct = -1;
    return;
  }

  if (pomodoro_phase_total_sec <= 0) {
    pomodoro_progress_pct = 0;
    pomodoro_arc_pct_cached = -1;
    pomodoro_ring_render_pct = -1;
    return;
  }

  if (pomodoro_run_state == PomodoroRunState::kRunning || pomodoro_run_state == PomodoroRunState::kPaused) {
    const int elapsed = pomodoro_phase_total_sec - pomodoro_remaining_sec;
    int pct = elapsed * 100 / pomodoro_phase_total_sec;
    if (pomodoro_run_state == PomodoroRunState::kRunning && pct > 98) {
      pct = 98;
    }
    pomodoro_progress_pct = pct > 100 ? 100 : pct;
    pomodoro_arc_pct_cached = -1;
    pomodoro_ring_render_pct = -1;
    return;
  }

  pomodoro_progress_pct = 0;
  pomodoro_arc_pct_cached = -1;
  pomodoro_ring_render_pct = -1;
}

void pomodoro_arm_idle_phase_timer_locked() {
  const int duration = pomodoro_phase_duration_sec_locked(pomodoro_phase);
  pomodoro_remaining_sec = duration;
  pomodoro_phase_total_sec = duration;
  pomodoro_progress_pct = 0;
  pomodoro_arc_pct_cached = -1;
}

void pomodoro_publish_state_locked(bool take_control) {
  if (take_control) {
    pomodoro_api_mark_local_control();
  }
  const char *pending = pomodoro_has_pending_phase ? pomodoro_pending_phase_str_locked() : nullptr;
  pomodoro_api_sync_session(
      pomodoro_phase_str_locked(),
      pomodoro_run_state_str_locked(),
      pomodoro_remaining_sec,
      pomodoro_phase_total_sec,
      pomodoro_session_work_rounds,
      pomodoro_plan_id,
      take_control,
      pending);
}

/** 待开始 / 阶段结束待确认：显示开始图标；倒计时中或暂停显示数字 */
bool pomodoro_show_start_icon_locked() {
  if (pomodoro_run_state != PomodoroRunState::kIdle) {
    return false;
  }
  if (pomodoro_rounds_goal_blocks_new_phase_locked()) {
    return false;
  }
  if (pomodoro_has_pending_phase) {
    return true;
  }
  return pomodoro_remaining_sec >= pomodoro_phase_total_sec;
}

void pomodoro_update_center_display_locked() {
  const bool show_start = pomodoro_show_start_icon_locked();
  if (show_start == pomodoro_show_start_ui_cached) {
    return;
  }
  pomodoro_show_start_ui_cached = show_start;
  if (pomodoro_time_label != nullptr) {
    if (show_start) {
      lv_obj_add_flag(pomodoro_time_label, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_clear_flag(pomodoro_time_label, LV_OBJ_FLAG_HIDDEN);
    }
  }
  if (pomodoro_start_icon_label != nullptr) {
    if (show_start) {
      lv_obj_clear_flag(pomodoro_start_icon_label, LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_add_flag(pomodoro_start_icon_label, LV_OBJ_FLAG_HIDDEN);
    }
  }
}

void format_mm_ss(int total_sec, char *buf, size_t buf_len) {
  if (total_sec < 0) {
    total_sec = 0;
  }
  const int minutes = total_sec / 60;
  const int seconds = total_sec % 60;
  snprintf(buf, buf_len, "%02d:%02d", minutes, seconds);
}

const char *pomodoro_status_text_locked() {
  if (pomodoro_today_plan_done) {
    return "今日任务已完成，可以放肆休息了";
  }
  if (pomodoro_today_rounds_goal_reached_locked() &&
      pomodoro_rounds_goal_blocks_new_phase_locked()) {
    return "今日轮次已达标，可以休息了";
  }
  if (pomodoro_has_pending_phase) {
    const bool pending_break =
        pomodoro_pending_phase == PomodoroPhase::kShortBreak ||
        pomodoro_pending_phase == PomodoroPhase::kLongBreak;
    return pending_break ? "专注完成，点按休息" : "休息完成，点按专注";
  }
  const bool is_work = pomodoro_phase == PomodoroPhase::kWork;
  switch (pomodoro_run_state) {
    case PomodoroRunState::kRunning:
      if (is_work) {
        return "专注中";
      }
      return pomodoro_phase == PomodoroPhase::kLongBreak ? "长休息中" : "休息中";
    case PomodoroRunState::kPaused:
      if (is_work) {
        return "专注 · 已暂停";
      }
      return pomodoro_phase == PomodoroPhase::kLongBreak ? "长休息 · 已暂停" : "休息 · 已暂停";
    default:
      return is_work ? "待开始专注" : "待开始休息";
  }
}

void pomodoro_apply_accent_locked(uint32_t accent_color) {
  pomodoro_accent_cached = accent_color;
  const lv_color_t accent = lv_color_hex(accent_color);
  if (pomodoro_prog_top) {
    lv_obj_set_style_bg_color(pomodoro_prog_top, accent, 0);
  }
  if (pomodoro_prog_top_l) {
    lv_obj_set_style_bg_color(pomodoro_prog_top_l, accent, 0);
  }
  if (pomodoro_prog_right) {
    lv_obj_set_style_bg_color(pomodoro_prog_right, accent, 0);
  }
  if (pomodoro_prog_bottom) {
    lv_obj_set_style_bg_color(pomodoro_prog_bottom, accent, 0);
  }
  if (pomodoro_prog_left) {
    lv_obj_set_style_bg_color(pomodoro_prog_left, accent, 0);
  }
}

void pomodoro_square_ring_hide_all_locked() {
  auto hide_bar = [](lv_obj_t *bar) {
    if (bar == nullptr) {
      return;
    }
    lv_obj_add_flag(bar, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_width(bar, 0);
    lv_obj_set_height(bar, 0);
  };
  hide_bar(pomodoro_prog_top);
  hide_bar(pomodoro_prog_top_l);
  hide_bar(pomodoro_prog_right);
  hide_bar(pomodoro_prog_bottom);
  hide_bar(pomodoro_prog_left);
}

void pomodoro_square_ring_set_progress_locked(int pct) {
  if (pomodoro_ring_clip == nullptr) {
    return;
  }
  if (pct < 0) {
    pct = 0;
  }
  if (pct > 100) {
    pct = 100;
  }
  if (pct == pomodoro_ring_render_pct) {
    pomodoro_progress_pct = pct;
    return;
  }
  pomodoro_ring_render_pct = pct;
  pomodoro_progress_pct = pct;

  const lv_coord_t w = lv_obj_get_width(pomodoro_ring_clip);
  const lv_coord_t h = lv_obj_get_height(pomodoro_ring_clip);
  if (w <= 0 || h <= 0) {
    return;
  }

  pomodoro_square_ring_hide_all_locked();

  if (pct <= 0) {
    return;
  }

  // 100% 用四边完整描边，避免分段算法在收尾时 top/top_l 重叠导致「方框崩溃」
  if (pct >= 100) {
    if (pomodoro_prog_top != nullptr) {
      lv_obj_clear_flag(pomodoro_prog_top, LV_OBJ_FLAG_HIDDEN);
      lv_obj_set_size(pomodoro_prog_top, w, kPomodoroRingThickness);
      lv_obj_set_pos(pomodoro_prog_top, 0, 0);
    }
    if (pomodoro_prog_right != nullptr) {
      lv_obj_clear_flag(pomodoro_prog_right, LV_OBJ_FLAG_HIDDEN);
      lv_obj_set_size(pomodoro_prog_right, kPomodoroRingThickness, h);
      lv_obj_set_pos(pomodoro_prog_right, w - kPomodoroRingThickness, 0);
    }
    if (pomodoro_prog_bottom != nullptr) {
      lv_obj_clear_flag(pomodoro_prog_bottom, LV_OBJ_FLAG_HIDDEN);
      lv_obj_set_size(pomodoro_prog_bottom, w, kPomodoroRingThickness);
      lv_obj_set_pos(pomodoro_prog_bottom, 0, h - kPomodoroRingThickness);
    }
    if (pomodoro_prog_left != nullptr) {
      lv_obj_clear_flag(pomodoro_prog_left, LV_OBJ_FLAG_HIDDEN);
      lv_obj_set_size(pomodoro_prog_left, kPomodoroRingThickness, h);
      lv_obj_set_pos(pomodoro_prog_left, 0, 0);
    }
    return;
  }

  const int32_t perim = 2 * (static_cast<int32_t>(w) + static_cast<int32_t>(h));
  int32_t remain = perim * pct / 100;
  if (remain <= 0) {
    return;
  }

  const lv_coord_t top_half = w / 2;
  const lv_coord_t center_x = w / 2;

  auto use_segment = [&](int32_t seg_len, auto &&apply) {
    if (remain <= 0 || seg_len <= 0) {
      return;
    }
    const int32_t use = remain > seg_len ? seg_len : remain;
    apply(static_cast<lv_coord_t>(use));
    remain -= use;
  };

  // 1) 上边：从中间向右
  use_segment(top_half, [&](lv_coord_t use) {
    lv_obj_clear_flag(pomodoro_prog_top, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomodoro_prog_top, use, kPomodoroRingThickness);
    lv_obj_set_pos(pomodoro_prog_top, center_x, 0);
  });
  // 2) 右边：向下
  use_segment(h, [&](lv_coord_t use) {
    lv_obj_clear_flag(pomodoro_prog_right, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomodoro_prog_right, kPomodoroRingThickness, use);
    lv_obj_set_pos(pomodoro_prog_right, w - kPomodoroRingThickness, 0);
  });
  // 3) 下边：从右向左
  use_segment(w, [&](lv_coord_t use) {
    lv_obj_clear_flag(pomodoro_prog_bottom, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomodoro_prog_bottom, use, kPomodoroRingThickness);
    lv_obj_set_pos(pomodoro_prog_bottom, w - use, h - kPomodoroRingThickness);
  });
  // 4) 左边：从下向上
  use_segment(h, [&](lv_coord_t use) {
    lv_obj_clear_flag(pomodoro_prog_left, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomodoro_prog_left, kPomodoroRingThickness, use);
    lv_obj_set_pos(pomodoro_prog_left, 0, h - use);
  });
  // 5) 上边：从左边回到中间
  use_segment(top_half, [&](lv_coord_t use) {
    lv_obj_clear_flag(pomodoro_prog_top_l, LV_OBJ_FLAG_HIDDEN);
    lv_obj_set_size(pomodoro_prog_top_l, use, kPomodoroRingThickness);
    lv_obj_set_pos(pomodoro_prog_top_l, center_x - use, 0);
  });
}

void update_pomodoro_tick_locked() {
  pomodoro_update_center_display_locked();

  char time_buf[8];
  format_mm_ss(pomodoro_remaining_sec, time_buf, sizeof(time_buf));

  if (!pomodoro_show_start_icon_locked() && pomodoro_time_label != nullptr &&
      std::strcmp(pomodoro_time_cached, time_buf) != 0) {
    lv_label_set_text(pomodoro_time_label, time_buf);
    std::strncpy(pomodoro_time_cached, time_buf, sizeof(pomodoro_time_cached));
    pomodoro_time_cached[sizeof(pomodoro_time_cached) - 1] = '\0';
  }

  if (pomodoro_phase_total_sec > 0 && pomodoro_run_state == PomodoroRunState::kRunning) {
    const int elapsed = pomodoro_phase_total_sec - pomodoro_remaining_sec;
    int pct = elapsed * 100 / pomodoro_phase_total_sec;
    if (pct > 98) {
      pct = 98;
    }
    const int clamped = pct > 100 ? 100 : pct;
    if (clamped != pomodoro_arc_pct_cached) {
      pomodoro_arc_pct_cached = clamped;
      pomodoro_progress_pct = clamped;
      pomodoro_square_ring_set_progress_locked(clamped);
    }
  }
}

void update_pomodoro_display_locked() {
  char time_buf[8];
  format_mm_ss(pomodoro_remaining_sec, time_buf, sizeof(time_buf));

  const bool is_work = pomodoro_phase == PomodoroPhase::kWork;
  const uint32_t accent_color = is_work ? kPomodoroArcColorWork : kPomodoroArcColorBreak;

  if (!pomodoro_show_start_icon_locked() && pomodoro_time_label != nullptr) {
    lv_label_set_text(pomodoro_time_label, time_buf);
    std::strncpy(pomodoro_time_cached, time_buf, sizeof(pomodoro_time_cached));
    pomodoro_time_cached[sizeof(pomodoro_time_cached) - 1] = '\0';
  }
  if (pomodoro_status_label != nullptr) {
    const char *status_text = pomodoro_status_text_locked();
    if (std::strcmp(status_text, pomodoro_status_cached) != 0) {
      lv_label_set_text(pomodoro_status_label, status_text);
      std::strncpy(pomodoro_status_cached, status_text, sizeof(pomodoro_status_cached) - 1);
      pomodoro_status_cached[sizeof(pomodoro_status_cached) - 1] = '\0';
    }
  }
  if (accent_color != pomodoro_accent_cached) {
    pomodoro_apply_accent_locked(accent_color);
  }
  update_pomodoro_tick_locked();
  pomodoro_update_center_display_locked();
  if (pomodoro_progress_pct != pomodoro_ring_render_pct) {
    pomodoro_square_ring_set_progress_locked(pomodoro_progress_pct);
  }
}

void pomodoro_enter_pending_phase_locked(PomodoroPhase next_phase) {
  const bool same_pending = pomodoro_has_pending_phase && pomodoro_pending_phase == next_phase &&
                            pomodoro_run_state == PomodoroRunState::kIdle &&
                            pomodoro_progress_pct == 100 && pomodoro_ring_render_pct == 100;

  pomodoro_has_pending_phase = true;
  pomodoro_pending_phase = next_phase;
  pomodoro_run_state = PomodoroRunState::kIdle;
  pomodoro_remaining_sec = 0;
  pomodoro_progress_pct = 100;
  pomodoro_arc_pct_cached = 100;
  if (pomodoro_lv_timer != nullptr) {
    lv_timer_pause(pomodoro_lv_timer);
  }

  if (same_pending) {
    return;
  }

  if (pomodoro_ring_render_pct != 100) {
    pomodoro_ring_render_pct = -1;
    pomodoro_square_ring_set_progress_locked(100);
  }
  pomodoro_show_start_ui_cached = !pomodoro_show_start_icon_locked();
  pomodoro_update_center_display_locked();
  if (pomodoro_status_label != nullptr) {
    const char *status_text = pomodoro_status_text_locked();
    if (std::strcmp(status_text, pomodoro_status_cached) != 0) {
      lv_label_set_text(pomodoro_status_label, status_text);
      std::strncpy(pomodoro_status_cached, status_text, sizeof(pomodoro_status_cached) - 1);
      pomodoro_status_cached[sizeof(pomodoro_status_cached) - 1] = '\0';
    }
  }
}

PomodoroPhase parse_remote_phase(const char *phase);

bool remote_session_is_pending(const PomodoroRemoteSession *remote) {
  if (remote == nullptr) {
    return false;
  }
  if (remote->pending_phase[0] != '\0') {
    return true;
  }
  if (remote->remaining_sec > 0) {
    return false;
  }
  if (std::strcmp(remote->phase, "IDLE") == 0) {
    return false;
  }
  if (std::strcmp(remote->run_state, "IDLE") == 0) {
    return true;
  }
  return std::strcmp(remote->run_state, "RUNNING") == 0 && remote->remaining_sec <= 0;
}

void pomodoro_apply_remote_pending_locked(const PomodoroRemoteSession *remote) {
  pomodoro_session_work_rounds = remote->session_work_rounds;

  PomodoroPhase next = parse_remote_phase(
      remote->pending_phase[0] != '\0' ? remote->pending_phase : remote->phase);
  if (remote->pending_phase[0] == '\0' && std::strcmp(remote->phase, "WORK") == 0) {
    next = pomodoro_next_break_phase_locked();
  }

  if (pomodoro_has_pending_phase && pomodoro_pending_phase == next &&
      pomodoro_run_state == PomodoroRunState::kIdle) {
    return;
  }

  pomodoro_enter_pending_phase_locked(next);
}

void pomodoro_start_pending_phase_locked() {
  pomodoro_has_pending_phase = false;
  pomodoro_phase = pomodoro_pending_phase;
  const int duration = pomodoro_phase_duration_sec_locked(pomodoro_phase);
  pomodoro_remaining_sec = duration;
  pomodoro_phase_total_sec = duration;
  pomodoro_run_state = PomodoroRunState::kRunning;
  pomodoro_progress_pct = 0;
  pomodoro_arc_pct_cached = -1;
  if (pomodoro_lv_timer != nullptr) {
    lv_timer_resume(pomodoro_lv_timer);
  }
}

void pomodoro_on_phase_complete_locked() {
  if (pomodoro_has_pending_phase || pomodoro_run_state != PomodoroRunState::kRunning) {
    return;
  }
  const int elapsed = pomodoro_phase_total_sec > 0 ? pomodoro_phase_total_sec : pomodoro_remaining_sec;
  if (pomodoro_phase == PomodoroPhase::kWork) {
    pomodoro_api_create_record("WORK", elapsed, pomodoro_plan_id, pomodoro_session_work_rounds + 1);
    ++pomodoro_session_work_rounds;
    pomodoro_enter_pending_phase_locked(pomodoro_next_break_phase_locked());
  } else {
    const char *break_type =
        pomodoro_phase == PomodoroPhase::kLongBreak ? "LONG_BREAK" : "SHORT_BREAK";
    pomodoro_api_create_record(break_type, elapsed, pomodoro_plan_id, 0);
    pomodoro_enter_pending_phase_locked(PomodoroPhase::kWork);
  }
  pomodoro_api_mark_local_control();
  pomodoro_publish_state_locked(true);
}

PomodoroPhase parse_remote_phase(const char *phase) {
  if (phase == nullptr) {
    return PomodoroPhase::kWork;
  }
  if (std::strcmp(phase, "LONG_BREAK") == 0) {
    return PomodoroPhase::kLongBreak;
  }
  if (std::strcmp(phase, "SHORT_BREAK") == 0) {
    return PomodoroPhase::kShortBreak;
  }
  return PomodoroPhase::kWork;
}

PomodoroRunState parse_remote_run_state(const char *run_state) {
  if (run_state != nullptr && std::strcmp(run_state, "RUNNING") == 0) {
    return PomodoroRunState::kRunning;
  }
  if (run_state != nullptr && std::strcmp(run_state, "PAUSED") == 0) {
    return PomodoroRunState::kPaused;
  }
  return PomodoroRunState::kIdle;
}

bool remote_session_matches_local_locked(const PomodoroRemoteSession *remote) {
  if (remote == nullptr || !remote->valid) {
    return true;
  }

  const bool remote_pending = remote_session_is_pending(remote);
  if (remote_pending != pomodoro_has_pending_phase) {
    return false;
  }
  if (remote_pending) {
    PomodoroPhase remote_next = parse_remote_phase(
        remote->pending_phase[0] != '\0' ? remote->pending_phase : remote->phase);
    if (remote->pending_phase[0] == '\0' && std::strcmp(remote->phase, "WORK") == 0) {
      remote_next = pomodoro_next_break_phase_locked();
    }
    if (remote_next != pomodoro_pending_phase) {
      return false;
    }
    if (pomodoro_session_work_rounds != remote->session_work_rounds) {
      return false;
    }
    return true;
  }

  if (std::strcmp(remote->run_state, "IDLE") == 0 && std::strcmp(remote->phase, "IDLE") == 0) {
    return !pomodoro_has_pending_phase && pomodoro_run_state == PomodoroRunState::kIdle &&
           pomodoro_phase == PomodoroPhase::kWork &&
           pomodoro_remaining_sec == pomodoro_work_sec;
  }

  if (parse_remote_phase(remote->phase) != pomodoro_phase) {
    return false;
  }
  if (parse_remote_run_state(remote->run_state) != pomodoro_run_state) {
    return false;
  }
  if (pomodoro_session_work_rounds != remote->session_work_rounds) {
    return false;
  }

  if (pomodoro_run_state == PomodoroRunState::kRunning) {
    const int diff = pomodoro_remaining_sec - remote->remaining_sec;
    return diff >= -3 && diff <= 3;
  }

  return pomodoro_remaining_sec == remote->remaining_sec;
}

void pomodoro_apply_remote_session_locked(const PomodoroRemoteSession *remote) {
  if (remote == nullptr || !remote->valid) {
    return;
  }
  if (remote_session_matches_local_locked(remote)) {
    return;
  }

  if (pomodoro_has_pending_phase) {
    if (std::strcmp(remote->run_state, "IDLE") == 0 && std::strcmp(remote->phase, "IDLE") == 0) {
      pomodoro_has_pending_phase = false;
      pomodoro_run_state = PomodoroRunState::kIdle;
      pomodoro_phase = PomodoroPhase::kWork;
      pomodoro_remaining_sec = pomodoro_work_sec;
      pomodoro_phase_total_sec = pomodoro_work_sec;
      if (pomodoro_lv_timer != nullptr) {
        lv_timer_pause(pomodoro_lv_timer);
      }
      update_pomodoro_display_locked();
      return;
    }
    if (remote_session_is_pending(remote)) {
      pomodoro_apply_remote_pending_locked(remote);
    }
    if (pomodoro_lv_timer != nullptr) {
      lv_timer_pause(pomodoro_lv_timer);
    }
    return;
  }

  if (std::strcmp(remote->run_state, "IDLE") == 0 && std::strcmp(remote->phase, "IDLE") == 0) {
    pomodoro_has_pending_phase = false;
    pomodoro_run_state = PomodoroRunState::kIdle;
    pomodoro_phase = PomodoroPhase::kWork;
    pomodoro_remaining_sec = pomodoro_work_sec;
    pomodoro_phase_total_sec = pomodoro_work_sec;
    if (pomodoro_lv_timer != nullptr) {
      lv_timer_pause(pomodoro_lv_timer);
    }
    update_pomodoro_display_locked();
    return;
  }

  if (remote_session_is_pending(remote)) {
    pomodoro_apply_remote_pending_locked(remote);
    if (pomodoro_lv_timer != nullptr) {
      lv_timer_pause(pomodoro_lv_timer);
    }
    pomodoro_refresh_progress_from_state_locked();
    update_pomodoro_display_locked();
    return;
  }

  pomodoro_has_pending_phase = false;
  pomodoro_phase = parse_remote_phase(remote->phase);
  pomodoro_run_state = parse_remote_run_state(remote->run_state);
  pomodoro_remaining_sec = remote->remaining_sec;
  pomodoro_phase_total_sec = remote->phase_total_sec > 0 ? remote->phase_total_sec : pomodoro_remaining_sec;
  pomodoro_session_work_rounds = remote->session_work_rounds;
  if (remote->plan_id > 0) {
    pomodoro_plan_id = remote->plan_id;
  }

  pomodoro_refresh_progress_from_state_locked();
  update_pomodoro_display_locked();

  if (pomodoro_lv_timer == nullptr) {
    return;
  }
  if (pomodoro_run_state == PomodoroRunState::kRunning && pomodoro_remaining_sec > 0) {
    lv_timer_resume(pomodoro_lv_timer);
  } else {
    lv_timer_pause(pomodoro_lv_timer);
  }
}

void pomodoro_apply_remote_async(void *param) {
  auto *remote = static_cast<PomodoroRemoteSession *>(param);
  if (remote == nullptr) {
    return;
  }
  display_lock();
  pomodoro_apply_remote_session_locked(remote);
  display_unlock();
  delete remote;
}

void pomodoro_on_remote_session(const PomodoroRemoteSession *remote) {
  if (remote == nullptr) {
    return;
  }
  auto *copy = new PomodoroRemoteSession(*remote);
  lv_async_call(pomodoro_apply_remote_async, copy);
}

void pomodoro_timer_cb(lv_timer_t *timer) {
  (void)timer;
  if (pomodoro_run_state != PomodoroRunState::kRunning) {
    return;
  }

  if (pomodoro_remaining_sec <= 0) {
    pomodoro_on_phase_complete_locked();
    return;
  }

  --pomodoro_remaining_sec;
  update_pomodoro_tick_locked();
  if (++pomodoro_sync_tick_counter >= 2) {
    pomodoro_sync_tick_counter = 0;
    pomodoro_publish_state_locked(false);
  }
}

void on_pomodoro_start_clicked(lv_event_t *event) {
  (void)event;

  display_lock();
  if (pomodoro_has_pending_phase) {
    if (pomodoro_rounds_goal_blocks_new_phase_locked()) {
      display_unlock();
      return;
    }
    display_unlock();
    pomodoro_module::request_start_pending_phase();
    return;
  }
  if (pomodoro_run_state == PomodoroRunState::kRunning) {
    pomodoro_run_state = PomodoroRunState::kPaused;
    if (pomodoro_lv_timer != nullptr) {
      lv_timer_pause(pomodoro_lv_timer);
    }
    update_pomodoro_display_locked();
    pomodoro_publish_state_locked(true);
    display_unlock();
    return;
  }
  if (pomodoro_run_state == PomodoroRunState::kPaused) {
    pomodoro_run_state = PomodoroRunState::kRunning;
    if (pomodoro_lv_timer != nullptr) {
      lv_timer_resume(pomodoro_lv_timer);
    }
    update_pomodoro_display_locked();
    pomodoro_publish_state_locked(true);
    display_unlock();
    return;
  }
  if (pomodoro_run_state == PomodoroRunState::kIdle) {
    if (pomodoro_rounds_goal_blocks_new_phase_locked()) {
      display_unlock();
      return;
    }
    display_unlock();
    pomodoro_module::request_start_work();
    return;
  }
  display_unlock();
}

void on_pomodoro_reset_clicked(lv_event_t *event) {
  (void)event;
  display_lock();
  pomodoro_has_pending_phase = false;
  pomodoro_run_state = PomodoroRunState::kIdle;
  if (pomodoro_phase != PomodoroPhase::kShortBreak &&
      pomodoro_phase != PomodoroPhase::kLongBreak) {
    pomodoro_phase = PomodoroPhase::kWork;
  }
  pomodoro_arm_idle_phase_timer_locked();
  pomodoro_sync_tick_counter = 0;
  if (pomodoro_lv_timer != nullptr) {
    lv_timer_pause(pomodoro_lv_timer);
  }
  update_pomodoro_display_locked();
  pomodoro_api_mark_local_control();
  pomodoro_api_sync_session(
      pomodoro_phase_str_locked(),
      "IDLE",
      pomodoro_remaining_sec,
      pomodoro_phase_total_sec,
      pomodoro_session_work_rounds,
      pomodoro_plan_id,
      true,
      nullptr);
  display_unlock();
}

void on_pomodoro_gesture(lv_event_t *event) {
  const lv_event_code_t code = lv_event_get_code(event);
  lv_indev_t *indev = lv_indev_active();
  if (indev == nullptr) {
    return;
  }

  if (code == LV_EVENT_PRESSED) {
    lv_indev_get_point(indev, &pomodoro_press_point);
    pomodoro_press_valid = true;
    app_shell::set_gesture_suppress_control_click(false);
    return;
  }

  if (code == LV_EVENT_PRESSING && pomodoro_press_valid) {
    lv_point_t cur = {};
    lv_indev_get_point(indev, &cur);
    const int dx = static_cast<int>(cur.x) - static_cast<int>(pomodoro_press_point.x);
    const int dy = static_cast<int>(cur.y) - static_cast<int>(pomodoro_press_point.y);
    if (LV_ABS(dx) >= app_shell::kGestureCancelThresholdPx ||
        LV_ABS(dy) >= app_shell::kGestureCancelThresholdPx) {
      app_shell::set_gesture_suppress_control_click(true);
    }
    return;
  }

  if (code == LV_EVENT_RELEASED || code == LV_EVENT_PRESS_LOST) {
    pomodoro_press_valid = false;
  }
}

bool pomodoro_press_moved_too_far(lv_indev_t *indev) {
  if (!pomodoro_press_valid || indev == nullptr) {
    return false;
  }
  lv_point_t cur = {};
  lv_indev_get_point(indev, &cur);
  const int dx = static_cast<int>(cur.x) - static_cast<int>(pomodoro_press_point.x);
  const int dy = static_cast<int>(cur.y) - static_cast<int>(pomodoro_press_point.y);
  return LV_ABS(dx) >= app_shell::kGestureCancelThresholdPx ||
         LV_ABS(dy) >= app_shell::kGestureCancelThresholdPx;
}

void on_pomodoro_center_event(lv_event_t *event) {
  const lv_event_code_t code = lv_event_get_code(event);
  lv_event_stop_bubbling(event);

  if (app_shell::gesture_suppress_control_click()) {
    if (code == LV_EVENT_SHORT_CLICKED || code == LV_EVENT_LONG_PRESSED ||
        code == LV_EVENT_RELEASED) {
      app_shell::set_gesture_suppress_control_click(false);
    }
    return;
  }

  if (code == LV_EVENT_SHORT_CLICKED) {
    on_pomodoro_start_clicked(event);
  } else if (code == LV_EVENT_LONG_PRESSED) {
    lv_indev_t *indev = lv_indev_active();
    if (pomodoro_press_moved_too_far(indev)) {
      return;
    }
    on_pomodoro_reset_clicked(event);
  }
}

void build_page(lv_obj_t *parent) {
  const lv_coord_t ring_w = static_cast<lv_coord_t>(PANEL_VISUAL_W(kPomodoroRingNominal));
  const lv_coord_t ring_h = static_cast<lv_coord_t>(PANEL_VISUAL_H(kPomodoroRingNominal));
  const lv_coord_t face_w = ring_w + 36;
  const lv_coord_t face_h = ring_h + 52;

  pomodoro_face = lv_obj_create(parent);
  lv_obj_set_size(pomodoro_face, face_w, face_h);
  lv_obj_set_style_bg_color(pomodoro_face, lv_color_hex(ui_common::kBgColor), 0);
  lv_obj_set_style_bg_opa(pomodoro_face, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(pomodoro_face, 0, 0);
  lv_obj_set_style_pad_all(pomodoro_face, 0, 0);
  lv_obj_set_style_radius(pomodoro_face, 0, 0);
  lv_obj_clear_flag(pomodoro_face, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_clear_flag(pomodoro_face, LV_OBJ_FLAG_OVERFLOW_VISIBLE);
  lv_obj_align(pomodoro_face, LV_ALIGN_CENTER, 0, -12);
  lv_obj_add_flag(pomodoro_face, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_event_cb(pomodoro_face, on_pomodoro_gesture, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_event_cb(pomodoro_face, on_pomodoro_gesture, LV_EVENT_PRESSING, nullptr);
  lv_obj_add_event_cb(pomodoro_face, on_pomodoro_gesture, LV_EVENT_RELEASED, nullptr);
  lv_obj_add_event_cb(pomodoro_face, on_pomodoro_gesture, LV_EVENT_PRESS_LOST, nullptr);

  // Rounded-square ring: clip container + arc (bg track + indicator share same path).
  pomodoro_ring_clip = lv_obj_create(pomodoro_face);
  lv_obj_set_size(pomodoro_ring_clip, ring_w, ring_h);
  ui_common::make_plain(pomodoro_ring_clip);
  lv_obj_set_style_bg_opa(pomodoro_ring_clip, LV_OPA_TRANSP, 0);
  lv_obj_set_style_radius(pomodoro_ring_clip, kPomodoroRingCornerRadius, 0);
  lv_obj_set_style_clip_corner(pomodoro_ring_clip, true, 0);
  lv_obj_clear_flag(pomodoro_ring_clip, LV_OBJ_FLAG_OVERFLOW_VISIBLE);
  lv_obj_clear_flag(pomodoro_ring_clip, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_clear_flag(pomodoro_ring_clip, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_align(pomodoro_ring_clip, LV_ALIGN_TOP_MID, 0, 0);

  // Gray rounded-square track
  lv_obj_t *track = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(track);
  lv_obj_set_size(track, ring_w, ring_h);
  lv_obj_set_style_radius(track, kPomodoroRingCornerRadius, 0);
  lv_obj_set_style_border_width(track, kPomodoroRingThickness, 0);
  lv_obj_set_style_border_color(track, lv_color_hex(0x333333), 0);
  lv_obj_set_style_border_opa(track, LV_OPA_COVER, 0);
  lv_obj_center(track);

  // Colored progress overlays (4 edges). Clip + radius makes them rounded-square, not a circle.
  const uint32_t accent = pomodoro_accent_cached != 0 ? pomodoro_accent_cached : kPomodoroArcColorWork;
  pomodoro_prog_top = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(pomodoro_prog_top);
  lv_obj_add_flag(pomodoro_prog_top, LV_OBJ_FLAG_HIDDEN);
  lv_obj_set_style_bg_color(pomodoro_prog_top, lv_color_hex(accent), 0);
  lv_obj_set_style_bg_opa(pomodoro_prog_top, LV_OPA_COVER, 0);

  pomodoro_prog_top_l = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(pomodoro_prog_top_l);
  lv_obj_add_flag(pomodoro_prog_top_l, LV_OBJ_FLAG_HIDDEN);
  lv_obj_set_style_bg_color(pomodoro_prog_top_l, lv_color_hex(accent), 0);
  lv_obj_set_style_bg_opa(pomodoro_prog_top_l, LV_OPA_COVER, 0);

  pomodoro_prog_right = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(pomodoro_prog_right);
  lv_obj_add_flag(pomodoro_prog_right, LV_OBJ_FLAG_HIDDEN);
  lv_obj_set_style_bg_color(pomodoro_prog_right, lv_color_hex(accent), 0);
  lv_obj_set_style_bg_opa(pomodoro_prog_right, LV_OPA_COVER, 0);

  pomodoro_prog_bottom = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(pomodoro_prog_bottom);
  lv_obj_add_flag(pomodoro_prog_bottom, LV_OBJ_FLAG_HIDDEN);
  lv_obj_set_style_bg_color(pomodoro_prog_bottom, lv_color_hex(accent), 0);
  lv_obj_set_style_bg_opa(pomodoro_prog_bottom, LV_OPA_COVER, 0);

  pomodoro_prog_left = lv_obj_create(pomodoro_ring_clip);
  ui_common::make_plain(pomodoro_prog_left);
  lv_obj_add_flag(pomodoro_prog_left, LV_OBJ_FLAG_HIDDEN);
  lv_obj_set_style_bg_color(pomodoro_prog_left, lv_color_hex(accent), 0);
  lv_obj_set_style_bg_opa(pomodoro_prog_left, LV_OPA_COVER, 0);

  lv_obj_update_layout(pomodoro_face);
  pomodoro_square_ring_set_progress_locked(0);

  lv_obj_t *text_stack = lv_obj_create(pomodoro_face);
  ui_common::make_plain(text_stack);
  lv_obj_set_width(text_stack, ring_w);
  lv_obj_set_height(text_stack, ring_h);
  lv_obj_clear_flag(text_stack, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_align_to(text_stack, pomodoro_ring_clip, LV_ALIGN_CENTER, 0, 0);

  pomodoro_time_label = lv_label_create(text_stack);
  lv_label_set_text(pomodoro_time_label, "25:00");
  lv_obj_set_style_text_font(pomodoro_time_label, &lv_font_montserrat_36, 0);
  lv_obj_set_style_text_color(pomodoro_time_label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_set_width(pomodoro_time_label, ring_w);
  lv_label_set_long_mode(pomodoro_time_label, LV_LABEL_LONG_MODE_CLIP);
  lv_obj_set_style_text_align(pomodoro_time_label, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_center(pomodoro_time_label);

  pomodoro_start_icon_label = lv_label_create(text_stack);
  lv_label_set_text(pomodoro_start_icon_label, LV_SYMBOL_PLAY);
  lv_obj_set_style_text_font(pomodoro_start_icon_label, &lv_font_montserrat_36, 0);
  lv_obj_set_style_text_color(pomodoro_start_icon_label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_set_style_text_opa(pomodoro_start_icon_label, LV_OPA_90, 0);
  lv_obj_center(pomodoro_start_icon_label);

  lv_obj_t *center_hit = lv_button_create(pomodoro_face);
  lv_obj_set_size(center_hit, static_cast<lv_coord_t>(PANEL_VISUAL_W(kPomodoroCenterHitNominal)),
                  static_cast<lv_coord_t>(PANEL_VISUAL_H(kPomodoroCenterHitNominal)));
  lv_obj_set_style_radius(center_hit, LV_RADIUS_CIRCLE, LV_PART_MAIN);
  lv_obj_set_style_bg_opa(center_hit, LV_OPA_TRANSP, LV_PART_MAIN);
  lv_obj_set_style_border_width(center_hit, 0, LV_PART_MAIN);
  lv_obj_set_style_shadow_width(center_hit, 0, LV_PART_MAIN);
  lv_obj_center(center_hit);
  lv_obj_add_event_cb(center_hit, on_pomodoro_gesture, LV_EVENT_PRESSED, nullptr);
  lv_obj_add_event_cb(center_hit, on_pomodoro_gesture, LV_EVENT_PRESSING, nullptr);
  lv_obj_add_event_cb(center_hit, on_pomodoro_gesture, LV_EVENT_RELEASED, nullptr);
  lv_obj_add_event_cb(center_hit, on_pomodoro_gesture, LV_EVENT_PRESS_LOST, nullptr);
  lv_obj_add_event_cb(center_hit, on_pomodoro_center_event, LV_EVENT_SHORT_CLICKED, nullptr);
  lv_obj_add_event_cb(center_hit, on_pomodoro_center_event, LV_EVENT_LONG_PRESSED, nullptr);
  lv_obj_move_foreground(center_hit);

  pomodoro_status_label = lv_label_create(pomodoro_face);
  lv_label_set_text(pomodoro_status_label, "待开始专注");
  lv_obj_set_style_text_font(pomodoro_status_label, &font_chinese_28, 0);
  lv_obj_set_style_text_color(pomodoro_status_label, lv_color_hex(0xFFFFFF), 0);
  lv_obj_set_style_text_opa(pomodoro_status_label, LV_OPA_80, 0);
  lv_obj_set_width(pomodoro_status_label, face_w);
  lv_obj_set_style_text_align(pomodoro_status_label, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_align_to(pomodoro_status_label, pomodoro_ring_clip, LV_ALIGN_OUT_BOTTOM_MID, 0, 4);

  pomodoro_arc_pct_cached = -1;
  pomodoro_accent_cached = 0;
  std::strncpy(pomodoro_time_cached, "25:00", sizeof(pomodoro_time_cached));
  update_pomodoro_display_locked();
}

bool bootstrap_running = false;
bool bootstrap_done = false;
bool today_goal_check_running = false;
bool start_with_plan_running = false;

bool is_today_plan_complete(int work_rounds, int work_minutes) {
  return work_rounds >= pomodoro_daily_goal_rounds && work_minutes >= pomodoro_daily_goal_minutes;
}

void today_goal_check_task(void *arg) {
  (void)arg;
  if (!wifi_is_connected()) {
    today_goal_check_running = false;
    vTaskDelete(nullptr);
    return;
  }

  PomodoroTodayStat stat = {};
  if (pomodoro_api_fetch_today_stat(&stat) != ESP_OK || !stat.valid) {
    today_goal_check_running = false;
    vTaskDelete(nullptr);
    return;
  }

  display_lock();
  const int prev_rounds = pomodoro_today_work_rounds;
  pomodoro_today_work_rounds = stat.work_rounds;
  const bool complete = is_today_plan_complete(stat.work_rounds, stat.work_minutes);
  if (complete && !pomodoro_today_goal_notified) {
    pomodoro_today_goal_notified = true;
    pomodoro_apply_today_plan_done_locked(true);
    ESP_LOGI(TAG, "Today plan completed: rounds=%d min=%d", stat.work_rounds, stat.work_minutes);
  } else if (!complete) {
    pomodoro_apply_today_plan_done_locked(false);
  }
  if (prev_rounds != pomodoro_today_work_rounds) {
    pomodoro_show_start_ui_cached = !pomodoro_show_start_icon_locked();
    pomodoro_status_cached[0] = '\0';
    pomodoro_update_center_display_locked();
    update_pomodoro_display_locked();
  }
  display_unlock();

  today_goal_check_running = false;
  vTaskDelete(nullptr);
}

enum class StartKind : int { kWork = 0, kPendingPhase = 1 };

bool begin_start_work_locked() {
  if (pomodoro_rounds_goal_blocks_new_phase_locked()) {
    return false;
  }
  if (pomodoro_run_state != PomodoroRunState::kIdle || pomodoro_has_pending_phase) {
    return false;
  }
  pomodoro_arm_idle_phase_timer_locked();
  pomodoro_run_state = PomodoroRunState::kRunning;
  if (pomodoro_lv_timer != nullptr) {
    lv_timer_resume(pomodoro_lv_timer);
  }
  return true;
}

bool begin_start_pending_locked() {
  if (pomodoro_rounds_goal_blocks_new_phase_locked()) {
    return false;
  }
  if (!pomodoro_has_pending_phase) {
    return false;
  }
  pomodoro_start_pending_phase_locked();
  return true;
}

void finish_start_and_publish_locked() {
  update_pomodoro_display_locked();
  pomodoro_publish_state_locked(true);
}

bool fetch_default_plan(PomodoroPlanConfig *cfg) {
  if (cfg == nullptr || !wifi_is_connected()) {
    return false;
  }
  return pomodoro_api_fetch_default_plan(cfg) == ESP_OK && cfg->valid;
}

void start_local_without_fetch_locked(StartKind kind) {
  const bool started =
      kind == StartKind::kPendingPhase ? begin_start_pending_locked() : begin_start_work_locked();
  if (started) {
    finish_start_and_publish_locked();
  }
}

}  // namespace

namespace pomodoro_module {

bool is_pending_phase() {
  display_lock();
  const bool pending = pomodoro_has_pending_phase;
  display_unlock();
  return pending;
}

bool remote_session_matches_local(const PomodoroRemoteSession *remote) {
  display_lock();
  const bool matches = remote_session_matches_local_locked(remote);
  display_unlock();
  return matches;
}

void check_today_goal_after_work_record() {
  if (today_goal_check_running) {
    return;
  }
  today_goal_check_running = true;
  if (xTaskCreate(today_goal_check_task, "pomo_goal", 6144, nullptr, 4, nullptr) != pdPASS) {
    today_goal_check_running = false;
  }
}

void bootstrap_task(void *arg);
void start_with_plan_task(void *arg);

void request_bootstrap() {
  if (bootstrap_running) {
    return;
  }
  bootstrap_running = true;
  if (xTaskCreate(bootstrap_task, "pomodoro_boot", 8192, nullptr, 4, nullptr) != pdPASS) {
    bootstrap_running = false;
  }
}

void request_plan_fetch() {
  request_bootstrap();
}

void request_start_work() {
  if (start_with_plan_running) {
    return;
  }
  display_lock();
  const bool blocked = pomodoro_rounds_goal_blocks_new_phase_locked();
  display_unlock();
  if (blocked) {
    return;
  }
  if (!wifi_is_connected()) {
    display_lock();
    start_local_without_fetch_locked(StartKind::kWork);
    display_unlock();
    return;
  }
  start_with_plan_running = true;
  if (xTaskCreate(
          start_with_plan_task,
          "pomo_start",
          6144,
          reinterpret_cast<void *>(static_cast<intptr_t>(StartKind::kWork)),
          4,
          nullptr) != pdPASS) {
    start_with_plan_running = false;
  }
}

void request_start_pending_phase() {
  if (start_with_plan_running) {
    return;
  }
  display_lock();
  const bool blocked = pomodoro_rounds_goal_blocks_new_phase_locked();
  display_unlock();
  if (blocked) {
    return;
  }
  if (!wifi_is_connected()) {
    display_lock();
    start_local_without_fetch_locked(StartKind::kPendingPhase);
    display_unlock();
    return;
  }
  start_with_plan_running = true;
  if (xTaskCreate(
          start_with_plan_task,
          "pomo_start",
          6144,
          reinterpret_cast<void *>(static_cast<intptr_t>(StartKind::kPendingPhase)),
          4,
          nullptr) != pdPASS) {
    start_with_plan_running = false;
  }
}

esp_err_t create(lv_obj_t *tile) {
  if (tile == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }
  ESP_ERROR_CHECK(pomodoro_api_client_init());
  pomodoro_api_set_remote_handler(pomodoro_on_remote_session);
  build_page(tile);
  pomodoro_lv_timer = lv_timer_create(pomodoro_timer_cb, 1000, nullptr);
  lv_timer_pause(pomodoro_lv_timer);
  return ESP_OK;
}

void bootstrap_task(void *arg) {
  (void)arg;

  for (int i = 0; i < 60 && !wifi_is_connected(); ++i) {
    vTaskDelay(pdMS_TO_TICKS(500));
  }

  PomodoroPlanConfig cfg = {};
  const bool has_plan = fetch_default_plan(&cfg);

  PomodoroRemoteSession session = {};
  const bool has_session =
      wifi_is_connected() && pomodoro_api_fetch_active_session(&session) == ESP_OK && session.valid;

  display_lock();
  if (has_plan) {
    if (has_session) {
      pomodoro_apply_plan_durations_locked(&cfg);
    } else {
      pomodoro_apply_plan_config_locked(&cfg);
    }
  }
  if (has_session) {
    pomodoro_apply_remote_session_locked(&session);
    pomodoro_api_set_last_applied_synced_ms(session.synced_at_ms);
    ESP_LOGI(
        TAG,
        "Restored session phase=%s state=%s remain=%ds",
        session.phase,
        session.run_state,
        session.remaining_sec);
  } else if (has_plan) {
    ESP_LOGI(TAG, "No active session, using default plan idle UI");
  }
  if (has_plan && wifi_is_connected()) {
    check_today_goal_after_work_record();
  }
  update_pomodoro_display_locked();
  display_unlock();

  bootstrap_done = true;
  bootstrap_running = false;
  vTaskDelete(nullptr);
}

void start_with_plan_task(void *arg) {
  const auto kind = static_cast<StartKind>(reinterpret_cast<intptr_t>(arg));

  PomodoroPlanConfig cfg = {};
  const bool fetched = fetch_default_plan(&cfg);

  display_lock();
  if (fetched) {
    pomodoro_apply_plan_config_locked(&cfg);
  }

  const bool started =
      kind == StartKind::kPendingPhase ? begin_start_pending_locked() : begin_start_work_locked();
  if (started) {
    finish_start_and_publish_locked();
  }
  display_unlock();

  start_with_plan_running = false;
  vTaskDelete(nullptr);
}

void on_show() {
  if (!bootstrap_done) {
    request_bootstrap();
  }
}

void on_hide() {
  // 切到其它界面时番茄钟继续计时，不自动暂停
}

}  // namespace pomodoro_module
