# 固件架构总览

## 项目简介

ESP32-S3 副屏固件是运行在微雪 7 寸触摸屏上的嵌入式应用，基于 ESP-IDF + LVGL 构建，提供番茄钟显示、歌词展示、像素狗宠物等功能，与管理后台通过 WiFi HTTP 通信实现数据同步。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 主控芯片 | ESP32-S3 | 32位双核处理器 |
| 开发框架 | ESP-IDF v5.x | 官方开发框架 |
| 图形库 | LVGL v9.x | 开源嵌入式GUI |
| 显示屏 | 7寸 IPS LCD | 800×480 分辨率 |
| 触摸屏 | GT911 | 电容触摸 |
| 存储 | SD卡 + NVS Flash | 资源文件 + 配置存储 |
| 网络 | WiFi STA | 局域网HTTP通信 |
| 编程语言 | C/C++ | C++17 |

## 硬件配置

| 硬件 | 规格 |
|------|------|
| 主控 | ESP32-S3-WROOM-1 |
| 屏幕 | 7寸 IPS TFT，800×480 |
| 触摸 | GT911 电容触摸 |
| 存储 | Micro SD卡（SPI） |
| 连接 | WiFi 2.4G |

## 功能模块

```
ESP32-S3 副屏固件
├── 系统层
│   ├── 显示驱动 (display.cpp)
│   ├── 触摸驱动 (gt911_touch.cpp)
│   ├── WiFi连接 (wifi_sta.cpp)
│   ├── SD卡存储 (sd_storage.cpp)
│   └── 时钟管理 (app_clock.cpp)
│
├── UI层
│   ├── UI管理 (app_ui.cpp)
│   ├── 首页布局 (ui_home_static_layout.c)
│   └── 像素UI (pixel_ui.cpp)
│
├── 业务模块
│   ├── 番茄钟
│   │   ├── 模型 (pomodoro_model.cpp)
│   │   ├── 同步 (pomodoro_sync.cpp)
│   │   ├── 进度条 (pomodoro_bar.cpp)
│   │   ├── 计划缓存 (pomodoro_plan_cache.cpp)
│   │   └── API配置 (pomodoro_api_config.cpp)
│   │
│   ├── 像素狗
│   │   ├── 模型 (pixel_dog_model.cpp)
│   │   ├── 精灵动画 (pixel_dog_sprite.cpp)
│   │   ├── 同步 (pixel_dog_sync.cpp)
│   │   └── API配置 (pixel_dog_api_config.cpp)
│   │
│   └── 媒体/歌词
│       ├── 状态 (media_state.h)
│       ├── 控制 (media_control.cpp)
│       ├── 同步 (media_sync.cpp)
│       └── API配置 (media_api_config.cpp)
│
├── 资源管理
│   ├── 嵌入资源 (assets_embed/)
│   ├── SD卡资源 (sd_assets.h)
│   └── 资源加载 (assets_loader.cpp)
│
└── 系统设置
    ├── 设置管理 (app_settings.cpp)
    ├── 电源管理 (app_power.cpp)
    └── 板级IO (board_io.cpp)
```

## 目录结构

```
esp32_s3_sub_display/
├── main/                          # 主程序源码
│   ├── include/                   # 头文件
│   │   ├── app_ui.h
│   │   ├── app_clock.h
│   │   ├── app_power.h
│   │   ├── app_settings.h
│   │   ├── display.h
│   │   ├── gt911_touch.h
│   │   ├── panel_config.h        # 屏幕配置
│   │   ├── lv_conf.h             # LVGL配置
│   │   ├── pomodoro_model.h
│   │   ├── pomodoro_sync.h
│   │   ├── pomodoro_bar.h
│   │   ├── pixel_dog_model.h
│   │   ├── pixel_dog_sprite.h
│   │   ├── pixel_dog_sync.h
│   │   ├── media_state.h
│   │   ├── media_control.h
│   │   ├── media_sync.h
│   │   ├── sd_assets.h
│   │   ├── sd_storage.h
│   │   ├── wifi_sta.h
│   │   └── ...
│   ├── assets_embed/              # 嵌入资源（编译进Flash）
│   │   ├── ui_embed_images.cpp
│   │   ├── assets_seed.cpp
│   │   └── ... (各种PNG转C数组)
│   ├── fonts/                     # 中文字体
│   │   ├── lv_font_cn_gb2312_16_*.c
│   │   └── gb2312_font_parts.cmake
│   ├── main.cpp                   # 程序入口
│   ├── app_ui.cpp                 # UI管理
│   ├── app_clock.cpp              # 时钟
│   ├── app_power.cpp              # 电源
│   ├── app_settings.cpp           # 设置
│   ├── display.cpp                # 显示驱动
│   ├── gt911_touch.cpp            # 触摸驱动
│   ├── wifi_sta.cpp               # WiFi
│   ├── sd_storage.cpp             # SD卡
│   ├── pomodoro_model.cpp         # 番茄钟模型
│   ├── pomodoro_sync.cpp          # 番茄钟同步
│   ├── pomodoro_bar.cpp           # 番茄钟进度条UI
│   ├── pomodoro_plan_cache.cpp    # 番茄钟计划缓存
│   ├── pixel_dog_model.cpp        # 像素狗模型
│   ├── pixel_dog_sprite.cpp       # 像素狗精灵动画
│   ├── pixel_dog_sync.cpp         # 像素狗同步
│   ├── pixel_ui.cpp               # 像素UI
│   ├── media_control.cpp          # 媒体控制
│   ├── media_sync.cpp             # 媒体同步
│   ├── ui_home_static_layout.c    # 首页静态布局
│   ├── board_io.cpp               # 板级IO
│   ├── assets_loader.cpp          # 资源加载
│   ├── CMakeLists.txt             # 组件CMake
│   └── Kconfig.projbuild          # 菜单配置
│
├── sdcard_assets/                 # SD卡资源文件
│   ├── assets/                    # 图片资源
│   ├── config/                    # 配置文件
│   ├── lyrics/                    # 歌词文件
│   ├── reference/                 # 参考图片
│   ├── generate_assets.py         # 资源生成脚本
│   └── README.md
│
├── scripts/                       # 构建烧录脚本
│   ├── build.ps1
│   ├── flash-monitor.ps1
│   ├── preview-ui.ps1
│   └── ...
│
├── fonts/                         # 字体生成工具
│   ├── generate_gb2312_font.py
│   └── glyphs_gb2312.txt
│
├── CMakeLists.txt                 # 顶层CMake
├── partitions.csv                 # Flash分区表
├── sdkconfig.defaults             # 默认配置
└── README.md
```

## 启动流程

```
app_main()
    │
    ├─ 1. 初始化NVS (nvs_flash_init)
    │
    ├─ 2. 初始化显示 (display_init)
    │   └─ 启动LVGL任务 (display_start_lvgl_task)
    │
    ├─ 3. 初始化SD卡 (sd_storage_init)
    │   └─ 成功则播种SD卡资源 (assets_seed_sdcard)
    │
    ├─ 4. 初始化UI (app_ui_init)
    │   ├─ 创建首页布局
    │   ├─ 初始化番茄钟UI
    │   ├─ 初始化像素狗UI
    │   └─ 初始化歌词UI
    │
    ├─ 5. 初始化触摸 (touch_init)
    │
    ├─ 6. 加载番茄钟计划缓存
    │
    ├─ 7. 启动番茄钟同步 (pomodoro_sync_start)
    │
    ├─ 8. 启动像素狗同步 (dog_sync_start)
    │
    └─ 9. 主循环 (LVGL task 驱动)
```

## LVGL任务架构

LVGL运行在独立的FreeRTOS任务中：

```
┌─────────────────────────────────────────┐
│        LVGL Task (独立任务)              │
│  优先级：较高                            │
│                                         │
│  while(1) {                             │
│    lv_timer_handler();  // 处理LVGL定时 │
│    vTaskDelay(5ms);     // 约200fps     │
│  }                                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│        业务同步任务 (多个)               │
│  优先级：较低                            │
│                                         │
│  番茄钟同步任务（每1-2秒轮询）           │
│  像素狗同步任务（定时同步）              │
│  媒体同步任务（定时同步）                │
└─────────────────────────────────────────┘
```

## 与后端通信方式

### HTTP轮询机制

所有业务模块均采用 **HTTP轮询** 方式与后端同步数据：

| 模块 | 轮询间隔 | 端点 | 说明 |
|------|---------|------|------|
| 番茄钟 | 1-2秒 | `/api/pomodoro/session` | 状态变化时立即上报 |
| 像素狗 | 15秒（带退避） | `/api/pixel-dog/state` + `/api/pixel-dog/interact` | 脏标记 + 互动即时通知 |
| 歌词/媒体 | 3-5秒 | 本地文件读取 | 从SD卡读取当前歌词 |

### 轮询策略

- **空闲时**：较长轮询间隔（省电、减少请求）
- **活动时**：较短轮询间隔（实时同步）
- **操作时**：立即上报 + 短时间加密轮询
- **指数退避**：网络异常时逐步增加间隔

## 资源管理

### 双轨资源策略

固件采用 **嵌入资源 + SD卡资源** 双轨制：

| 类型 | 存储位置 | 用途 | 优点 |
|------|---------|------|------|
| 嵌入资源 | Flash (编译时) | 核心UI图标、默认图片 | 启动快、可靠 |
| SD卡资源 | Micro SD卡 | 大图片、歌词、配置 | 可替换、容量大 |

**降级策略**：SD卡不可用时，自动降级使用嵌入资源。

### 资源播种（Seed）

首次插入SD卡时，自动将嵌入资源复制到SD卡：
- 确保SD卡有基础资源
- 用户可替换SD卡上的资源自定义UI
- 不影响固件正常运行

## 首页UI布局

首页采用 **静态布局 + 动态内容** 设计：

```
┌───────────────────────────────────────┐
│  状态栏  (WiFi图标、时间、电量等)       │
├───────────────────────────────────────┤
│                                       │
│  主内容区（根据当前页面切换）           │
│  ┌─────────────────────────────────┐  │
│  │                                 │  │
│  │   番茄钟 / 歌词 / 像素狗         │  │
│  │                                 │  │
│  └─────────────────────────────────┘  │
│                                       │
├───────────────────────────────────────┤
│  底部 Dock（5个图标导航按钮）          │
│  [首页][番茄钟][歌词][像素狗][设置]    │
└───────────────────────────────────────┘
```

### 底部Dock导航

| 图标 | 功能 |
|------|------|
| 🏠 首页 | 返回首页 |
| 🍅 番茄钟 | 番茄钟页面 |
| 🎵 歌词 | 歌词显示页面 |
| 🐕 像素狗 | 像素狗宠物页面 |
| ⚙️ 设置 | 设置页面 |

## 配置管理

### 配置来源优先级

1. **SD卡配置文件**（最高优先级，可动态修改）
   - `sdcard/config/pomodoro_host.txt` - 番茄钟API地址
   - `sdcard/config/media_host.txt` - 媒体API地址

2. **编译时配置**（头文件宏定义）
   - `panel_config.h` - 屏幕引脚、时序配置
   - `lv_conf.h` - LVGL功能配置
   - `*_api_config.h` - 各模块API默认配置

3. **NVS存储**（运行时持久化）
   - WiFi凭据
   - 番茄钟计划缓存
   - 用户设置

## 中文字体支持

### GB2312字体方案

使用自定义生成的 GB2312 简体中文字体：
- 字号：16px
- 字符集：GB2312 一级汉字（约3755个）
- 存储：分多个.c文件，按需编译
- 生成工具：`fonts/generate_gb2312_font.py`

**字体文件拆分原因**：单个字体文件过大，拆分为多个部分便于编译和链接。

## 开发工具

### 构建脚本

| 脚本 | 功能 |
|------|------|
| `scripts/build.ps1` | 编译固件 |
| `scripts/flash-monitor.ps1` | 烧录并监视串口 |
| `scripts/preview-ui.ps1` | UI预览（PC端模拟） |
| `scripts/deploy-sdcard.ps1` | 部署SD卡资源 |

### Python工具

| 工具 | 功能 |
|------|------|
| `sdcard_assets/generate_assets.py` | 生成SD卡资源清单 |
| `fonts/generate_gb2312_font.py` | 生成中文字体 |
| `scripts/preview_ui.py` | UI预览脚本 |

## 功耗管理

- 屏幕亮度调节
- 空闲自动熄屏
- WiFi低功耗模式
- 深度睡眠支持（可选）

## 系统特点

1. **模块化设计**：各业务模块独立，耦合度低
2. **双轨资源**：嵌入+SD卡，兼顾可靠性和灵活性
3. **HTTP轮询**：实现简单，适合设备端状态同步
4. **LVGL硬件加速**：充分利用ESP32-S3的LCD外设
5. **中文支持**：完整GB2312字库，满足日常使用
6. **OTA支持**：可扩展固件空中升级
