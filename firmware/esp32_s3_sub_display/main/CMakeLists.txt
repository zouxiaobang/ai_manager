idf_component_register(
    SRCS
        "main.cpp"
        "board_io.cpp"
        "display.cpp"
        "gt911_touch.cpp"
        "app_ui.cpp"
        "pixel_ui.cpp"
        "app_settings.cpp"
        "app_power.cpp"
        "app_clock.cpp"
        "pomodoro_model.cpp"
        "pomodoro_plan_cache.cpp"
        "pomodoro_sync.cpp"
        "pomodoro_api_config.cpp"
        "wifi_sta.cpp"
        "sd_storage.cpp"
        "assets_loader.cpp"
        "pomodoro_bar.cpp"
        "assets_embed/assets_seed.cpp"
        "assets_embed/ui_embed_images.cpp"
        "ui_home_static_layout.c"
    INCLUDE_DIRS
        "include"
        "assets_embed"
    REQUIRES
        driver
        esp_lcd
        esp_timer
        lvgl
        nvs_flash
        esp_lcd_touch_gt911
        fatfs
        sdmmc
        esp_wifi
        esp_netif
        esp_http_client
        json
        mdns
)

include("${CMAKE_CURRENT_SOURCE_DIR}/fonts/gb2312_font_parts.cmake")
if(NOT GB2312_FONT_SRCS)
    message(FATAL_ERROR "GB2312 font missing. Run: py fonts/generate_gb2312_font.py")
endif()
target_sources(${COMPONENT_LIB} PRIVATE ${GB2312_FONT_SRCS})

file(GLOB EMBED_IMG_SRCS CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/assets_embed/ui_img_*.c")
file(GLOB SEED_IMG_SRCS CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/assets_embed/k_seed_*.c")
target_sources(${COMPONENT_LIB} PRIVATE ${EMBED_IMG_SRCS} ${SEED_IMG_SRCS})

target_compile_features(${COMPONENT_LIB} PRIVATE cxx_std_17)
target_compile_definitions(${COMPONENT_LIB} PUBLIC LV_LVGL_H_INCLUDE_SIMPLE=1)
