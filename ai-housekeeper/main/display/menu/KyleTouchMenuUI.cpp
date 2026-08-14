//
// Created by kyle on 2026/4/13.
//

#include "KyleTouchMenuUI.h"
#include "lvgl.h"

#include "ImageLoader.h"
#include "application.h"
#include "assets/custom_font.h"

#define TAG "KyleTouchMenuUI"

lv_style_t KyleTouchMenuUI::menuUnselectedStyle;
lv_style_t KyleTouchMenuUI::menuSelectedStyle;

KyleTouchMenuUI::KyleTouchMenuUI(lv_obj_t* parent) {
    container_ = lv_obj_create(parent);
    lv_obj_set_size(container_, LV_PCT(100), LV_PCT(100));
    lv_obj_set_style_bg_color(container_, lv_color_white(), 0);
    lv_obj_set_style_border_width(container_, 0, 0);
    lv_obj_set_style_pad_all(container_, 0, 0);
    lv_obj_set_style_pad_top(container_, 7, 0);
    lv_obj_set_style_margin_all(container_, 0, 0);

    // 标题栏
    lv_obj_t* topContainer = lv_obj_create(container_);
    lv_obj_set_scrollbar_mode(topContainer, LV_SCROLLBAR_MODE_OFF);
    lv_obj_set_size(topContainer, LV_PCT(100), 40);
    lv_obj_set_style_bg_opa(topContainer, LV_OPA_TRANSP, 0);
    lv_obj_set_style_pad_all(topContainer, 0, 0);
    lv_obj_set_style_border_width(topContainer, 0, 0);
    lv_obj_set_layout(topContainer, LV_LAYOUT_FLEX);
    lv_obj_set_flex_flow(topContainer, LV_FLEX_FLOW_ROW);
    lv_obj_set_flex_align(topContainer, LV_FLEX_ALIGN_START, LV_FLEX_ALIGN_CENTER,
                          LV_FLEX_ALIGN_CENTER);
    auto backImg = ImageLoader::GetImage("back_black.png", 24, 24);
    if (backImg) {
        lv_obj_t* back = lv_image_create(topContainer);
        lv_obj_set_size(back, 32, 32);
        lv_image_set_src(back, backImg);
        lv_obj_set_style_margin_left(back, 5, 0);
        lv_obj_clear_state(back, LV_STATE_CHECKED);
        lv_obj_move_foreground(back);
        lv_obj_set_ext_click_area(back, 10);
        lv_obj_add_flag(back, LV_OBJ_FLAG_CLICKABLE);
        lv_obj_add_event_cb(
            back,
            [](lv_event_t* e) {
                ESP_LOGI(TAG, "back event");
                auto* _this = static_cast<KyleTouchMenuUI*>(lv_event_get_user_data(e));
                _this->back();
            },
            LV_EVENT_CLICKED, this);
    }
    topTitle_ = lv_label_create(topContainer);
    lv_obj_set_style_bg_color(topTitle_, lv_color_black(), 0);
    lv_obj_set_style_text_font(topTitle_, &lv_font_han_24, 0);
    lv_label_set_text(topTitle_, "目录");
    lv_obj_align(topTitle_, LV_ALIGN_LEFT_MID, 0, 0);
    lv_obj_set_height(topTitle_, LV_SIZE_CONTENT);
    lv_obj_clear_flag(topTitle_, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(topTitle_, LV_OBJ_FLAG_CLICKABLE);

    // 目录区
    // 必须使用 static，确保函数返回后 LVGL 依然能访问到这些数据
    static lv_coord_t col_dsc[] = {109, 109, LV_GRID_TEMPLATE_LAST};
    // 建议行数也设为 FR 或者预留足够的行
    static lv_coord_t row_dsc[] = {109, 109, 109, 109, 109, 109, LV_GRID_TEMPLATE_LAST};
    pageContainer_ = lv_obj_create(container_);
    lv_obj_set_pos(pageContainer_, 0, 40);
    lv_obj_set_style_bg_opa(pageContainer_, LV_OPA_TRANSP, 0);
    lv_obj_set_style_border_width(pageContainer_, 0, 0);
    lv_obj_set_size(pageContainer_, LV_PCT(100), 190);
    lv_obj_set_layout(pageContainer_, LV_LAYOUT_GRID);
    lv_obj_set_style_grid_column_dsc_array(pageContainer_, col_dsc, 0);
    lv_obj_set_style_grid_row_dsc_array(pageContainer_, row_dsc, 0);
    lv_obj_set_scroll_dir(pageContainer_, LV_DIR_VER);
    lv_obj_set_style_pad_all(pageContainer_, 0, 0);
    lv_obj_set_style_pad_left(pageContainer_, 7, 0);
    lv_obj_set_style_margin_all(pageContainer_, 0, 0);
    lv_obj_set_scrollbar_mode(pageContainer_, LV_SCROLLBAR_MODE_AUTO);
    lv_obj_set_scroll_dir(pageContainer_, LV_DIR_VER);
    lv_obj_set_style_anim_duration(pageContainer_, 200, 0);
    lv_obj_set_scroll_snap_y(pageContainer_, LV_SCROLL_SNAP_START);

    // 初始化目录的样式
    lv_style_init(&menuSelectedStyle);
    lv_style_set_opa(&menuSelectedStyle, LV_OPA_COVER);
    lv_style_set_text_font(&menuSelectedStyle, &lv_font_han_24);
    lv_style_init(&menuUnselectedStyle);
    lv_style_set_opa(&menuUnselectedStyle, LV_OPA_40);
    lv_style_set_text_font(&menuSelectedStyle, &lv_font_han_18);
    static lv_style_transition_dsc_t trans;
    static const lv_style_prop_t props[] = {LV_STYLE_OPA, LV_STYLE_PROP_INV};
    lv_style_transition_dsc_init(&trans, props, lv_anim_path_linear, 200, 0, nullptr);
    lv_style_set_transition(&menuSelectedStyle, &trans);

    lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
}

void KyleTouchMenuUI::setMenu(const std::vector<MenuItem>& menus) {
    menuStack_.clear();
    Menus m;
    m.hisIndex = 0;
    m.menus = menus;
    menuStack_.push_back(m);
    currentMenus_ = menus;
}
void KyleTouchMenuUI::show() {
    currentIndex_ = 0;
    if (container_) {
        lv_obj_move_foreground(container_);
        lv_obj_clear_flag(container_, LV_OBJ_FLAG_HIDDEN);
    }
    initMenu();
}
void KyleTouchMenuUI::hide() {
    menuStack_.clear();
    currentMenus_.clear();
    currentIndex_ = 0;
    lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
    isMenuOpened_ = false;
}

void KyleTouchMenuUI::select() {
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
            currentIndex_ = menu.onCurrentIndex();
            currentMenus_ = next_level.menus;
            initMenu();
        }
    } else {
        if (menu.onConfirm) {
            menu.onConfirm();
            updateFocus();
        }
    }
}
void KyleTouchMenuUI::back() {
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
bool KyleTouchMenuUI::isMenuOpened() { return isMenuOpened_; }

void KyleTouchMenuUI::prev() {
    if (currentIndex_ == 0) {
        currentIndex_ = currentMenus_.size() - 1;
    } else {
        currentIndex_--;
    }

    updateFocus();
}
void KyleTouchMenuUI::next() {
    if (currentIndex_ == currentMenus_.size() - 1) {
        currentIndex_ = 0;
    } else {
        currentIndex_++;
    }

    updateFocus();
}
void KyleTouchMenuUI::initMenu() {
    lv_obj_clean(pageContainer_);
    for (int i = 0; i < currentMenus_.size(); i++) {
        lv_obj_t* menu = lv_obj_create(pageContainer_);
        lv_obj_set_grid_cell(menu, LV_GRID_ALIGN_CENTER, i % 2, 1, LV_GRID_ALIGN_CENTER, i / 2, 1);
        lv_obj_set_size(menu, 113, 113);
        lv_obj_set_style_border_width(menu, 0, 0);
        lv_obj_set_scrollbar_mode(menu, LV_SCROLLBAR_MODE_OFF);
        lv_obj_set_style_pad_all(menu, 0, 0);
        MenuItem item = currentMenus_[i];
        lv_obj_add_flag(menu, LV_OBJ_FLAG_CLICKABLE);
        lv_obj_clear_flag(menu, LV_OBJ_FLAG_SCROLLABLE);
        lv_obj_add_flag(menu, LV_OBJ_FLAG_EVENT_BUBBLE);
        lv_obj_add_flag(menu, LV_OBJ_FLAG_SCROLL_CHAIN_VER);
        lv_obj_set_style_bg_opa(menu, LV_OPA_TRANSP, 0);
        lv_obj_add_flag(menu, LV_OBJ_FLAG_GESTURE_BUBBLE);
        lv_obj_add_style(menu, &menuUnselectedStyle, LV_STATE_DEFAULT);
        lv_obj_add_style(menu, &menuSelectedStyle, LV_STATE_CHECKED);
        lv_obj_clear_state(menu, LV_STATE_CHECKED);
        lv_obj_set_user_data(menu, reinterpret_cast<void*>(i));
        lv_obj_add_event_cb(
            menu,
            [](lv_event_t* e) {
                auto* _this = static_cast<KyleTouchMenuUI*>(lv_event_get_user_data(e));
                auto* target = static_cast<lv_obj_t*>(lv_event_get_target(e));
                int index = reinterpret_cast<intptr_t>(
                    lv_obj_get_user_data(static_cast<lv_obj_t*>(lv_event_get_target(e))));
                if (index < _this->currentMenus_.size()) {
                    auto& item = _this->currentMenus_[index];
                    _this->currentIndex_ = index;
                    lv_obj_scroll_to_view(target, LV_ANIM_ON);
                    _this->select();
                }
            },
            LV_EVENT_CLICKED, this);

        // 图标
        std::string iconTitle = item.iconTitle + ".png";
        if (auto menuIcon = ImageLoader::GetImage(iconTitle.c_str(), 64, 64)) {
            lv_obj_t* icon = nullptr;
            icon = lv_image_create(menu);
            lv_obj_set_size(icon, 64, 64);
            lv_obj_add_flag(icon, LV_OBJ_FLAG_EVENT_BUBBLE);
            lv_obj_align(icon, LV_ALIGN_TOP_MID, 0, 10);
            lv_image_set_src(icon, menuIcon);
        }

        // 名称
        lv_obj_t* label = lv_label_create(menu);
        lv_obj_add_flag(label, LV_OBJ_FLAG_EVENT_BUBBLE);
        if (!item.name.empty()) {
            lv_label_set_text(label, item.name.c_str());
        }
        lv_obj_align(label, LV_ALIGN_BOTTOM_MID, 0, 0);
    }
    updateFocus();
}
void KyleTouchMenuUI::updateFocus() {
    isMenuOpened_ = true;
    for (int i = 0; i < currentMenus_.size(); i++) {
        lv_obj_t* menu = lv_obj_get_child(pageContainer_, i);
        if (i == currentIndex_ && currentIndex_ != -1) {
            lv_obj_add_state(menu, LV_STATE_CHECKED);
            lv_obj_scroll_to_view(menu, LV_ANIM_ON);
        } else {
            lv_obj_clear_state(menu, LV_STATE_CHECKED);
        }
    }
}