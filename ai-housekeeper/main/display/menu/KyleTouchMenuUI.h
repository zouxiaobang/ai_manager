//
// Created by kyle on 2026/4/13.
//

#ifndef AI_HOUSEKEEPER_KYLETOUCHMENUUI_H
#define AI_HOUSEKEEPER_KYLETOUCHMENUUI_H
#include "MenuUI.h"
#include "misc/lv_style.h"
#include "misc/lv_types.h"

class KyleTouchMenuUI: public MenuUI{
public:
    KyleTouchMenuUI(lv_obj_t* parent);

    void setMenu(const std::vector<MenuItem>& menus) override;
    void show() override;
    void hide() override;

    void next();
    void prev();
    void select() override;
    void back() override;
    bool isMenuOpened() override;
private:
    // 页面
    lv_obj_t* container_;
    // 目录容器
    lv_obj_t* pageContainer_ = nullptr;
    lv_obj_t* topTitle_ = nullptr;
    static lv_style_t menuSelectedStyle;
    static lv_style_t menuUnselectedStyle;

    void initMenu();
    void updateFocus();
};



#endif //AI_HOUSEKEEPER_KYLETOUCHMENUUI_H
