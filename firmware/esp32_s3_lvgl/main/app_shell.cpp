#include "app_shell.h"

#include "display.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "modules/music_module.h"
#include "modules/pomodoro_module.h"
#include "panel_config.h"
#include "ui_common.h"
#include <lvgl.h>

extern "C" {
LV_FONT_DECLARE(font_chinese_20);
LV_FONT_DECLARE(lv_font_montserrat_20);
}

namespace app_shell {
namespace {

constexpr char TAG[] = "app_shell";

struct ModuleSlot {
  esp_err_t (*create)(lv_obj_t *tile);
  void (*on_show)();
  void (*on_hide)();
};

constexpr ModuleSlot kModules[kModuleCount] = {
    {pomodoro_module::create, pomodoro_module::on_show, pomodoro_module::on_hide},
    {music_module::create, music_module::on_show, music_module::on_hide},
};

lv_obj_t *page_viewport = nullptr;
lv_obj_t *module_tiles[kModuleCount] = {nullptr};
int active_module_index = kModulePomodoro;
int pending_module_index = -1;
lv_point_t swipe_press_point = {};
bool gesture_suppress_active = false;
int64_t last_page_switch_us = 0;

constexpr int kPageSwipeThreshold = 70;
constexpr int kControlSwipeCancelThreshold = 36;
constexpr int64_t kPageSwitchCooldownUs = 400000;

#if TOUCH_TEST_ENABLED
lv_obj_t *touch_label = nullptr;
lv_obj_t *touch_dot = nullptr;
#endif

void update_visibility_locked() {
  for (int i = 0; i < kModuleCount; ++i) {
    if (module_tiles[i] == nullptr) {
      continue;
    }
    if (i == active_module_index) {
      lv_obj_clear_flag(module_tiles[i], LV_OBJ_FLAG_HIDDEN);
    } else {
      lv_obj_add_flag(module_tiles[i], LV_OBJ_FLAG_HIDDEN);
    }
  }
}

void set_active_module_locked(int module_index) {
  if (module_index < 0 || module_index >= kModuleCount || module_index == active_module_index) {
    return;
  }

  kModules[active_module_index].on_hide();
  active_module_index = module_index;
  update_visibility_locked();
  kModules[active_module_index].on_show();
}

void set_active_module(int module_index) {
  if (module_index < 0 || module_index >= kModuleCount || module_index == active_module_index) {
    return;
  }

  const int64_t now = esp_timer_get_time();
  if (now - last_page_switch_us < kPageSwitchCooldownUs) {
    return;
  }
  last_page_switch_us = now;

  display_lock();
  set_active_module_locked(module_index);
  display_unlock();
  ESP_LOGI(TAG, "active_module=%d", active_module_index);
}

void apply_pending_module_async_cb(void *user_data) {
  (void)user_data;
  if (pending_module_index < 0) {
    return;
  }
  const int module_index = pending_module_index;
  pending_module_index = -1;
  set_active_module(module_index);
}

void request_active_module(int module_index) {
  if (module_index < 0 || module_index >= kModuleCount || module_index == active_module_index) {
    return;
  }
  pending_module_index = module_index;
  lv_async_call(apply_pending_module_async_cb, nullptr);
}

bool try_handle_page_swipe(int dx, int dy, int start_x) {
  if (LV_ABS(dx) < LV_ABS(dy)) {
    return false;
  }
  if (LV_ABS(dx) >= kGestureCancelThresholdPx) {
    gesture_suppress_active = true;
  }
  if (LV_ABS(dx) < kPageSwipeThreshold) {
    return false;
  }

  if (dx < -kPageSwipeThreshold && active_module_index == kModulePomodoro) {
    request_active_module(kModuleMusic);
    return true;
  }

  (void)start_x;
  return false;
}

#if PANEL_CALIBRATION_ENABLED
lv_obj_t *create_outline_shape(lv_obj_t *parent, lv_coord_t width, lv_coord_t height, uint32_t border_color, bool circle) {
  lv_obj_t *shape = lv_obj_create(parent);
  lv_obj_set_size(shape, width, height);
  lv_obj_set_style_bg_opa(shape, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(shape, 3, 0);
  lv_obj_set_style_border_color(shape, lv_color_hex(border_color), 0);
  lv_obj_set_style_pad_all(shape, 0, 0);
  lv_obj_set_style_radius(shape, circle ? LV_RADIUS_CIRCLE : 0, 0);
  lv_obj_clear_flag(shape, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_clear_flag(shape, LV_OBJ_FLAG_CLICKABLE);
  return shape;
}

lv_obj_t *create_filled_bar(lv_obj_t *parent, lv_coord_t width, lv_coord_t height, uint32_t color) {
  lv_obj_t *bar = lv_obj_create(parent);
  lv_obj_set_size(bar, width, height);
  lv_obj_set_style_bg_color(bar, lv_color_hex(color), 0);
  lv_obj_set_style_bg_opa(bar, LV_OPA_COVER, 0);
  lv_obj_set_style_border_width(bar, 0, 0);
  lv_obj_set_style_radius(bar, 0, 0);
  lv_obj_clear_flag(bar, LV_OBJ_FLAG_SCROLLABLE);
  lv_obj_clear_flag(bar, LV_OBJ_FLAG_CLICKABLE);
  return bar;
}

void add_px_label(lv_obj_t *parent, lv_obj_t *anchor, const char *text, lv_align_t align) {
  lv_obj_t *label = lv_label_create(parent);
  lv_label_set_text(label, text);
  lv_obj_set_style_text_color(label, lv_color_hex(0xE0E0E0), 0);
  lv_obj_set_style_text_font(label, &lv_font_montserrat_20, 0);
  lv_obj_align_to(label, anchor, align, 0, -6);
}

void create_calibration_page(lv_obj_t *parent) {
  lv_obj_t *title = lv_label_create(parent);
  lv_label_set_text(title, "比例校准：用尺子量各图形实际宽高 (mm)");
  lv_obj_set_style_text_font(title, &font_chinese_20, 0);
  lv_obj_set_style_text_color(title, lv_color_hex(0xFFFFFF), 0);
  lv_obj_align(title, LV_ALIGN_TOP_MID, 0, 6);

  lv_obj_t *screen_frame = create_outline_shape(parent, PANEL_WIDTH, PANEL_HEIGHT, 0x888888, false);
  lv_obj_set_pos(screen_frame, 0, 0);
  lv_obj_set_style_border_width(screen_frame, 2, 0);

  lv_obj_t *h_ruler = create_filled_bar(parent, PANEL_WIDTH, 4, 0xFFD54F);
  lv_obj_align(h_ruler, LV_ALIGN_TOP_MID, 0, 36);
  add_px_label(parent, h_ruler, "800 px 横线", LV_ALIGN_OUT_BOTTOM_MID);

  lv_obj_t *v_ruler = create_filled_bar(parent, 4, PANEL_HEIGHT - 80, 0xFFD54F);
  lv_obj_align(v_ruler, LV_ALIGN_LEFT_MID, 8, 20);
  lv_obj_t *v_label = lv_label_create(parent);
  lv_label_set_text(v_label, "480px");
  lv_obj_set_style_text_font(v_label, &lv_font_montserrat_20, 0);
  lv_obj_set_style_text_color(v_label, lv_color_hex(0xFFD54F), 0);
  lv_obj_align_to(v_label, v_ruler, LV_ALIGN_OUT_RIGHT_MID, 8, 0);

  lv_obj_t *sq400 = create_outline_shape(parent, 400, 400, 0xFF5252, false);
  lv_obj_align(sq400, LV_ALIGN_CENTER, -110, 10);
  add_px_label(parent, sq400, "400 x 400 px 方框", LV_ALIGN_OUT_TOP_MID);

  lv_obj_t *circ400 = create_outline_shape(parent, 400, 400, 0x40C4FF, true);
  lv_obj_align(circ400, LV_ALIGN_CENTER, 110, 10);
  add_px_label(parent, circ400, "400 x 400 px 圆", LV_ALIGN_OUT_TOP_MID);

  lv_obj_t *sq280 = create_outline_shape(parent, 280, 280, 0x69F0AE, false);
  lv_obj_align(sq280, LV_ALIGN_BOTTOM_LEFT, 24, -24);
  add_px_label(parent, sq280, "280方框(番茄环)", LV_ALIGN_OUT_TOP_MID);

  lv_obj_t *circ280 = create_outline_shape(parent, 280, 280, 0xB388FF, true);
  lv_obj_align(circ280, LV_ALIGN_BOTTOM_RIGHT, -24, -24);
  add_px_label(parent, circ280, "280圆(番茄环)", LV_ALIGN_OUT_TOP_MID);

  lv_obj_t *hint = lv_label_create(parent);
  lv_label_set_text(hint,
                    "量: 红方框/蓝圆 的 宽mm 与 高mm\n"
                    "量完发我或记入 panel_config.h\n"
                    "完成后 PANEL_CALIBRATION_ENABLED 改 0");
  lv_obj_set_style_text_font(hint, &font_chinese_20, 0);
  lv_obj_set_style_text_color(hint, lv_color_hex(0x9E9E9E), 0);
  lv_obj_set_style_text_align(hint, LV_TEXT_ALIGN_CENTER, 0);
  lv_obj_align(hint, LV_ALIGN_BOTTOM_MID, 0, -4);

  ESP_LOGI(TAG, "Calibration: measure 400x400 square and circle (width mm, height mm)");
}
#endif

}  // namespace

esp_err_t init(lv_obj_t *screen) {
  if (screen == nullptr) {
    return ESP_ERR_INVALID_ARG;
  }

  lv_obj_set_style_bg_color(screen, lv_color_hex(ui_common::kBgColor), 0);
  lv_obj_set_style_bg_grad_dir(screen, LV_GRAD_DIR_NONE, 0);
  lv_obj_set_style_text_color(screen, lv_color_hex(0xFFFFFF), 0);
  lv_obj_clear_flag(screen, LV_OBJ_FLAG_SCROLLABLE);

#if PANEL_CALIBRATION_ENABLED
  lv_obj_t *cal_page = lv_obj_create(screen);
  ui_common::style_page(cal_page);
  lv_obj_set_size(cal_page, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(cal_page, 0, 0);
  create_calibration_page(cal_page);
  return ESP_OK;
#endif

  page_viewport = lv_obj_create(screen);
  lv_obj_set_size(page_viewport, PANEL_WIDTH, PANEL_HEIGHT);
  lv_obj_set_pos(page_viewport, 0, 0);
  lv_obj_set_style_bg_opa(page_viewport, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(page_viewport, 0, 0);
  lv_obj_set_style_pad_all(page_viewport, 0, 0);
  lv_obj_clear_flag(page_viewport, LV_OBJ_FLAG_OVERFLOW_VISIBLE);
  lv_obj_clear_flag(page_viewport, LV_OBJ_FLAG_SCROLLABLE);

  for (int i = 0; i < kModuleCount; ++i) {
    module_tiles[i] = lv_obj_create(page_viewport);
    ui_common::style_page(module_tiles[i]);
    lv_obj_set_size(module_tiles[i], PANEL_WIDTH, PANEL_HEIGHT);
    lv_obj_set_pos(module_tiles[i], 0, 0);
    lv_obj_clear_flag(module_tiles[i], LV_OBJ_FLAG_SCROLLABLE);

    const esp_err_t err = kModules[i].create(module_tiles[i]);
    if (err != ESP_OK) {
      ESP_LOGE(TAG, "module %d create failed: %s", i, esp_err_to_name(err));
      return err;
    }
  }

  active_module_index = kModulePomodoro;
  display_lock();
  update_visibility_locked();
  kModules[active_module_index].on_show();
  display_unlock();

#if TOUCH_TEST_ENABLED
  touch_label = lv_label_create(screen);
  lv_label_set_text(touch_label, "Touch: waiting");
  lv_obj_set_style_text_color(touch_label, lv_color_hex(0xFFCC00), 0);
  lv_obj_align(touch_label, LV_ALIGN_TOP_LEFT, 12, 12);

  touch_dot = lv_obj_create(screen);
  lv_obj_set_size(touch_dot, 24, 24);
  lv_obj_set_style_radius(touch_dot, LV_RADIUS_CIRCLE, 0);
  lv_obj_set_style_bg_color(touch_dot, lv_color_hex(0xFF3030), 0);
  lv_obj_set_style_border_width(touch_dot, 0, 0);
  lv_obj_clear_flag(touch_dot, LV_OBJ_FLAG_CLICKABLE);
  lv_obj_add_flag(touch_dot, LV_OBJ_FLAG_HIDDEN);
#endif

  return ESP_OK;
}

void handle_swipe_release(int dx, int dy) {
  (void)try_handle_page_swipe(dx, dy, swipe_press_point.x);
}

void note_touch_press(int x, int y) {
  swipe_press_point.x = static_cast<lv_coord_t>(x);
  swipe_press_point.y = static_cast<lv_coord_t>(y);
  gesture_suppress_active = false;
}

bool gesture_suppress_control_click() {
  return gesture_suppress_active;
}

void set_gesture_suppress_control_click(bool suppress) {
  gesture_suppress_active = suppress;
}

int active_module() {
  return active_module_index;
}

bool is_module_active(int module_index) {
  return module_index == active_module_index;
}

void switch_to_module(int module_index) {
  request_active_module(module_index);
}

void set_status(const char *text) {
  (void)text;
}

void set_touch_debug(const char *text) {
#if TOUCH_TEST_ENABLED
  display_lock();
  if (touch_label != nullptr) {
    lv_label_set_text(touch_label, text != nullptr ? text : "Touch: empty");
  }
  display_unlock();
#else
  (void)text;
#endif
}

void show_touch_point(int x, int y, bool pressed) {
#if TOUCH_TEST_ENABLED
  display_lock();

  if (touch_label != nullptr) {
    if (pressed) {
      lv_label_set_text_fmt(touch_label, "Touch: x=%d y=%d", x, y);
    } else {
      lv_label_set_text(touch_label, "Touch: released");
    }
  }

  if (touch_dot != nullptr) {
    if (pressed) {
      int dot_x = x - 12;
      int dot_y = y - 12;
      if (dot_x < 0) {
        dot_x = 0;
      }
      if (dot_y < 0) {
        dot_y = 0;
      }
      if (dot_x > PANEL_WIDTH - 24) {
        dot_x = PANEL_WIDTH - 24;
      }
      if (dot_y > PANEL_HEIGHT - 24) {
        dot_y = PANEL_HEIGHT - 24;
      }
      lv_obj_clear_flag(touch_dot, LV_OBJ_FLAG_HIDDEN);
      lv_obj_set_pos(touch_dot, dot_x, dot_y);
    } else {
      lv_obj_add_flag(touch_dot, LV_OBJ_FLAG_HIDDEN);
    }
  }

  display_unlock();
#else
  (void)x;
  (void)y;
  (void)pressed;
#endif
}

}  // namespace app_shell
