#include "ui_common.h"

namespace ui_common {

void make_plain(lv_obj_t *obj) {
  lv_obj_set_style_bg_opa(obj, LV_OPA_TRANSP, 0);
  lv_obj_set_style_border_width(obj, 0, 0);
  lv_obj_set_style_pad_all(obj, 0, 0);
  lv_obj_clear_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
}

void style_page(lv_obj_t *page) {
  lv_obj_set_style_bg_color(page, lv_color_hex(kBgColor), 0);
  lv_obj_set_style_bg_opa(page, LV_OPA_COVER, 0);
  lv_obj_set_style_bg_grad_dir(page, LV_GRAD_DIR_NONE, 0);
  lv_obj_set_style_border_width(page, 0, 0);
  lv_obj_set_style_pad_all(page, 0, 0);
  lv_obj_clear_flag(page, LV_OBJ_FLAG_SCROLLABLE);
}

void disable_click(lv_obj_t *obj) {
  if (obj == nullptr) {
    return;
  }
  lv_obj_clear_flag(obj, LV_OBJ_FLAG_CLICKABLE);
}

}  // namespace ui_common
