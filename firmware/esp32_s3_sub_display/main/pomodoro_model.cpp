#include "pomodoro_model.h"

PomodoroSnapshot g_state = {};

void pomodoro_init() {
  g_state.phase = PomodoroPhase::Idle;
  g_state.running = false;
  g_state.remaining_sec = 25 * 60;
  g_state.total_sec = 25 * 60;
}

void pomodoro_tick() {
  if (!g_state.running || g_state.phase == PomodoroPhase::Idle) {
    return;
  }
  if (g_state.remaining_sec > 0) {
    g_state.remaining_sec--;
  }
  if (g_state.remaining_sec <= 0) {
    g_state.running = false;
    g_state.phase = PomodoroPhase::Idle;
    g_state.remaining_sec = g_state.total_sec;
  }
}

PomodoroSnapshot pomodoro_get() {
  return g_state;
}

void pomodoro_toggle_start() {
  if (g_state.phase == PomodoroPhase::Idle) {
    g_state.phase = PomodoroPhase::Focus;
    g_state.total_sec = 25 * 60;
    g_state.remaining_sec = g_state.total_sec;
  }
  g_state.running = !g_state.running;
}

void pomodoro_reset() {
  g_state.running = false;
  g_state.phase = PomodoroPhase::Idle;
  g_state.total_sec = 25 * 60;
  g_state.remaining_sec = g_state.total_sec;
}

bool pomodoro_is_active_on_lock() {
  return g_state.phase != PomodoroPhase::Idle && (g_state.running || g_state.remaining_sec < g_state.total_sec);
}
