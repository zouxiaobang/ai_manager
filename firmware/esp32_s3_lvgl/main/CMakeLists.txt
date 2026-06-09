idf_component_register(
    SRCS
        "main.cpp"
        "display.cpp"
        "gt911_touch.cpp"
        "fonts/font_chinese_20.c"
        "fonts/font_chinese_28.c"
        "ui.cpp"
        "ui_common.cpp"
        "app_shell.cpp"
        "modules/pomodoro_module.cpp"
        "modules/music_module.cpp"
        "pomodoro_api_client.cpp"
        "websocket_client.cpp"
        "wifi_manager.cpp"
    INCLUDE_DIRS
        "include"
    REQUIRES
        driver
        esp_event
        esp_lcd
        esp_netif
        esp_timer
        esp_wifi
        esp_http_client
        json
        lvgl
        mbedtls
        nvs_flash
        esp_websocket_client
        esp_lcd_touch_gt911
)

target_compile_features(${COMPONENT_LIB} PRIVATE cxx_std_17)
target_compile_definitions(${COMPONENT_LIB} PUBLIC LV_LVGL_H_INCLUDE_SIMPLE=1)
