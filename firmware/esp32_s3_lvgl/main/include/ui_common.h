#pragma once

#include "lvgl.h"

namespace ui_common {

constexpr uint32_t kBgColor = 0x1A1A1A;

void make_plain(lv_obj_t *obj);
void style_page(lv_obj_t *page);
void disable_click(lv_obj_t *obj);

}  // namespace ui_common
