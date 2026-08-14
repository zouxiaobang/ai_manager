#pragma once

#include <lvgl.h>
#include <string>

class HomeUI {
protected:
    lv_obj_t* container_;
    lv_timer_t* timer_;
    lv_timer_t* weather_timer_;

public:
    HomeUI();
    virtual ~HomeUI();

    virtual void Show();
    virtual void Hide();
};

class TwtHomeUI : public HomeUI {
private:
    /* data */
    lv_obj_t* top_bar_;
    lv_obj_t* mid_bar_;
    lv_obj_t* bottom_bar_;

    lv_obj_t* label_time_hour_ = nullptr;
    lv_obj_t* label_time_min_ = nullptr;
    lv_obj_t* label_time_sec_ = nullptr;
    lv_obj_t* label_date_ = nullptr;
    lv_obj_t* label_weekday_ = nullptr;
    lv_obj_t* label_city_ = nullptr;
    lv_obj_t* weather_bar_ = nullptr;
    lv_obj_t* icon_weather_ = nullptr;
    lv_obj_t* label_weather_ = nullptr;
    lv_obj_t* label_wind_speed_ = nullptr;
    lv_obj_t* label_wind_ = nullptr;
    lv_obj_t* label_temperature_ = nullptr;

    std::string theme_name_;

public:
    TwtHomeUI(lv_obj_t* parent);
    ~TwtHomeUI();

    void UpdateTime(const std::string& time_hour, const std::string& time_min, const std::string& time_sec);
    void UpdateDate(const std::string& date_str);
    void UpdateWeekday(const std::string& weekday_str);
    void UpdateWeather(const std::string& weather_icon, const std::string& weather_str, const std::string& speed, const std::string& level);
    void UpdateTemperature(const std::string& temperature_str);
};
