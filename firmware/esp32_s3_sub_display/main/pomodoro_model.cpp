#include "pomodoro_model.h"

#include <algorithm>
#include <mutex>

namespace {
std::mutex g_mu;
PomodoroSnapshot g_state = {};
PomodoroPlanConfig g_plan = {};
int64_t g_last_applied_sync_ms = 0;
bool g_sync_dirty = false;
bool g_take_control = false;
bool g_need_work_record = false;
int g_work_record_duration_sec = 0;

bool is_today_goal_reached_locked(int today_rounds) {
  return g_plan.daily_goal_rounds > 0 && today_rounds >= g_plan.daily_goal_rounds;
}

void recompute_today_goal_done_locked() {
  g_state.today_goal_done = is_today_goal_reached_locked(g_state.today_work_rounds);
}

void enter_today_done_locked() {
  g_state.phase = PomodoroPhase::Idle;
  g_state.pending = PomodoroPendingPhase::None;
  g_state.running = false;
  g_state.remaining_sec = 0;
  g_state.today_goal_done = true;
}

int sec_from_min(int min) {
  return std::max(1, min) * 60;
}

int phase_total_sec(PomodoroPhase phase) {
  switch (phase) {
    case PomodoroPhase::Focus:
      return sec_from_min(g_plan.work_duration_min);
    case PomodoroPhase::ShortBreak:
      return sec_from_min(g_plan.short_break_min);
    case PomodoroPhase::LongBreak:
      return sec_from_min(g_plan.long_break_min);
    default:
      return sec_from_min(g_plan.work_duration_min);
  }
}

PomodoroPendingPhase next_pending_after_work() {
  const int rounds = g_state.session_work_rounds + 1;
  if (g_plan.rounds_before_long_break > 0 &&
      rounds % g_plan.rounds_before_long_break == 0) {
    return PomodoroPendingPhase::LongBreak;
  }
  return PomodoroPendingPhase::ShortBreak;
}

void mark_dirty_locked(bool take_control) {
  g_sync_dirty = true;
  if (take_control) {
    g_take_control = true;
  }
}

void enter_phase_locked(PomodoroPhase phase) {
  g_state.phase = phase;
  g_state.total_sec = phase_total_sec(phase);
  g_state.remaining_sec = g_state.total_sec;
  g_state.running = true;
  g_state.pending = PomodoroPendingPhase::None;
}

void start_pending_locked() {
  switch (g_state.pending) {
    case PomodoroPendingPhase::Work:
      enter_phase_locked(PomodoroPhase::Focus);
      break;
    case PomodoroPendingPhase::ShortBreak:
      enter_phase_locked(PomodoroPhase::ShortBreak);
      break;
    case PomodoroPendingPhase::LongBreak:
      enter_phase_locked(PomodoroPhase::LongBreak);
      break;
    default:
      enter_phase_locked(PomodoroPhase::Focus);
      break;
  }
}

void on_phase_complete_locked() {
  g_state.running = false;
  if (g_state.phase == PomodoroPhase::Focus) {
    g_state.session_work_rounds += 1;
    g_work_record_duration_sec = g_state.total_sec > 0 ? g_state.total_sec : phase_total_sec(PomodoroPhase::Focus);
    g_need_work_record = true;
    const int next_today = g_state.today_work_rounds + 1;
    if (is_today_goal_reached_locked(next_today)) {
      g_state.today_work_rounds = next_today;
      enter_today_done_locked();
      return;
    }
    g_state.pending = next_pending_after_work();
    g_state.remaining_sec = 0;
    return;
  }
  if (g_state.phase == PomodoroPhase::ShortBreak || g_state.phase == PomodoroPhase::LongBreak) {
    if (is_today_goal_reached_locked(g_state.today_work_rounds)) {
      enter_today_done_locked();
      return;
    }
    g_state.pending = PomodoroPendingPhase::Work;
    g_state.remaining_sec = 0;
    return;
  }
  g_state.phase = PomodoroPhase::Idle;
  g_state.remaining_sec = sec_from_min(g_plan.work_duration_min);
  g_state.total_sec = g_state.remaining_sec;
}

void apply_user_action_locked(PomodoroUserAction action) {
  if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
    return;
  }
  switch (action) {
    case PomodoroUserAction::StartFocus:
      if (g_state.phase == PomodoroPhase::Idle ||
          (g_state.pending == PomodoroPendingPhase::Work && !g_state.running &&
           g_state.remaining_sec <= 0)) {
        if (g_state.pending == PomodoroPendingPhase::Work) {
          start_pending_locked();
        } else if (g_state.phase == PomodoroPhase::Idle &&
                   g_state.pending != PomodoroPendingPhase::None) {
          start_pending_locked();
        } else {
          enter_phase_locked(PomodoroPhase::Focus);
        }
      } else if (g_state.phase == PomodoroPhase::Focus) {
        g_state.running = true;
      }
      break;
    case PomodoroUserAction::PauseFocus:
      if (g_state.phase == PomodoroPhase::Focus) {
        g_state.running = false;
      }
      break;
    case PomodoroUserAction::StartShortBreak:
      if (g_state.phase == PomodoroPhase::ShortBreak) {
        g_state.running = true;
      } else if (g_state.pending == PomodoroPendingPhase::ShortBreak) {
        enter_phase_locked(PomodoroPhase::ShortBreak);
      }
      break;
    case PomodoroUserAction::PauseShortBreak:
      if (g_state.phase == PomodoroPhase::ShortBreak) {
        g_state.running = false;
      }
      break;
    case PomodoroUserAction::StartLongBreak:
      if (g_state.phase == PomodoroPhase::LongBreak) {
        g_state.running = true;
      } else if (g_state.pending == PomodoroPendingPhase::LongBreak) {
        enter_phase_locked(PomodoroPhase::LongBreak);
      }
      break;
    case PomodoroUserAction::PauseLongBreak:
      if (g_state.phase == PomodoroPhase::LongBreak) {
        g_state.running = false;
      }
      break;
  }
  mark_dirty_locked(true);
}
}  // namespace

void pomodoro_init() {
  std::lock_guard<std::mutex> lock(g_mu);
  g_plan = {};
  g_state.phase = PomodoroPhase::Idle;
  g_state.running = false;
  g_state.session_work_rounds = 0;
  g_state.pending = PomodoroPendingPhase::None;
  g_state.remaining_sec = sec_from_min(g_plan.work_duration_min);
  g_state.total_sec = g_state.remaining_sec;
  g_state.plan_id = g_plan.plan_id;
  g_state.today_work_rounds = 0;
  g_state.today_goal_done = false;
  g_last_applied_sync_ms = 0;
  g_sync_dirty = false;
  g_take_control = false;
  g_need_work_record = false;
  g_work_record_duration_sec = 0;
}

void pomodoro_apply_plan(const PomodoroPlanConfig &plan) {
  std::lock_guard<std::mutex> lock(g_mu);
  g_plan = plan;
  g_state.plan_id = plan.plan_id;
  recompute_today_goal_done_locked();
  if (g_state.today_goal_done) {
    enter_today_done_locked();
    return;
  }
  if (g_state.phase == PomodoroPhase::Idle && g_state.pending == PomodoroPendingPhase::None &&
      !g_state.running) {
    g_state.remaining_sec = sec_from_min(g_plan.work_duration_min);
    g_state.total_sec = g_state.remaining_sec;
  }
}

PomodoroPlanConfig pomodoro_get_plan() {
  std::lock_guard<std::mutex> lock(g_mu);
  return g_plan;
}

void pomodoro_set_today_work_rounds(int rounds) {
  std::lock_guard<std::mutex> lock(g_mu);
  g_state.today_work_rounds = std::max(0, rounds);
  if (is_today_goal_reached_locked(g_state.today_work_rounds)) {
    enter_today_done_locked();
  } else {
    g_state.today_goal_done = false;
  }
}

void pomodoro_set_backend_connected(bool connected) {
  std::lock_guard<std::mutex> lock(g_mu);
  g_state.backend_connected = connected;
}

void pomodoro_tick() {
  std::lock_guard<std::mutex> lock(g_mu);
  if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
    return;
  }
  if (!g_state.running || g_state.phase == PomodoroPhase::Idle) {
    return;
  }
  if (g_state.remaining_sec > 0) {
    g_state.remaining_sec--;
    return;
  }
  on_phase_complete_locked();
  mark_dirty_locked(true);
}

PomodoroSnapshot pomodoro_get() {
  std::lock_guard<std::mutex> lock(g_mu);
  return g_state;
}

void pomodoro_apply_user_action(PomodoroUserAction action) {
  std::lock_guard<std::mutex> lock(g_mu);
  apply_user_action_locked(action);
}

void pomodoro_card_action() {
  std::lock_guard<std::mutex> lock(g_mu);
  if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
    return;
  }
  if (!g_state.running && g_state.pending != PomodoroPendingPhase::None &&
      g_state.remaining_sec <= 0) {
    if (g_state.pending == PomodoroPendingPhase::ShortBreak) {
      apply_user_action_locked(PomodoroUserAction::StartShortBreak);
    } else if (g_state.pending == PomodoroPendingPhase::LongBreak) {
      apply_user_action_locked(PomodoroUserAction::StartLongBreak);
    } else {
      apply_user_action_locked(PomodoroUserAction::StartFocus);
    }
    return;
  }
  if (g_state.phase == PomodoroPhase::Idle) {
    if (g_state.pending == PomodoroPendingPhase::ShortBreak) {
      apply_user_action_locked(PomodoroUserAction::StartShortBreak);
    } else if (g_state.pending == PomodoroPendingPhase::LongBreak) {
      apply_user_action_locked(PomodoroUserAction::StartLongBreak);
    } else if (g_state.pending == PomodoroPendingPhase::Work) {
      apply_user_action_locked(PomodoroUserAction::StartFocus);
    } else {
      apply_user_action_locked(PomodoroUserAction::StartFocus);
    }
    return;
  }
  if (g_state.phase == PomodoroPhase::Focus) {
    apply_user_action_locked(g_state.running ? PomodoroUserAction::PauseFocus
                                             : PomodoroUserAction::StartFocus);
    return;
  }
  if (g_state.phase == PomodoroPhase::ShortBreak) {
    apply_user_action_locked(g_state.running ? PomodoroUserAction::PauseShortBreak
                                             : PomodoroUserAction::StartShortBreak);
    return;
  }
  if (g_state.phase == PomodoroPhase::LongBreak) {
    apply_user_action_locked(g_state.running ? PomodoroUserAction::PauseLongBreak
                                             : PomodoroUserAction::StartLongBreak);
  }
}

void pomodoro_reset() {
  std::lock_guard<std::mutex> lock(g_mu);
  g_state.running = false;
  g_state.phase = PomodoroPhase::Idle;
  g_state.pending = PomodoroPendingPhase::None;
  g_state.session_work_rounds = 0;
  g_state.remaining_sec = sec_from_min(g_plan.work_duration_min);
  g_state.total_sec = g_state.remaining_sec;
  mark_dirty_locked(true);
}

bool pomodoro_is_today_goal_done() {
  std::lock_guard<std::mutex> lock(g_mu);
  return g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds);
}

bool pomodoro_is_operation_blocked() {
  return pomodoro_is_today_goal_done();
}

bool pomodoro_is_current_task_complete() {
  std::lock_guard<std::mutex> lock(g_mu);
  if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
    return true;
  }
  if (!g_state.running && g_state.pending != PomodoroPendingPhase::None && g_state.remaining_sec <= 0) {
    return true;
  }
  return false;
}

bool pomodoro_consume_work_record_request(int *duration_sec) {
  std::lock_guard<std::mutex> lock(g_mu);
  if (!g_need_work_record) {
    return false;
  }
  g_need_work_record = false;
  if (duration_sec != nullptr) {
    *duration_sec = g_work_record_duration_sec;
  }
  return true;
}

bool pomodoro_is_active_on_lock() {
  std::lock_guard<std::mutex> lock(g_mu);
  return g_state.phase != PomodoroPhase::Idle &&
         (g_state.running || g_state.remaining_sec < g_state.total_sec);
}

bool pomodoro_build_sync_payload(PomodoroSyncPayload *out) {
  if (out == nullptr) {
    return false;
  }
  std::lock_guard<std::mutex> lock(g_mu);

  static const char *kPhaseIdle = "IDLE";
  static const char *kPhaseWork = "WORK";
  static const char *kPhaseShort = "SHORT_BREAK";
  static const char *kPhaseLong = "LONG_BREAK";
  static const char *kRunIdle = "IDLE";
  static const char *kRunRunning = "RUNNING";
  static const char *kRunPaused = "PAUSED";
  static const char *kPendingWork = "WORK";
  static const char *kPendingShort = "SHORT_BREAK";
  static const char *kPendingLong = "LONG_BREAK";

  out->plan_id = g_state.plan_id > 0 ? g_state.plan_id : g_plan.plan_id;
  out->session_work_rounds = g_state.session_work_rounds;
  out->take_control = g_take_control;
  out->pending_phase = nullptr;

  if (g_state.phase == PomodoroPhase::Idle && g_state.pending == PomodoroPendingPhase::None) {
    const int total = sec_from_min(g_plan.work_duration_min);
    out->phase = kPhaseIdle;
    out->run_state = kRunIdle;
    if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
      out->remaining_sec = 0;
      out->phase_total_sec = total;
    } else {
      out->remaining_sec = total;
      out->phase_total_sec = total;
    }
    return true;
  }

  if (g_state.pending != PomodoroPendingPhase::None) {
    switch (g_state.phase) {
      case PomodoroPhase::Focus:
        out->phase = kPhaseWork;
        break;
      case PomodoroPhase::ShortBreak:
        out->phase = kPhaseShort;
        break;
      case PomodoroPhase::LongBreak:
        out->phase = kPhaseLong;
        break;
      default:
        out->phase = kPhaseIdle;
        break;
    }
    out->run_state = kRunIdle;
    out->remaining_sec = 0;
    out->phase_total_sec = g_state.total_sec > 0 ? g_state.total_sec : phase_total_sec(g_state.phase);
    switch (g_state.pending) {
      case PomodoroPendingPhase::Work:
        out->pending_phase = kPendingWork;
        break;
      case PomodoroPendingPhase::ShortBreak:
        out->pending_phase = kPendingShort;
        break;
      case PomodoroPendingPhase::LongBreak:
        out->pending_phase = kPendingLong;
        break;
      default:
        break;
    }
    return true;
  }

  switch (g_state.phase) {
    case PomodoroPhase::Focus:
      out->phase = kPhaseWork;
      break;
    case PomodoroPhase::ShortBreak:
      out->phase = kPhaseShort;
      break;
    case PomodoroPhase::LongBreak:
      out->phase = kPhaseLong;
      break;
    default:
      out->phase = kPhaseIdle;
      break;
  }
  out->run_state = g_state.running ? kRunRunning : kRunPaused;
  out->remaining_sec = std::max(0, g_state.remaining_sec);
  out->phase_total_sec = g_state.total_sec > 0 ? g_state.total_sec : phase_total_sec(g_state.phase);
  return true;
}

void pomodoro_apply_remote_session(const PomodoroRemoteSession &remote, bool force) {
  std::lock_guard<std::mutex> lock(g_mu);
  if (!remote.valid) {
    return;
  }
  if (g_state.today_goal_done || is_today_goal_reached_locked(g_state.today_work_rounds)) {
    enter_today_done_locked();
    g_last_applied_sync_ms = remote.synced_at_ms;
    return;
  }
  if (!force && remote.synced_at_ms > 0 && remote.synced_at_ms <= g_last_applied_sync_ms) {
    return;
  }

  g_state.session_work_rounds = remote.session_work_rounds;
  if (remote.plan_id > 0) {
    g_state.plan_id = remote.plan_id;
  }

  if (remote.run_state_idle && remote.phase == PomodoroPhase::Idle &&
      remote.pending == PomodoroPendingPhase::None) {
    g_state.phase = PomodoroPhase::Idle;
    g_state.running = false;
    g_state.pending = PomodoroPendingPhase::None;
    g_state.remaining_sec = sec_from_min(g_plan.work_duration_min);
    g_state.total_sec = g_state.remaining_sec;
    g_last_applied_sync_ms = remote.synced_at_ms;
    return;
  }

  if (remote.run_state_idle && remote.pending != PomodoroPendingPhase::None) {
    g_state.phase = remote.phase;
    g_state.pending = remote.pending;
    g_state.running = false;
    g_state.remaining_sec = 0;
    g_state.total_sec = remote.phase_total_sec > 0 ? remote.phase_total_sec : phase_total_sec(remote.phase);
    g_last_applied_sync_ms = remote.synced_at_ms;
    return;
  }

  g_state.phase = remote.phase;
  g_state.pending = PomodoroPendingPhase::None;
  g_state.running = remote.running;
  g_state.total_sec = remote.phase_total_sec > 0 ? remote.phase_total_sec : phase_total_sec(remote.phase);
  g_state.remaining_sec = std::max(0, remote.remaining_sec);
  g_last_applied_sync_ms = remote.synced_at_ms;
}

void pomodoro_mark_sync_dirty(bool take_control) {
  std::lock_guard<std::mutex> lock(g_mu);
  mark_dirty_locked(take_control);
}

bool pomodoro_consume_sync_dirty(bool *take_control_out) {
  std::lock_guard<std::mutex> lock(g_mu);
  const bool dirty = g_sync_dirty;
  if (take_control_out != nullptr) {
    *take_control_out = g_take_control;
  }
  g_sync_dirty = false;
  g_take_control = false;
  return dirty;
}

int64_t pomodoro_last_applied_sync_ms() {
  std::lock_guard<std::mutex> lock(g_mu);
  return g_last_applied_sync_ms;
}
