cmake_minimum_required(VERSION 3.16)

set(LV_CONF_PATH "${CMAKE_CURRENT_SOURCE_DIR}/main/include/lv_conf.h" CACHE STRING "LVGL config path" FORCE)
add_compile_definitions(LV_CONF_INCLUDE_SIMPLE=1)
set(PROJECT_VER "0.1.0")

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(esp32_s3_sub_display)
