cmake_minimum_required(VERSION 3.16)
project(kyle_host_tests C CXX)

# host 单元测试：把 main/core + boards 纯逻辑直接编进可执行文件，不经过 ESP-IDF。
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CORE_DIR  ${CMAKE_CURRENT_SOURCE_DIR}/../main/core)
set(BOARD_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../main/boards)

add_executable(unit_tests
    unity.c
    core/wire_format_test.cpp
    core/chat_session_test.cpp
    core/ota_version_test.cpp
    core/net_config_test.cpp
    core/wifi_config_test.cpp
    core/wifi_state_test.cpp
    core/ota_check_test.cpp
    core/ota_report_test.cpp
    core/ws_protocol_test.cpp
    core/ota_plan_test.cpp
    core/provisioning_test.cpp
    board/pin_conflict_test.cpp
    drivers/lcd_pattern_test.cpp
    drivers/backlight_test.cpp
    drivers/cst816s_parse_test.cpp
    drivers/i2s_audio_math_test.cpp
    drivers/button_debounce_test.cpp
    drivers/button_gesture_test.cpp
    drivers/led_pattern_test.cpp
    drivers/composite_input_test.cpp
    app/application_test.cpp
    ${CORE_DIR}/json_mini.cc
    ${CORE_DIR}/wire_format.cc
    ${CORE_DIR}/chat_session.cc
    ${CORE_DIR}/ota_version.cc
    ${CORE_DIR}/net_config.cc
    ${CORE_DIR}/wifi_config.cc
    ${CORE_DIR}/wifi_state.cc
    ${CORE_DIR}/ota_check.cc
    ${CORE_DIR}/ota_report.cc
    ${CORE_DIR}/ws_protocol.cc
    ${CORE_DIR}/ota_plan.cc
    ${CORE_DIR}/provisioning.cc
    ${BOARD_DIR}/pin_conflict.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/app/application.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/drivers/lcd_pattern.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/drivers/led_pattern.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/drivers/cst816s_parse.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/drivers/i2s_audio_math.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/../main/drivers/composite_input.cc
)

target_include_directories(unit_tests PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../main
    ${CORE_DIR}
)

# 与 IDF Kconfig 同源的默认 OTA URL / WiFi 兜底 / 语言 / 固件版本，覆盖验证 CONFIG_* 宏路径
target_compile_definitions(unit_tests PRIVATE
    "CONFIG_OTA_URL=\"https://api.tenclass.net/kyle/ota/\""
    "CONFIG_WIFI_SSID=\"kyle-fallback-ssid\""
    "CONFIG_WIFI_PASSWORD=\"\""
    "CONFIG_LANGUAGE=\"zh-CN\""
    "CONFIG_FIRMWARE_VERSION=\"2.2.2\""
)
