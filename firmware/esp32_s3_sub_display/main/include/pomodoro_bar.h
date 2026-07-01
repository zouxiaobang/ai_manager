#pragma once

#include "lvgl.h"

#ifdef __cplusplus
extern "C" {
#endif

#define POMO_HOME_BAR_W           200
#define POMO_HOME_BAR_H           14
#define POMO_HOME_BAR_BORDER_P    2
#define POMO_HOME_BAR_CORNER_INSET 6
#define POMO_FULL_TIME_SCALE      5
#define POMO_FULL_TIME_GAP        12

#define POMO_BAR_W  14
#define POMO_BAR_H  168

typedef enum {
  POMO_BAR_PHASE_IDLE = 0,
  POMO_BAR_PHASE_FOCUS,
  POMO_BAR_PHASE_SHORT,
  POMO_BAR_PHASE_LONG,
} pomo_bar_phase_t;

void pomodoro_bar_init(lv_obj_t *bar, lv_coord_t w, lv_coord_t h);
void pomodoro_bar_init_horizontal(lv_obj_t *bar, lv_coord_t w, lv_coord_t h);
void pomodoro_bar_set_fill_phase(lv_obj_t *bar, pomo_bar_phase_t phase);

#ifdef __cplusplus
}
#endif
