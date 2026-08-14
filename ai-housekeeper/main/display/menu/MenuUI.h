//
// Created by kyle on 2026/4/13.
//

#ifndef AI_HOUSEKEEPER_MENUUI_H
#define AI_HOUSEKEEPER_MENUUI_H
#include <functional>
#include <string>
#include <vector>

enum class MenuItemType { SUBMENU, PAGE, ACTION_CONFIRM, TOGGLE };

struct MenuItem {
    std::string name = "";
    std::string iconTitle = "";
    MenuItemType type = MenuItemType::TOGGLE;
    std::function<int()> onCurrentIndex = []() -> int { return -1; };
    std::vector<MenuItem> children = {};
    std::function<void()> onEnterPage = []() {};
    std::function<void()> onConfirm = []() {};
    std::string description = "";
};
struct Menus {
    int hisIndex;
    std::vector<MenuItem> menus;
};

class MenuUI {
public:
    virtual ~MenuUI() = default;
    virtual void setMenu(const std::vector<MenuItem>& menus) = 0;
    virtual void show() = 0;
    virtual void hide() = 0;
    virtual void select() = 0;
    virtual void back() = 0;
    virtual bool isMenuOpened() = 0;

protected:
    // 当前分级菜单
    std::vector<MenuItem> currentMenus_;
    // 菜单栈
    std::vector<Menus> menuStack_;
    // 当前选中菜单
    int currentIndex_ = 0;
    bool isMenuOpened_ = false;
};

#endif  // AI_HOUSEKEEPER_MENUUI_H
