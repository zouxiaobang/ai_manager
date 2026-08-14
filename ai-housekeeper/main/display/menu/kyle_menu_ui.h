#pragma once
#include <string>
#include <vector>
#include "lvgl.h"
#include "MenuUI.h"

class KyleMenuUI: public MenuUI {
public:
    KyleMenuUI(lv_obj_t* parent);

    void setMenu(const std::vector<MenuItem>& menus) override;
    void show() override;
    void hide() override;

    void next();
    void prev();
    void select() override;
    void back() override;
    bool isMenuOpened() override;

private:
    // 父容器 -- 由display提供
    lv_obj_t* parent_;
    // 菜单容器
    lv_obj_t* container_;
    // 横向滚动容器
    lv_obj_t* pageContainer_;

    void initMenu();
    void updateFocus();
};