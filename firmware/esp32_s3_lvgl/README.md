# ESP-IDF 固件

该目录是 ESP32-S3 智能副屏的 ESP-IDF C++ 工程，不再使用 Arduino IDE、Arduino 框架或 PlatformIO。

## 依赖

- ESP-IDF，目标芯片 `esp32s3`
- ESP-IDF Component Manager
- LVGL 8.x，由 `main/idf_component.yml` 管理
- `esp_websocket_client`，由 `main/idf_component.yml` 通过 Component Manager 管理
- ESP-IDF 内置组件：`esp_lcd`、`esp_wifi`、`driver`、`json`

## 文件说明

- `CMakeLists.txt`: ESP-IDF 工程入口。
- `sdkconfig.defaults`: 默认启用 ESP32-S3、PSRAM、较大 APP 分区等配置。
- `main/CMakeLists.txt`: 固件组件源码和依赖声明。
- `main/idf_component.yml`: 托管组件依赖。
- `main/main.cpp`: `app_main()` 入口。
- `main/display.cpp`: RGB LCD 和 LVGL 显示刷新。
- `main/gt911_touch.cpp`: GT911 I2C 触摸驱动。
- `main/ui.cpp`: LVGL UI 和触摸按钮。
- `main/wifi_manager.cpp`: Wi-Fi STA 连接。
- `main/websocket_client.cpp`: WebSocket 客户端（网易云控制/歌词，连 `pc_daemon`）。
- `main/pomodoro_api_client.cpp`: 番茄钟 HTTP 直连管理后台 `PUT /api/pomodoro/session`。
- `main/include/panel_config.h`: 屏幕、触摸、Wi-Fi、`pc_daemon` 与 `ADMIN_API_*` 配置。
- `main/include/lv_conf.h`: LVGL 编译配置。
- `main/fonts/`: 界面用中文点阵字库（由脚本从 TTF 生成）。
- `main/fonts/source/`: 放置 **阿里妈妈刀隶体** TTF 源文件，见该目录 `README.md`。

### 中文字体（阿里妈妈刀隶体）

ESP32 不能直接使用系统字体，需将 TTF 转为 LVGL 的 `font_chinese_20.c` / `font_chinese_28.c`。

1. 从 [阿里妈妈字体](https://www.alibabafonts.com) 下载 **刀隶体**，放到 `main/fonts/source/AlimamaDaoLiTi.ttf`
2. 安装 Node.js 后执行：`python scripts/generate_all_fonts.py`
3. 重新编译烧录

歌词、番茄钟状态等中文界面均使用上述字库；数字时间仍用 Montserrat。

## 1. 进入 ESP-IDF 环境

Windows 推荐打开 `ESP-IDF PowerShell`。如果使用普通 PowerShell，需要先执行 ESP-IDF 的环境脚本，例如：

```powershell
& "C:\Espressif\frameworks\esp-idf-v5.x\export.ps1"
```

路径按你的实际 ESP-IDF 安装位置修改。

确认 `idf.py` 可用：

```powershell
idf.py --version
```

## 2. 配置工程

进入固件目录：

```powershell
cd firmware\esp32_s3_lvgl
idf.py set-target esp32s3
```

首次构建时，ESP-IDF 会根据 `main/idf_component.yml` 自动下载 LVGL、`esp_websocket_client` 和 `esp_lcd_touch_gt911` 组件。

如果之前已经配置失败过，建议重新配置一次：

```powershell
idf.py fullclean
idf.py reconfigure
```

## 3. 修改设备参数

编辑：

```text
main/include/panel_config.h
```

需要重点确认：

- `WIFI_SSID`
- `WIFI_PASSWORD`
- `PC_DAEMON_HOST` / `PC_DAEMON_PORT`（音乐 WebSocket，通常为 PC 局域网 IP）
- `ADMIN_API_HOST` / `ADMIN_API_PORT`（番茄钟 HTTP，与跑 Spring Boot 的机器 IP 一致，默认 `8080`）
- `GT911_I2C_ADDRESS`

LCD 和触摸引脚已经按你的屏幕说明书配置：

- `LCD_DE=GPIO5`
- `LCD_VSYNC=GPIO3`
- `LCD_HSYNC=GPIO46`
- `LCD_PCLK=GPIO7`
- `TP_IRQ=GPIO4`
- `TP_SDA=GPIO8`
- `TP_SCL=GPIO9`

## 4. 编译

```powershell
idf.py build
```

如果提示 PSRAM、Flash Size 或分区相关错误，可以执行：

```powershell
idf.py menuconfig
```

重点检查：

- `Serial flasher config -> Flash size`
- `Component config -> ESP PSRAM`
- `Partition Table`

## 5. 烧录

先查看设备管理器中的串口号，然后执行：

```powershell
idf.py -p COMx flash
```

例如：

```powershell
idf.py -p COM5 flash
```

如果无法自动进入下载模式，按住开发板 `BOOT` 键后重新执行烧录，开始写入后松开。

## 6. 启动和串口监视

烧录后直接监视：

```powershell
idf.py -p COMx monitor
```

也可以烧录和监视一步完成：

```powershell
idf.py -p COMx flash monitor
```

退出 monitor：

```text
Ctrl+]
```

正常启动时应看到类似日志：

```text
I display: Initialize RGB panel
I display: LCD DISP is controlled by CH422G EXIO2
I gt911: GT911 found at 0x5D
I wifi: Got IP: 192.168.x.x
I websocket: ...
```

## 7. 运行流程

固件启动后会依次执行：

1. 初始化 NVS。
2. 初始化 RGB LCD 和 LVGL。
3. 创建歌词 UI 和底部控制按钮。
4. 初始化 GT911 触摸，并注册为 LVGL pointer 输入设备。
5. 连接 Wi-Fi。
6. 自动打开 WebSocket 播放会话。
7. 通过触摸按钮发送 `previous`、`toggle`、`next` 控制命令。

## 注意事项

- `TP_RST` 位于 CH422G `EXIO1`，当前已按厂家 demo 在触摸初始化时通过 CH422G 拉低/拉高复位 GT911。
- `LCD DISP` 位于 CH422G `EXIO2`，当前还没有实现 CH422G 背光使能控制。
- 如果串口提示 `Create GT911 touch failed`，优先看 I2C 扫描结果里是否有 `0x24`、`0x38`、`0x5D` 或 `0x14`。
- 触摸初始化参考厂家 demo：先通过 CH422G 复位 GT911，再使用 `esp_lcd_touch_gt911` 官方组件读取触摸。
- 当前默认关闭 `TOUCH_TEST_ENABLED`，开启 `TOUCH_POLL_TASK_ENABLED`。GT911 由独立任务读取硬件坐标并缓存，LVGL 的 input device 从缓存读取坐标并触发按钮事件；不会再用手写坐标区域判断按钮。
- 如果触摸坐标方向不对，可以在 `panel_config.h` 中调整 `TOUCH_SWAP_XY`、`TOUCH_MIRROR_X`、`TOUCH_MIRROR_Y`。
- 如果屏幕内容持续向左或向右漂移，优先确认 `RGB_BOUNCE_BUFFER_SIZE_PX` 已启用。当前值按官方 demo 设置为 `PANEL_WIDTH * 10`，用于避免 ESP32-S3 RGB 屏 screen drift。
- 如果屏幕无显示，优先检查 CH422G 背光使能、RGB 时序、PCLK 极性和 PSRAM 是否启用。
- 当前使用 `main/fonts/font_chinese_20.c` 项目专用简体中文字体，基于 Windows `simhei.ttf` 生成，字符集为 GB2312（约 6763 个常用简体字）+ ASCII/中文标点/全角符号。重新生成字体：`python scripts/generate_font_chinese_20.py`
