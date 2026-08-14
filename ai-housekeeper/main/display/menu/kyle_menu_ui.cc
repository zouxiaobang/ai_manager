#include "kyle_menu_ui.h"
#include <esp_log.h>

#include "application.h"

#define TAG "MenuUI"
KyleMenuUI::KyleMenuUI(lv_obj_t* parent) : parent_(parent) {
    // 配置容器
    container_ = lv_obj_create(parent);
    lv_obj_set_size(container_, LV_PCT(100), LV_PCT(100));
    lv_obj_set_style_bg_color(container_, lv_color_black(), 0);
    lv_obj_set_style_border_width(container_, 0, 0);
    lv_obj_set_style_pad_all(container_, 0, 0);
    lv_obj_set_style_pad_top(container_, 7, 0);
    lv_obj_set_style_margin_all(container_, 0, 0);

    // 必须使用 static，确保函数返回后 LVGL 依然能访问到这些数据
    static lv_coord_t col_dsc[] = {70, 70, 70, LV_GRID_TEMPLATE_LAST};
    // 建议行数也设为 FR 或者预留足够的行
    static lv_coord_t row_dsc[] = {70, 70, 70, 70, 70, 70, LV_GRID_TEMPLATE_LAST};
    pageContainer_ = lv_obj_create(container_);
    lv_obj_set_style_bg_color(pageContainer_, lv_color_black(), 0);
    lv_obj_set_style_border_width(pageContainer_, 0, 0);
    lv_obj_set_size(pageContainer_, LV_PCT(100), LV_PCT(90));
    lv_obj_set_layout(pageContainer_, LV_LAYOUT_GRID);
    lv_obj_set_style_grid_column_dsc_array(pageContainer_, col_dsc, 0);
    lv_obj_set_style_grid_row_dsc_array(pageContainer_, row_dsc, 0);
    // lv_obj_set_flex_flow(page_container_, LV_FLEX_FLOW_ROW);
    lv_obj_set_scroll_dir(pageContainer_, LV_DIR_VER);
    lv_obj_set_scrollbar_mode(pageContainer_, LV_SCROLLBAR_MODE_OFF);
    lv_obj_set_style_pad_all(pageContainer_, 0, 0);
    lv_obj_set_style_pad_left(pageContainer_, 7, 0);
    lv_obj_set_style_margin_all(pageContainer_, 0, 0);

    lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
}

void KyleMenuUI::setMenu(const std::vector<MenuItem>& menus) {
    menuStack_.clear();
    Menus m;
    m.hisIndex = 0;
    m.menus = menus;
    menuStack_.push_back(m);
    currentMenus_ = menus;
}

void KyleMenuUI::show() {
    currentIndex_ = 0;
    ESP_LOGI(TAG, "准备绘制菜单");
    if (container_) {
        lv_obj_move_foreground(container_);
        lv_obj_clear_flag(container_, LV_OBJ_FLAG_HIDDEN);
    }
    initMenu();
}

bool KyleMenuUI::isMenuOpened() { return isMenuOpened_; }

void KyleMenuUI::initMenu() {
    lv_obj_clean(pageContainer_);
    for (int i = 0; i < currentMenus_.size(); i++) {
        lv_obj_t* menu = lv_obj_create(pageContainer_);
        lv_obj_set_grid_cell(menu, LV_GRID_ALIGN_CENTER, i % 3, 1, LV_GRID_ALIGN_CENTER, i / 3, 1);
        lv_obj_set_size(menu, 70, 70);
        lv_obj_set_scrollbar_mode(menu, LV_SCROLLBAR_MODE_OFF);
        lv_obj_set_style_pad_all(menu, 0, 0);
        MenuItem item = currentMenus_[i];
        lv_obj_add_flag(menu, LV_OBJ_FLAG_CLICKABLE); // 使其可点击
        lv_obj_clear_flag(menu, LV_OBJ_FLAG_SCROLL_CHAIN);
        lv_obj_add_event_cb(menu, [](lv_event_t* e) {
            MenuItem* item = (MenuItem*)lv_event_get_user_data(e);
            ESP_LOGI("EVENT", "menu: %s Clicked!", item->name.c_str());
            Application::GetInstance().sendGlobalEvent(GlobalEvent::MENU_BATTERY);
        }, LV_EVENT_CLICKED, &currentMenus_[i]);
        //
        // // 图标
        // lv_obj_t* icon = lv_img_create(menu);
        // lv_obj_set_size(icon, 32, 32);
        // lv_image_set_src(icon, item.icon);
        // lv_obj_set_style_bg_opa(icon, LV_OPA_0, 0);
        // lv_obj_set_style_border_width(icon, 0, 0);
        // lv_obj_set_style_shadow_width(icon, 0, 0);
        // lv_obj_align(icon, LV_ALIGN_TOP_MID, 0, 5);

        // 名称
        lv_obj_t* label = lv_label_create(menu);
        if (!item.name.empty()) {
            lv_label_set_text(label, item.name.c_str());
        }
        lv_obj_align(label, LV_ALIGN_BOTTOM_MID, 0, 0);

        // if (item.icon == nullptr) {
        //     lv_obj_add_flag(icon, LV_OBJ_FLAG_HIDDEN);
        //     lv_obj_align(label, LV_ALIGN_CENTER, 0, 0);
        // }
        // if (item.name.empty()) {
        //     /* code */
        //     lv_obj_add_flag(label, LV_OBJ_FLAG_HIDDEN);
        //     lv_obj_align(icon, LV_ALIGN_CENTER, 0, 0);
        // }
    }

    updateFocus();
}

void KyleMenuUI::updateFocus() {
    isMenuOpened_ = true;
    for (int i = 0; i < currentMenus_.size(); i++) {
        lv_obj_t* menu = lv_obj_get_child(pageContainer_, i);
        if (i == currentIndex_) {
            lv_obj_set_style_bg_color(menu, lv_color_hex(0x2E8B57), 0);
            lv_obj_set_style_text_color(menu, lv_color_white(), 0);
            lv_obj_set_style_border_width(menu, 0, 0);
        } else {
            lv_obj_set_style_bg_color(menu, lv_color_hex(0xC0C0C0), 0);
            lv_obj_set_style_border_width(menu, 4, 0);
            lv_obj_set_style_text_color(menu, lv_color_hex(0x808080), 0);
            lv_obj_set_style_border_color(menu, lv_color_black(), 0);
        }
    }
}

void KyleMenuUI::next() {
    if (currentIndex_ == currentMenus_.size() - 1) {
        currentIndex_ = 0;
    } else {
        currentIndex_++;
    }

    updateFocus();
}

void KyleMenuUI::prev() {
    if (currentIndex_ == 0) {
        currentIndex_ = currentMenus_.size() - 1;
    } else {
        currentIndex_--;
    }
    
    updateFocus();
}

void KyleMenuUI::select() {
    if (currentIndex_ >= currentMenus_.size() || currentIndex_ < 0) {
        return;
    }

    auto& menu = currentMenus_[currentIndex_];
    if (menu.type == MenuItemType::ACTION_CONFIRM) {
        /* code */
    } else if (menu.type == MenuItemType::PAGE) {
        /* code */
    } else if (menu.type == MenuItemType::SUBMENU) {
        if (!menu.children.empty()) {
            if (!menuStack_.empty()) {
                menuStack_.back().hisIndex = currentIndex_;
            }

            Menus next_level;
            next_level.hisIndex = 0;
            next_level.menus = menu.children;
            menuStack_.push_back(next_level);
            currentMenus_ = next_level.menus;
            currentIndex_ = 0;
            initMenu();
        }
    } else {
        if (menu.onConfirm) {
            menu.onConfirm();
        }
    }
}

void KyleMenuUI::hide() {
    menuStack_.clear();
    currentMenus_.clear();
    currentIndex_ = 0;
    lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
    isMenuOpened_ = false;
}

void KyleMenuUI::back() {
    // 如果栈中只有一层（当前层），说明退回后就没菜单了
    if (menuStack_.size() <= 1) {
        hide();
        return;
    }

    // 1. 弹出当前层
    menuStack_.pop_back();

    // 2. 获取上一层的数据
    Menus& parent_level = menuStack_.back();
    currentMenus_ = parent_level.menus;
    currentIndex_ = parent_level.hisIndex;

    initMenu();
}