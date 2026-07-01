#include "pomodoro_bar.h"

#include "lvgl.h"

namespace {
constexpr uint32_t kBaseColor = 0x2D325A;
constexpr uint32_t kFocusFill = 0xE86A5A;
constexpr uint32_t kShortFill = 0x8FBC7A;
constexpr uint32_t kLongFill = 0xA88BC4;

uint32_t fill_color_for_phase(pomo_bar_phase_t phase) {
  switch (phase) {
    case POMO_BAR_PHASE_FOCUS:
      return kFocusFill;
    case POMO_BAR_PHASE_SHORT:
      return kShortFill;
    case POMO_BAR_PHASE_LONG:
      return kLongFill;
    default:
      return kFocusFill;
  }
}

void apply_bar_style(lv_obj_t *bar, lv_coord_t w, lv_coord_t h, bool horizontal) {
  if (bar == nullptr) {
    return;
  }
  lv_obj_set_size(bar, w, h);
  lv_bar_set_range(bar, 0, 100);
  lv_bar_set_orientation(bar, horizontal ? LV_BAR_ORIENTATION_HORIZONTAL : LV_BAR_ORIENTATION_VERTICAL);
  lv_obj_set_style_bg_color(bar, lv_color_hex(kBaseColor), LV_PART_MAIN);
  lv_obj_set_style_bg_opa(bar, LV_OPA_COVER, LV_PART_MAIN);
  lv_obj_set_style_border_width(bar, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar, 0, LV_PART_MAIN);
  lv_obj_set_style_radius(bar, 0, LV_PART_INDICATOR);
  lv_obj_set_style_pad_all(bar, 0, LV_PART_MAIN);
  lv_obj_set_style_bg_color(bar, lv_color_hex(kFocusFill), LV_PART_INDICATOR);
  lv_obj_set_style_bg_opa(bar, LV_OPA_COVER, LV_PART_INDICATOR);
}
}  // namespace

extern "C" void pomodoro_bar_init(lv_obj_t *bar, lv_coord_t w, lv_coord_t h) {
  apply_bar_style(bar, w, h, false);
}

extern "C" void pomodoro_bar_init_horizontal(lv_obj_t *bar, lv_coord_t w, lv_coord_t h) {
  apply_bar_style(bar, w, h, true);
}

extern "C" void pomodoro_bar_set_fill_phase(lv_obj_t *bar, pomo_bar_phase_t phase) {
  if (bar == nullptr) {
    return;
  }
  lv_obj_set_style_bg_color(bar, lv_color_hex(fill_color_for_phase(phase)), LV_PART_INDICATOR);
}
