#pragma once

#include <cstdint>

#include "esp_err.h"

typedef struct {
  char phase[20];
  char run_state[16];
  char pending_phase[20];
  int remaining_sec;
  int phase_total_sec;
  int session_work_rounds;
  int64_t plan_id;
  char source[16];
  char controller[16];
  int64_t synced_at_ms;
  bool valid;
} PomodoroRemoteSession;

typedef void (*pomodoro_remote_handler_t)(const PomodoroRemoteSession *session);

typedef struct {
  int64_t plan_id;
  int work_sec;
  int short_break_sec;
  int long_break_sec;
  int rounds_before_long_break;
  int daily_goal_rounds;
  int daily_goal_minutes;
  bool valid;
} PomodoroPlanConfig;

typedef struct {
  int work_rounds;
  int work_minutes;
  int break_minutes;
  bool valid;
} PomodoroTodayStat;

esp_err_t pomodoro_api_fetch_today_stat(PomodoroTodayStat *out);

esp_err_t pomodoro_api_client_init();

/** GET /api/pomodoro/plans/default，获取专注/休息时长（分钟配置） */
esp_err_t pomodoro_api_fetch_default_plan(PomodoroPlanConfig *out);

/** GET /api/pomodoro/session，恢复重启前的计时状态 */
esp_err_t pomodoro_api_fetch_active_session(PomodoroRemoteSession *out);

void pomodoro_api_set_remote_handler(pomodoro_remote_handler_t handler);

/** 用户在本机点击开始/暂停/重置后调用，短暂忽略 ADMIN 远端覆盖 */
void pomodoro_api_mark_local_control(void);

/** 记录已应用的远端 syncedAtMs，避免轮询重复 apply */
void pomodoro_api_set_last_applied_synced_ms(int64_t synced_at_ms);

void pomodoro_api_sync_session(
    const char *phase,
    const char *run_state,
    int remaining_sec,
    int phase_total_sec,
    int session_work_rounds,
    int64_t plan_id,
    bool take_control,
    const char *pending_phase);

/** 阶段自然结束或跳过后写入完成记录（异步 POST） */
void pomodoro_api_create_record(
    const char *record_type,
    int duration_sec,
    int64_t plan_id,
    int round_index);
