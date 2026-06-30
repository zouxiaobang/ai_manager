#pragma once

#include <cstdint>

enum class PomodoroPhase { Idle, Focus, ShortBreak, LongBreak };

struct PomodoroSnapshot {
  PomodoroPhase phase = PomodoroPhase::Idle;
  bool running = false;
  int remaining_sec = 0;
  int total_sec = 25 * 60;
};

void pomodoro_init();
void pomodoro_tick();
PomodoroSnapshot pomodoro_get();
void pomodoro_toggle_start();
void pomodoro_reset();
bool pomodoro_is_active_on_lock();
