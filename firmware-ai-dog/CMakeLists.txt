cmake_minimum_required(VERSION 3.16)

# kyle ESP32 全新架构固件（从零重构，只保留 supermini-c3 / kyle-s3-lcd 两块板）
set(PROJECT_VER "2.2.2")
set(PROJECT_NAME "kyle")

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(kyle)

# 说明：
#  - 构建命令：cd firmware-ai-dog && idf.py set-target esp32c3|esp32s3 && idf.py build
#  - host 单元测试独立构建（不依赖 IDF）：cmake -S test -B build && cmake --build build && ./build/unit_tests
