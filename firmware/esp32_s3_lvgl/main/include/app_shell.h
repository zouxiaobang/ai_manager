#pragma once

#include "esp_err.h"
#include <lvgl.h>

namespace app_shell {

constexpr int kModulePomodoro = 0;
constexpr int kModuleMusic = 1;
constexpr int kModuleCount = 2;
/** 按下后移动超过该像素视为滑动，取消点击/长按 */
constexpr int kGestureCancelThresholdPx = 36;

esp_err_t init(lv_obj_t *screen);

void handle_swipe_release(int dx, int dy);
void note_touch_press(int x, int y);

bool gesture_suppress_control_click();
void set_gesture_suppress_control_click(bool suppress);

int active_module();
bool is_module_active(int module_index);
void switch_to_module(int module_index);

void set_status(const char *text);
void set_touch_debug(const char *text);
void show_touch_point(int x, int y, bool pressed);

}  // namespace app_shell
