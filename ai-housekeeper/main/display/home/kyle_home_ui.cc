#include "kyle_home_ui.h"
#include <ctime>
#include <iomanip>
#include <ios>

#include "assets.h"
#include "src/misc/lv_timer.h"

#include "cJSON.h"
#include "esp_err.h"
#include "esp_http_client.h"
#include "esp_log.h"
#include "esp_lvgl_port.h"
#include "esp_netif_types.h"
#include "lvgl_theme.h"
#include "settings.h"

#define TAG "TwtHomeUI"

HomeUI::HomeUI() {}
HomeUI::~HomeUI() {}

void HomeUI::Show() {
    if (container_) {
        lv_obj_clear_flag(container_, LV_OBJ_FLAG_HIDDEN);
        lv_timer_resume(timer_);
    }
}

void HomeUI::Hide() {
    if (container_) {
        lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
        lv_timer_pause(timer_);
    }
}
std::string url_encode(const std::string& value) {
    std::ostringstream escaped;
    escaped.fill('0');
    escaped << std::hex;

    for (std::string::value_type c : value) {
        // 保留字母、数字以及一些特殊符号
        if (isalnum((unsigned char)c) || c == '-' || c == '_' || c == '.' || c == '~') {
            escaped << c;
            continue;
        }
        // 其他字符（包括中文）全部转义
        escaped << '%' << std::setw(2) << int((unsigned char)c);
    }

    return escaped.str();
}
std::string get_city_from_amap() {
    std::string result = "shantou";  // 默认值

    esp_http_client_config_t config = {
        .url = "http://restapi.amap.com/v3/ip?key=463f6275019e0d7f422c6c7036f04c2f",
        .method = HTTP_METHOD_GET,
        .timeout_ms = 5000,
        // 关键：不使用 event_handler，直接同步读
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);
    if (client == NULL)
        return result;

    esp_err_t err = esp_http_client_open(client, 0);
    if (err == ESP_OK) {
        esp_http_client_fetch_headers(client);
        int64_t content_length = esp_http_client_get_content_length(client);

        // 如果 content_length <= 0 (比如 chunked 传输)，我们给一个预设缓冲区
        int buffer_size = (content_length <= 0) ? 1024 : content_length + 1;
        char* buffer = (char*)malloc(buffer_size);

        if (buffer) {
            int read_len = esp_http_client_read_response(client, buffer, buffer_size);
            if (read_len > 0) {
                buffer[read_len] = '\0';
                ESP_LOGI("HTTP", "高德原始返回: %s", buffer);

                cJSON* root = cJSON_Parse(buffer);
                if (root) {
                    cJSON* city = cJSON_GetObjectItem(root, "city");
                    if (cJSON_IsString(city) && city->valuestring) {
                        result = city->valuestring;
                    }
                    cJSON_Delete(root);
                }
            }
            free(buffer);
        }
    } else {
        ESP_LOGE("HTTP", "连接高德失败: %s", esp_err_to_name(err));
    }
    esp_http_client_cleanup(client);
    return result;
}
std::string get_weather(const std::string& city) {
    // 强制使用自动定位，避开中文编码导致的 invalid host
    std::string encoded_city = url_encode(city);
    std::string url = "http://wttr.in/" + encoded_city + "?format=%C|%t|%w&lang=zh";

    esp_http_client_config_t config = {
        .url = url.c_str(),
        .method = HTTP_METHOD_GET,
        .timeout_ms = 5000,
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);
    if (!client)
        return "N/A";

    std::string result = "获取失败";
    if (esp_http_client_open(client, 0) == ESP_OK) {
        esp_http_client_fetch_headers(client);

        char buffer[256];  // 天气字符串通常很短，256字节足够了
        int read_len = esp_http_client_read_response(client, buffer, sizeof(buffer) - 1);
        if (read_len > 0) {
            buffer[read_len] = '\0';
            result = buffer;
            // 过滤掉可能导致乱码的换行符
            result.erase(result.find_last_not_of("\n\r") + 1);
        }
    }
    esp_http_client_cleanup(client);
    return result;
}
void parse_weather_string(const std::string& raw, std::string& weather, std::string& temp,
                          std::string& wink) {
    std::string response = raw;
    size_t p1 = response.find('|');
    size_t p2 = response.find('|', p1 + 1);

    if (p1 != std::string::npos && p2 != std::string::npos) {
        weather = response.substr(0, p1);             // "多云"
        temp = response.substr(p1 + 1, p2 - p1 - 1);  // "+20°C"
        wink = response.substr(p2 + 1);               // "14"
    }
}
std::string code2Icon(std::string raw_weather) {
    if (raw_weather.find("晴") != std::string::npos) {
        return "weather_sunny";
    } else if (raw_weather.find("云") != std::string::npos ||
               raw_weather.find("阴") != std::string::npos) {
        return "weather_cloudy";
    } else if (raw_weather.find("雾") != std::string::npos ||
               raw_weather.find("霾") != std::string::npos) {
        return "weather_fog";
    } else if (raw_weather.find("雨") != std::string::npos) {
        return "weather_moderate_rain";
    } else if (raw_weather.find("雪") != std::string::npos) {
        return "weather_light_snow";
    } else if (raw_weather.find("雷") != std::string::npos) {
        return "weather_thundery";
    }

    return "unknown";
}
void code2Wink(std::string wink_str, std::string& speed, std::string& level) {
    size_t first_digit = wink_str.find_first_of("0123456789");
    if (first_digit == std::string::npos) {
        speed = "无";
        level = "无风";
        return;
    }
    int kmh = atoi(wink_str.substr(first_digit).c_str());
    if (kmh < 1) {
        speed = "无";
        level = "无风";
    } else if (kmh <= 5) {
        speed = "一级";
        level = "软风";
    } else if (kmh <= 11) {
        speed = "二级";
        level = "轻风";
    } else if (kmh <= 19) {
        speed = "三级";
        level = "微风";
    } else if (kmh <= 28) {
        speed = "四级";
        level = "和风";
    } else if (kmh <= 38) {
        speed = "五级";
        level = "清风";
    } else if (kmh <= 49) {
        speed = "六级";
        level = "强风";
    } else if (kmh <= 61) {
        speed = "七级";
        level = "疾风";
    } else {
        speed = "八级以上";
        level = "狂风";
    }
}
void fetch_weather_task(void* param) {
    TwtHomeUI* ui = static_cast<TwtHomeUI*>(param);

    // --- 哨兵：等待网络就绪 ---
    ESP_LOGI("MAIN", "天气任务已创建，等待 10 秒让网络稳定...");
    vTaskDelay(pdMS_TO_TICKS(10000));

    {
        std::string city = get_city_from_amap();
        if (city.empty()) {
            city = "shantou";  // fallback
        }
        if (city.find("市") != std::string::npos) {
            city = city.substr(0, city.size() - 3);
        }
        ESP_LOGI("MAIN", "City: %s", city.c_str());

        std::string weather = get_weather(city);
        ESP_LOGI("MAIN", "Weather: %s", weather.c_str());
        std::string weather_str, temp_str, wink_str;
        parse_weather_string(weather, weather_str, temp_str, wink_str);

        ESP_LOGI("MAIN", "weather_str: %s, temp_str: %s, wink_str: %s", weather_str.c_str(),
                 temp_str.c_str(), wink_str.c_str());
        // 显示到 LVGL
        if (lvgl_port_lock(0)) {
            std::string speed, level;
            code2Wink(wink_str, speed, level);
            ui->UpdateWeather(code2Icon(weather_str), weather_str, speed, level);
            Settings settings("display", true);
            settings.SetString("weather_icon", code2Icon(weather_str));
            settings.SetString("weather", weather_str);
            std::string temp = temp_str;
            if (!temp.empty() && temp[0] == '+') {
                temp.erase(0, 1);
            }
            ui->UpdateTemperature(temp);
            lvgl_port_unlock();
        }
    }
    ESP_LOGI("TASK", "任务即将安全退出");
    vTaskDelete(NULL);
}
void weather_timer_cb(lv_timer_t* timer) {
    auto* ui = static_cast<TwtHomeUI*>(lv_timer_get_user_data(timer));
    xTaskCreate(fetch_weather_task, "weather", 16384, ui, 5, NULL);
}

TwtHomeUI::TwtHomeUI(lv_obj_t* parent) {
    // 主题读取 -- 从Settings中读取
    Settings settings("display", false);
    // 没有设置时默认Light主题
    theme_name_ = settings.GetString("theme", "light");
    auto& theme_manager = LvglThemeManager::GetInstance();
    Theme* current_theme = theme_manager.GetTheme(theme_name_);
    LvglTheme* lvgl_theme = static_cast<LvglTheme*>(current_theme);

    container_ = lv_obj_create(parent);
    lv_obj_set_size(container_, LV_PCT(100), LV_PCT(100));
    // lv_obj_set_style_bg_color(container_, lv_color_black(), 0);
    lv_obj_set_style_bg_color(container_, lvgl_theme->background_color(), 0);
    lv_obj_set_style_border_width(container_, 0, 0);
    lv_obj_set_style_pad_all(container_, 0, 0);
    lv_obj_set_style_margin_all(container_, 0, 0);
    lv_obj_set_style_radius(container_, 0, 0);

    top_bar_ = lv_obj_create(container_);
    lv_obj_set_size(top_bar_, LV_HOR_RES, 80);
    lv_obj_set_style_pad_left(top_bar_, 4, 0);
    lv_obj_set_style_pad_right(top_bar_, 12, 0);
    lv_obj_set_style_border_width(top_bar_, 0, 0);
    // lv_obj_set_style_bg_color(top_bar_, lv_color_black(), 0);
    lv_obj_set_style_bg_color(top_bar_, lvgl_theme->background_color(), 0);

    label_city_ = lv_label_create(top_bar_);
    // lv_obj_set_style_text_font(label_city_, &lv_font_montserrat_14, 0);
    lv_obj_set_style_text_color(label_city_, lv_color_hex(0xFFD700), 0);
    lv_obj_align(label_city_, LV_ALIGN_TOP_LEFT, 10, 4);
    const std::string city = settings.GetString("city", "澄海");
    if (label_city_) {
        lv_label_set_text(label_city_, city.c_str());
    }

    label_weather_ = lv_label_create(top_bar_);
    lv_obj_align(label_weather_, LV_ALIGN_TOP_LEFT, 10, 28);
    // lv_obj_set_style_text_color(label_weather_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_weather_, lvgl_theme->text_color(), 0);

    label_wind_ = lv_label_create(top_bar_);
    lv_obj_align(label_wind_, LV_ALIGN_TOP_LEFT, 88, 28);
    // lv_obj_set_style_text_color(label_wind_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_wind_, lvgl_theme->text_color(), 0);
    if (label_wind_) {
        lv_label_set_text(label_wind_, "");
    }

    icon_weather_ = lv_img_create(top_bar_);
    lv_obj_set_style_pad_all(icon_weather_, 4, 0);
    lv_obj_align(icon_weather_, LV_ALIGN_RIGHT_MID, -12, 0);

    lv_obj_t* line = lv_line_create(top_bar_);
    static lv_point_precise_t line_points[] = {{10, 0}, {LV_HOR_RES / 2, 0}};
    lv_line_set_points(line, line_points, 2);
    lv_obj_align(line, LV_ALIGN_BOTTOM_LEFT, 0, 0);
    // lv_obj_set_style_line_color(line, lv_color_white(), 0);
    lv_obj_set_style_line_color(line, lvgl_theme->text_color(), 0);

    std::string weather_icon = settings.GetString("weather_icon", "weather_sunny");
    std::string weather = settings.GetString("weather", "晴天");
    UpdateWeather(weather_icon, weather, "", "");

    mid_bar_ = lv_obj_create(container_);
    lv_obj_set_size(mid_bar_, LV_HOR_RES, LV_SIZE_CONTENT);
    lv_obj_align(mid_bar_, LV_ALIGN_CENTER, 0, 0);
    lv_obj_set_style_pad_left(mid_bar_, 6, 0);
    lv_obj_set_style_pad_right(mid_bar_, 6, 0);
    lv_obj_set_style_border_width(mid_bar_, 0, 0);
    // lv_obj_set_style_bg_color(mid_bar_, lv_color_black(), 0);
    lv_obj_set_style_bg_color(mid_bar_, lvgl_theme->background_color(), 0);

    label_time_hour_ = lv_label_create(mid_bar_);
    lv_obj_set_style_text_color(label_time_hour_, lv_color_hex(0xFFD700), 0);
    lv_obj_set_style_text_font(label_time_hour_, &lv_font_montserrat_48, 0);
    lv_obj_align(label_time_hour_, LV_ALIGN_LEFT_MID, 10, 0);
    label_time_min_ = lv_label_create(mid_bar_);
    lv_obj_set_style_text_color(label_time_min_, lv_color_hex(0x00CED1), 0);
    lv_obj_set_style_text_font(label_time_min_, &lv_font_montserrat_44, 0);
    lv_obj_align(label_time_min_, LV_ALIGN_LEFT_MID, 70, 0);
    label_time_sec_ = lv_label_create(mid_bar_);
    // lv_obj_set_style_text_color(label_time_sec_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_time_sec_, lvgl_theme->text_color(), 0);
    lv_obj_set_style_text_font(label_time_sec_, &lv_font_montserrat_28, 0);
    lv_obj_align(label_time_sec_, LV_ALIGN_BOTTOM_LEFT, 130, 0);

    lv_obj_t* line_2 = lv_line_create(mid_bar_);
    static lv_point_precise_t line_points_2[] = {{10, 0}, {LV_HOR_RES - 60, 0}};
    lv_line_set_points(line_2, line_points_2, 2);
    lv_obj_align(line_2, LV_ALIGN_BOTTOM_LEFT, 0, 0);
    lv_obj_set_style_line_width(line_2, 2, 0);
    lv_obj_set_style_line_color(line_2, lvgl_theme->text_color(), 0);
    // lv_obj_set_style_line_color(line_2, lv_color_white(), 0);
    lv_obj_set_style_line_rounded(line_2, true, 0);

    bottom_bar_ = lv_obj_create(container_);
    lv_obj_set_size(bottom_bar_, LV_HOR_RES, 70);
    lv_obj_set_style_pad_all(bottom_bar_, 0, 0);
    lv_obj_set_style_pad_top(bottom_bar_, 4, 0);
    lv_obj_set_style_pad_left(bottom_bar_, 4, 0);
    lv_obj_align(bottom_bar_, LV_ALIGN_BOTTOM_LEFT, 0, 0);
    lv_obj_set_style_border_opa(bottom_bar_, LV_OPA_50, 0);
    lv_obj_set_style_bg_color(bottom_bar_, lvgl_theme->background_color(), 0);
    // lv_obj_set_style_bg_color(bottom_bar_, lv_color_black(), 0);
    lv_obj_set_style_border_color(bottom_bar_, lv_color_hex(0xD2B48C), 0);
    if (theme_name_ == "light") {
        lv_obj_set_style_border_width(bottom_bar_, 0, 0);
    }

    // 周几
    label_weekday_ = lv_label_create(bottom_bar_);
    lv_obj_align(label_weekday_, LV_ALIGN_LEFT_MID, 20, 12);
    lv_obj_set_size(label_weekday_, 60, 30);
    lv_obj_set_style_pad_top(label_weekday_, 4, 0);
    lv_obj_set_style_bg_color(label_weekday_, lv_color_hex(0xD2691E), 0);
    lv_obj_set_style_bg_opa(label_weekday_, LV_OPA_COVER, 0);
    lv_obj_set_style_radius(label_weekday_, 20, 0);
    lv_obj_set_style_text_align(label_weekday_, LV_TEXT_ALIGN_CENTER, 0);
    // lv_obj_set_style_text_color(label_weekday_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_weekday_, lvgl_theme->text_color(), 0);

    // 日期
    label_date_ = lv_label_create(bottom_bar_);
    // lv_obj_set_style_text_color(label_date_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_date_, lvgl_theme->text_color(), 0);
    lv_obj_align(label_date_, LV_ALIGN_TOP_LEFT, 10, 0);
    // lv_obj_set_style_text_color(label_date_, lv_color_white(), 0);
    lv_obj_set_style_text_color(label_date_, lvgl_theme->text_color(), 0);

    // 温度
    label_temperature_ = lv_label_create(bottom_bar_);
    lv_obj_set_style_text_color(label_temperature_, lvgl_theme->text_color(), 0);
    // lv_obj_set_style_text_color(label_temperature_, lv_color_white(), 0);
    lv_obj_set_style_text_font(label_temperature_, &lv_font_montserrat_24, 0);
    lv_obj_align(label_temperature_, LV_ALIGN_TOP_RIGHT, -10, 0);
    if (label_temperature_) {
        lv_label_set_text(label_temperature_, "");
    }

    label_wind_speed_ = lv_label_create(bottom_bar_);
    lv_obj_set_style_text_color(label_wind_speed_, lvgl_theme->text_color(), 0);
    // lv_obj_set_style_text_color(label_wind_speed_, lv_color_white(), 0);
    lv_obj_align(label_wind_speed_, LV_ALIGN_TOP_RIGHT, -10, 30);
    if (label_wind_speed_) {
        lv_label_set_text(label_wind_speed_, "");
    }

    lv_obj_add_flag(container_, LV_OBJ_FLAG_HIDDEN);
    timer_ = lv_timer_create(
        [](lv_timer_t* t) {
            TwtHomeUI* ui = static_cast<TwtHomeUI*>(lv_timer_get_user_data(t));
            time_t now;
            time(&now);
            struct tm* tm_info = localtime(&now);
            char time_hour[8];
            char time_min[8];
            char time_sec[8];
            strftime(time_hour, sizeof(time_hour), "%H", tm_info);
            strftime(time_min, sizeof(time_min), "%M", tm_info);
            strftime(time_sec, sizeof(time_sec), "%S", tm_info);
            ui->UpdateTime(time_hour, time_min, time_sec);
            char date_buf[32];
            strftime(date_buf, sizeof(date_buf), "%m月%d日", tm_info);
            ui->UpdateDate(date_buf);
            int day = tm_info->tm_wday;
            const char* weekday_arr[] = {
                "周日", "周一", "周二", "周三", "周四", "周五", "周六",
            };
            ui->UpdateWeekday(weekday_arr[day]);
        },
        1000, this);

    weather_timer_ = lv_timer_create(weather_timer_cb, 1800000, this);
    if (weather_timer_) {
        lv_timer_ready(weather_timer_);
    }
}

TwtHomeUI::~TwtHomeUI() {
    if (container_) {
        lv_obj_del(container_);
    }
    if (timer_) {
        lv_timer_del(timer_);
    }
    if (weather_timer_) {
        lv_timer_del(weather_timer_);
    }
}

void TwtHomeUI::UpdateTime(const std::string& time_hour, const std::string& time_min,
                           const std::string& time_sec) {
    if (label_time_hour_ && label_time_min_ && label_time_sec_) {
        lv_label_set_text(label_time_hour_, time_hour.c_str());
        lv_label_set_text(label_time_min_, time_min.c_str());
        lv_label_set_text(label_time_sec_, time_sec.c_str());
    }
}

void TwtHomeUI::UpdateDate(const std::string& date_str) {
    if (label_date_)
        lv_label_set_text(label_date_, date_str.c_str());
}
void TwtHomeUI::UpdateWeekday(const std::string& weekday_str) {
    if (label_weekday_)
        lv_label_set_text(label_weekday_, weekday_str.c_str());
}

void TwtHomeUI::UpdateWeather(const std::string& weather_icon, const std::string& weather_str,
                              const std::string& speed, const std::string& level) {
    size_t size = 0;
    void* ptr = nullptr;
    std::string picName = theme_name_ == "light" ? weather_icon + ".png" : weather_icon + "_dark.png";
    if (Assets::GetInstance().GetAssetData(weather_icon + ".png", ptr, size) && icon_weather_) {
        static lv_image_dsc_t png_dsc;
        memset(&png_dsc, 0, sizeof(lv_image_dsc_t));
        png_dsc.header.cf = LV_COLOR_FORMAT_RAW;
        png_dsc.header.w = 32;
        png_dsc.header.h = 32;
        png_dsc.header.magic = LV_IMAGE_HEADER_MAGIC;
        png_dsc.data_size = size;
        png_dsc.data = (const uint8_t*)ptr;
        lv_image_set_src(icon_weather_, &png_dsc);
    }

    if (label_weather_)
        lv_label_set_text(label_weather_, weather_str.c_str());
    if (label_wind_)
        lv_label_set_text(label_wind_, level.c_str());
    if (label_wind_speed_) {
        lv_label_set_text(label_wind_speed_, ("风量：" + speed).c_str());
    }
}

void TwtHomeUI::UpdateTemperature(const std::string& temp_str) {
    if (label_temperature_)
        lv_label_set_text(label_temperature_, temp_str.c_str());
}
