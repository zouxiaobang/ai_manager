#pragma once

#include <cstdint>

enum class PomodoroPhase { Idle, Focus, ShortBreak, LongBreak };

enum class PomodoroPendingPhase { None, Work, ShortBreak, LongBreak };

/** 副屏可触发的 6 种用户动作 */
enum class PomodoroUserAction {
  StartFocus,
  PauseFocus,
  StartShortBreak,
  PauseShortBreak,
  StartLongBreak,
  PauseLongBreak,
};

struct PomodoroPlanConfig {
  int64_t plan_id = 0;
  int work_duration_min = 25;
  int short_break_min = 5;
  int long_break_min = 15;
  int rounds_before_long_break = 4;
  int daily_goal_rounds = 0;
};

struct PomodoroSnapshot {
  PomodoroPhase phase = PomodoroPhase::Idle;
  bool running = false;
  int remaining_sec = 0;
  int total_sec = 25 * 60;
  int session_work_rounds = 0;
  int today_work_rounds = 0;
  int64_t plan_id = 0;
  PomodoroPendingPhase pending = PomodoroPendingPhase::None;
  bool backend_connected = false;
  bool today_goal_done = false;
};

struct PomodoroRemoteSession {
  PomodoroPhase phase = PomodoroPhase::Idle;
  bool running = false;
  bool run_state_idle = true;
  int remaining_sec = 0;
  int phase_total_sec = 25 * 60;
  int session_work_rounds = 0;
  int64_t plan_id = 0;
  PomodoroPendingPhase pending = PomodoroPendingPhase::None;
  int64_t synced_at_ms = 0;
  bool controller_is_device = false;
  bool valid = false;
};

struct PomodoroSyncPayload {
  const char *phase;
  const char *run_state;
  int remaining_sec;
  int phase_total_sec;
  int session_work_rounds;
  int64_t plan_id;
  const char *pending_phase;
  bool take_control;
};

void pomodoro_init();
void pomodoro_tick();
PomodoroSnapshot pomodoro_get();
PomodoroPlanConfig pomodoro_get_plan();

void pomodoro_apply_plan(const PomodoroPlanConfig &plan);
void pomodoro_set_today_work_rounds(int rounds);
void pomodoro_set_backend_connected(bool connected);

/** 今日轮次已满：显示结束态且禁止操作 */
bool pomodoro_is_today_goal_done();
bool pomodoro_is_operation_blocked();

/** 当前阶段任务已结束（专注/休息/今日任务），专注模式可退出 */
bool pomodoro_is_current_task_complete();

/** 卡片点击：按当前阶段切换开始/暂停（6 种动作之一） */
void pomodoro_card_action();
void pomodoro_apply_user_action(PomodoroUserAction action);
void pomodoro_reset();

/** 专注完成待上报记录；返回 true 时 *duration_sec 为有效秒数 */
bool pomodoro_consume_work_record_request(int *duration_sec);

bool pomodoro_is_active_on_lock();
bool pomodoro_build_sync_payload(PomodoroSyncPayload *out);
void pomodoro_apply_remote_session(const PomodoroRemoteSession &remote, bool force);
void pomodoro_mark_sync_dirty(bool take_control);
bool pomodoro_consume_sync_dirty(bool *take_control_out);
int64_t pomodoro_last_applied_sync_ms();
