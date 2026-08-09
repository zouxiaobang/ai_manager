# main 组件：只保留两块板的组装 + 公共 hal/core/drivers
# 板型由 Kconfig choice 二选一，这里按 CONFIG_BOARD_* 只编译对应一块板。

set(BOARD_SRC "boards/supermini-c3/supermini_c3_board.cc")
if(CONFIG_BOARD_KYLE_S3_LCD)
    set(BOARD_SRC "boards/kyle-s3-lcd/kyle_s3_lcd_board.cc")
endif()

idf_component_register(
    SRCS
        "app/app_main.cc"
        "app/application.cc"
        "app/nvs_storage.cc"
        "core/json_mini.cc"
        "core/wire_format.cc"
        "core/chat_session.cc"
        "core/ota_version.cc"
        "core/net_config.cc"
        "boards/board_factory.cc"
        "boards/pin_conflict.cc"
        "${BOARD_SRC}"
        "drivers/no_codec_i2s.cc"
        "drivers/ssd1306_oled.cc"
        "drivers/st7789_lcd.cc"
        "drivers/lcd_pattern.cc"
        "drivers/cst816s_parse.cc"
        "drivers/gpio_backlight.cc"
        "drivers/gpio_button.cc"
        "drivers/cst816s_touch.cc"
        "drivers/gpio_led.cc"
        "drivers/no_display.cc"
        "drivers/no_power.cc"
        "drivers/no_network.cc"
    INCLUDE_DIRS
        "."
    REQUIRES
        nvs_flash
    PRIV_REQUIRES
        # F1：固件私有依赖——驱动(传感器/外设)、LCD、网络(WiFi/Netif/WebSocket/HTTP)
        driver
        esp_lcd
        esp_timer
        esp_wifi
        esp_netif
        esp_websocket_client
        esp_http_client
        # F4 启用唤醒词/UI 后按需补充：espressif/esp-sr、espressif/esp_lvgl_port（经 idf_component.yml 引入）
)

# host 单测独立构建，见 test/CMakeLists.txt（不经过本文件）
