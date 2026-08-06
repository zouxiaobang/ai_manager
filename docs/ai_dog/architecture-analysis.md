# 小智 ESP32（G:\xiaozhi-esp32）现状架构分析

> 分析日期：2026-08-05
> 范围：main/、main/boards/、main/Kconfig.projbuild、main/CMakeLists.txt、main/ota.cc、main/protocols/、main/application.cc、docs/、sdkconfig* 等

## 1. 项目整体架构

### 1.1 顶层目录结构

| 目录/文件 | 说明 |
|---|---|
| `main/` | 全部应用代码（单组件，约 40 个 .cc/.h + 子目录） |
| `managed_components/` | IDF 组件管理器拉取的第三方组件（espressif 官方 + 社区） |
| `partitions/` | 分区表（v1/v2，按 4M/8M/16M/32M 区分） |
| `scripts/` | 发布/构建脚本（`release.py` 批量编译打包、`gen_lang.py` 语言生成等） |
| `docs/` | 协议与开发文档（`websocket.md`、`mcp-protocol.md`、`custom-board.md` 等） |
| `sdkconfig` / `sdkconfig.defaults*` | 构建配置；按芯片型号有 `.esp32c3`、`.esp32s3`、`.esp32p4` 等默认片段 |
| `.github/workflows/build.yml` | CI：全量/增量编译全部 board 变体 |
| `CMakeLists.txt` | 顶层 CMake，`project(xiaozhi)`，`PROJECT_VER "2.2.1"` |
| `my_emojis/` | kyle-s3-lcd 板自定义表情资源 |

### 1.2 构建系统

- 标准 **ESP-IDF** 工程，根 `CMakeLists.txt` 通过 `include($ENV{IDF_PATH}/tools/cmake/project.cmake)` 接入 IDF 构建。
- 依赖版本：`idf_component.yml` 声明 `idf: version: '>=5.5.2'`（即 **ESP-IDF 5.5.2+**，V5.5/V5.6 时代 API：`i2c_master`、`esp_lcd_*`、`esp_audio_*`、`esp-sr 2.2`）。
- 启用了 `idf_build_set_property(MINIMAL_BUILD ON)` 裁剪组件。
- 采用 **IDF Component Manager**（`main/idf_component.yml`）：依赖 LVGL 9.3、esp-sr 2.2、esp_audio_codec、esp_lcd_*、button 4.1、led_strip、knob、esp_video 等数十个组件。
- 单组件工程：全部代码在 `main/` 组件内，靠一个巨型 `main/CMakeLists.txt`（1049 行）组织源文件。

### 1.3 main 入口与 app_main

`main/main.cc`（17 行）：
1. `nvs_flash_init()`（含损坏自动擦除）；
2. `Application::GetInstance()`（单例）；
3. `app.Initialize()`：取 Board 单例 → 初始化显示/音频服务/回调/状态机监听/时钟定时器/MCP 工具/网络事件回调 → `board.StartNetwork()` 异步连网；
4. `app.Run()`：把主任务优先级提到 10，进入永不返回的事件循环。

`Application`（`main/application.h/.cc`，44KB）是**全局编排中枢**：所有业务事件、协议回调、状态机、OTA 流程、唤醒词处理都集中在这里。

### 1.4 FreeRTOS 任务模型

| 任务 | 优先级 | 栈 | 职责 |
|---|---|---|---|
| `main`（app_main → Run） | 10 | 8KB | 事件循环：消费事件组 + 调度队列 |
| `audio_input` | 8 | 2~3×2048 | 读麦克风 PCM → 送处理器/编码队列 |
| `audio_output` | 4 | 1~2×2048 | 播放下行 PCM 到扬声器 |
| `opus_codec` | 2 | 12×2048 | Opus 编解码 + 重采样 |
| `activation` | 2 | 2×4096 | 启动期 OTA/激活/协议初始化（一次性） |
| `board_task`（kyle-s3-lcd 专属） | 5 | 4096 | 轮询按钮队列处理菜单/音量/睡眠 |
| `wifi_cfg_delay` 等辅助任务 | 2 | 4096 | 进配网前的延时收尾 |

任务间通信：
- **事件组** `EventGroupHandle_t`：`MAIN_EVENT_*` 共 15 个 bit（调度/发音频/唤醒词/VAD/错误/激活完成/时钟/网络连/断/切换对话/开始监听/停止监听/状态变更/重置监听/打开菜单）。
- **调度队列** `std::deque<std::function<void()>>` + `MAIN_EVENT_SCHEDULE`：其他任务把 lambda 塞进主任务执行，保证单线程安全（`Schedule()`）。
- 音频侧：多个 `std::deque<unique_ptr<AudioStreamPacket>>` 队列 + mutex/CV。

### 1.5 通信架构

```
[WifiBoard] → WifiManager(managed 78__esp-wifi-connect)
   └── NetworkInterface / EspNetwork (HTTP / WebSocket / MQTT / UDP 抽象)
          ├── WebSocketProtocol  ← 音频+JSON（Opus 二进制帧）
          └── MqttProtocol + Udp ← 音频走 UDP 加密通道 + 控制走 MQTT
                      ↑
        由 OTA 激活响应决定用哪种（has_websocket_config / has_mqtt_config）
```

- 音频：双向 **Opus** 流；设备上行 16kHz 单声道（`OPUS_FRAME_DURATION_MS=60`），服务器下行可 24kHz。
- 控制：WebSocket 文本帧 JSON（`type` 字段驱动）或 MQTT JSON + UDP 音频。
- 应用层 `Application` 通过 `Protocol` 抽象接口（`OnIncomingAudio/Json/Connected/...`）与协议解耦。

## 2. boards 目录分析

### 2.1 完整清单（`main/boards/` 下共 118 个开发板目录 + `common/`）

aipi-lite, atk-dnesp32s3, atk-dnesp32s3-box, atk-dnesp32s3-box0, atk-dnesp32s3-box2-4g, atk-dnesp32s3-box2-wifi, atk-dnesp32s3m-4g, atk-dnesp32s3m-wifi, atom-echos3r, atommatrix-echo-base, atoms3-echo-base, atoms3r-cam-m12-echo-base, atoms3r-echo-base, bread-compact-esp32, bread-compact-esp32-lcd, bread-compact-ml307, bread-compact-nt26, bread-compact-wifi, bread-compact-wifi-lcd, bread-compact-wifi-s3cam, common, df-k10, df-s3-ai-cam, doit-s3-aibox, du-chatx, echoear, electron-bot, esp-box, esp-box-3, esp-box-lite, esp-hi, esp-p4-function-ev-board, esp-s3-lcd-ev-board, esp-s3-lcd-ev-board-2, esp-sensairshuttle, esp-sparkbot, esp-spot, esp32-cgc, esp32-cgc-144, esp32-s3-touch-amoled-1.8, esp32-s3-touch-lcd-1.46, esp32-s3-touch-lcd-1.85, esp32-s3-touch-lcd-1.85c, esp32-s3-touch-lcd-3.5, esp32s3-korvo2-v3, esp32s3-korvo2-v3-rndis, genjutech-s3-1.54tft, hu-087, jiuchuan-s3, kevin-box-2, kevin-c3, kevin-sp-v3-dev, kevin-sp-v4-dev, kevin-yuying-313lcd, kyle-s3-lcd, labplus-ledong-v2, labplus-mpython-v3, lichuang-c3-dev, lichuang-dev, lilygo-t-cameraplus-s3, lilygo-t-circle-s3, lilygo-t-display-p4, lilygo-t-display-s3-pro-mvsrlora, m5stack-core-s3, m5stack-tab5, magiclick-2p4, magiclick-2p5, magiclick-c3, magiclick-c3-v2, minsi-k08-dual, mixgo-nova, movecall-cuican-esp32s3, movecall-moji-esp32s3, movecall-moji2-esp32c5, otto-robot, sensecap-watcher, sp-esp32-s3-1.28-box, sp-esp32-s3-1.54-muma, supermini-c3, surfer-c3-1.14tft, taiji-pi-s3, tudouzi, waveshare-c6-lcd-1.69, waveshare-c6-touch-amoled-1.8, waveshare-c6-touch-amoled-1.32, waveshare-c6-touch-amoled-1.43, waveshare-c6-touch-amoled-2.06, waveshare-c6-touch-lcd-1.83, waveshare-p4-nano, waveshare-p4-wifi6-touch-lcd-4b, waveshare-p4-wifi6-touch-lcd-7b, waveshare-p4-wifi6-touch-lcd-xc, waveshare-s3-audio-board, waveshare-s3-epaper-1.54, waveshare-s3-rlcd-4.2, waveshare-s3-touch-amoled-1.32, waveshare-s3-touch-amoled-1.75, waveshare-s3-touch-amoled-2.06, waveshare-s3-touch-lcd-1.83, waveshare-s3-touch-lcd-3.5b, waveshare-s3-touch-lcd-3.49, waveshare-s3-touch-lcd-4b, wireless-tag-wtp4c5mp07s, xingzhi-cube-0.85tft-ml307, xingzhi-cube-0.85tft-wifi, xingzhi-cube-0.96oled-ml307, xingzhi-cube-0.96oled-wifi, xingzhi-cube-1.54tft-ml307, xingzhi-cube-1.54tft-wifi, xingzhi-metal-1.54-wifi, xmini-c3, xmini-c3-4g, xmini-c3-v3, yunliao-s3, zhengchen-1.54tft-ml307, zhengchen-1.54tft-wifi, zhengchen-cam, zhengchen-cam-ml307

每块板的差异点集中在 3 个文件：
- `config.h`：引脚宏（I2S/按钮/LED/屏幕/电源管理）
- `<name>.cc`：Board 子类，重写 `GetAudioCodec()/GetDisplay()/GetBacklight()/GetLed()/GetBatteryLevel()/...`
- `config.json`：`target`（芯片）+ `builds[].sdkconfig_append`（Flash 大小、分区表、语言、AEC 开关等）

差异维度主要有：**SoC**（C3/S3/C5/C6/P4/ESP32）、**音频**（外置 codec ES8311/ES8388/ES8374/ES8389/Box 或 I2S 直连无 codec，或 PDM 麦）、**屏幕**（OLED SSD1306 / SPI ST7789/ILI9341/GC9A01 / QSPI SH8601 / AMOLED / 无屏）、**网络**（WiFi / 4G ML307 / NT26 / RNDIS / 双网卡）、**电源**（AXP2101/SY6970/ADC 电池监测）、**摄像头**（esp32-camera/esp_video）、**输入**（按键/旋钮/触摸/ADC）。

### 2.2 boards 机制：构建时选择，运行时多态

**三步选择链**（全部构建时静态选择）：

1. **Kconfig**（`main/Kconfig.projbuild` 的 `choice BOARD_TYPE`，约 120 个 `config BOARD_TYPE_XXX`）：
   - `BOARD_TYPE_SUPERMINI_C3`（`depends on IDF_TARGET_ESP32C3`）
   - `BOARD_TYPE_KYLE_S3_LCD`（`depends on IDF_TARGET_ESP32S3`）
   - 通过 `idf.py menuconfig` / `sdkconfig` 中的 `CONFIG_BOARD_TYPE_*` 选择。
2. **CMakeLists**（`main/CMakeLists.txt:98-660` 巨型 `if/elseif` 链）：把 `CONFIG_BOARD_TYPE_XXX` 映射到目录名 `BOARD_TYPE` 变量，并设置字体/表情资源：
   ```cmake
   elseif (CONFIG_BOARD_TYPE_SUPERMINI_C3)
       set(BOARD_TYPE "supermini-c3")
       set(BUILTIN_TEXT_FONT font_puhui_basic_16_4)
       set(BUILTIN_ICON_FONT font_awesome_16_4)
       set(DEFAULT_EMOJI_COLLECTION twemoji_32)
   elseif (CONFIG_BOARD_TYPE_KYLE_S3_LCD)
       set(BOARD_TYPE "kyle-s3-lcd")
       ...
       set(DEFAULT_ASSETS_EXTRA_FILES "${PROJECT_DIR}/my_emojis")
   ```
   然后 `file(GLOB BOARD_SOURCES boards/${BOARD_TYPE}/*.cc)` 只编译选中板子的代码。
3. **运行时**：每个板子 `.cc` 末尾用 `DECLARE_BOARD(ClassName)` 宏生成 `void* create_board() { return new ClassName(); }`；`Board::GetInstance()`（`boards/common/board.h:62-65`）调 `create_board()` 拿到板子实例——**同一次固件只含一块板**。

结论：**构建时选择**（改 Kconfig 后需重新编译），**运行时只实例化一个**。新增板子要改 3 处（Kconfig + CMakeLists elseif + 新增目录），这也是原项目"板子越多 CMakeLists/Kconfig 越膨胀"的根因。

### 2.3 保留板一：supermini-c3

- **SoC**：ESP32-C3（RISC-V，单核 160MHz，无 PSRAM），4MB Flash（`config.json`：`target=esp32c3`，`CONFIG_ESPTOOLPY_FLASHSIZE_4MB=y`，分区 `partitions/v2/4m.csv`）。
- **音频**：**无外部 codec**，I2S 直连：
  - 麦克风：INMP441（I2S），`WS=GPIO5, SCK=GPIO6, DIN=GPIO4`，L/R 接 GND
  - 功放：MAX98357A，`DIN=GPIO7, BCLK=GPIO6, LRC=GPIO5`（与 MIC 分时/共用），`SD` 接 3.3V 常开（`AUDIO_PA_ENABLE_PIN=-1`）
  - codec 类：`I2sDirectCodec`（`supermini-c3/i2s_direct_codec.cc`），实际走 `NoAudioCodecDuplex`（16k 输入 / 16k 输出）
- **屏幕**：**SSD1306 OLED 128×64，I2C 0x3C**（`DISPLAY_SDA=GPIO8, SCL=GPIO9`，镜像 XY）。注意 `config.json` 里残留 `CONFIG_DISPLAY_SPI_ST7789=y`，与代码实际用的 OLED 不一致（历史遗留，不影响编译）。
- **按钮**：BOOT=GPIO2（单击切换对话，长按 1.5s 深睡）、VOL+ =GPIO1、VOL- =GPIO3。
- **LED**：状态 LED GPIO12（闪烁表示运行）。
- **深睡**：`esp_deep_sleep_enable_gpio_wakeup(GPIO2, LOW)` 唤醒。
- **唤醒词**：C3 无 PSRAM → `CONFIG_USE_ESP_WAKE_WORD=y`（Wakenet，非 AFE，不支持设备端 AEC）。
- 无背光控制、无电池检测、无触摸、无摄像头。

### 2.4 保留板二：kyle-s3-lcd

- **SoC**：ESP32-S3（双核 Xtensa 240MHz，8MB PSRAM，16MB Flash），`config.json`：`target=esp32s3`，`sdkconfig_append` 为空（用 `sdkconfig.defaults.esp32s3`：QIO 16MB + 8MB Octo PSRAM）。
- **音频**：**I2S 直连无外部 codec**（`NoAudioCodecSimplex`，16k 输入 / 24k 输出）：
  - MIC：`WS=GPIO4, SCK=GPIO5, DIN=GPIO6`
  - SPK：`DOUT=GPIO7, BCLK=GPIO15, LRCK=GPIO16`
  - `AUDIO_I2S_METHOD_SIMPLEX` 单工模式
- **屏幕**：**ST7789 SPI LCD 240×240**（`CONFIG_LCD_ST7789_240X240=y`，`LCD_TYPE_ST7789_SERIAL`）：
  - SPI3_HOST：`MOSI=GPIO47, CLK=GPIO21, DC=GPIO40, RST=GPIO45, CS=GPIO41`，40MHz，SPI_MODE 0，色序 RGB，颜色反转
  - 背光：`GPIO42`（PWM，可调亮度）
  - **触摸**：`CAN_TOUCH=true`，I2C 触摸芯片 **0x38 地址**（CST816S 系），`SDA=GPIO2, SCL=GPIO1`；`KyleV2Display` 注册 LVGL pointer 输入
- **按钮**：BOOT=GPIO3（单击=进入/退出菜单/确认，双击=返回/开菜单，长按=深睡）；触摸按键 UP=GPIO9、DOWN=GPIO11（菜单导航/音量）
- **LED**：板载 `BUILTIN_LED=GPIO48`（SingleLed，按状态变色）
- **电源**：`PowerManager`（充电检测 `CHARGING_GPIO` 未接），`PowerSaveTimer`（120s 暗屏 / 180s 睡眠）
- **MCP 外设**：`LAMP_GPIO=GPIO18` 控制灯
- **显示 UI**：LVGL 9.3 定制 `KyleV1Display/KyleV2Display` + 菜单系统（`display/menu/kyle_menu_ui.cc`、`KyleTouchMenuUI.cpp`）+ HomeUI（`display/home/kyle_home_ui.cc`），支持主题（白天/黑夜）、表情包（pig/rabbit/dog）、Toast。
- **唤醒词**：S3 + PSRAM → `CONFIG_USE_CUSTOM_WAKE_WORD=y`（Multinet 自定义唤醒词 "铃铛象" `ling dang xiang`，阈值 20），`CONFIG_USE_AUDIO_PROCESSOR=y`（AFE 降噪，设备端 AEC 默认关）。

### 2.5 common 目录（板级公共实现）

`boards/common/` 提供所有板子共享的基类与工具：`board.h/.cc`（Board 基类、UUID 生成、`DECLARE_BOARD` 宏、系统信息 JSON）、`wifi_board.h/.cc`（WiFi 板基类）、`ml307_board`/`nt26_board`/`rndis_board`/`dual_network_board`（蜂窝/双网）、`button.cc`、`backlight.cc`、`adc_battery_monitor.cc`、`axp2101.cc`、`sy6970.cc`、`power_save_timer.cc`、`sleep_timer.cc`、`system_reset.cc`、`afsk_demod.cc`、`blufi.cpp`、`KyleBoard.h`（kyle 专属事件接口）、`kyle_power_manager.h` 等。

## 3. 模块功能拆解

### 3.1 核心编排：`application.cc`（44KB，全局上帝类）

- `Application::GetInstance()` 单例；构造时创建事件组、时钟定时器（1s 心跳），按 Kconfig 决定 AEC 模式。
- `Initialize()`：显示/音频服务/MCP/网络回调装配 + `StartNetwork()`。
- `Run()`：主循环消费 15 个事件 bit；`Schedule()` 提供跨任务回调安全投递。
- 业务处理：`HandleNetworkConnectedEvent`（启动激活任务）、`HandleActivationDoneEvent`、`HandleToggleChatEvent`（对话开关机）、`HandleWakeWordDetectedEvent`（唤醒→开通道→发唤醒词数据）、`HandleStart/StopListeningEvent`、`HandleStateChangedEvent`（状态↔音频/显示/LED 联动）。
- `InitializeProtocol()`：按 OTA 结果 `new MqttProtocol()` 或 `new WebsocketProtocol()`，绑定全部回调（TTS/STT/LLM/MCP/system/alert/custom 分发）。
- 其他：`UpgradeFirmware`、`Reboot`、`CanEnterSleepMode`、`SetAecMode`、`ResetProtocol`。

### 3.2 音频：`main/audio/`

| 文件 | 职责 |
|---|---|
| `audio_service.h/.cc`（33KB） | 音频流水线中枢：Opus 编解码器、重采样、3 条 FreeRTOS 任务、4 条队列（decode/send/testing/encode）、唤醒词/语音处理开关、VAD 回调、播放音效 |
| `audio_codec.h/.cc` | `AudioCodec` 抽象基类：音量/增益/启停 + 纯虚 `Read/Write`；DMA 配置常量 |
| `codecs/` | 具体编解码器：`no_audio_codec`（I2S 直连 PDM/std，supermini/kyle 用）、`es8311/es8374/es8388/es8389`（外置 I2C codec）、`box_audio_codec`、`dummy_audio_codec` |
| `processors/` | `afe_audio_processor`（AFE 降噪/回采）、`no_audio_processor`（透传）、`audio_debugger`（UDP 音频调试） |
| `wake_words/` | `esp_wake_word`（C3 Wakenet）、`afe_wake_word`（S3 AFE）、`custom_wake_word`（Multinet 自定义词） |

音频双链路：
- 上行 `MIC → codec.InputData → audio_input task → AudioProcessor → encode queue → OpusCodecTask(opus 16k) → send queue → Run() 里 SendAudio`
- 下行 `WS OnData(binary) → decode queue → OpusCodecTask 解码+重采样 → playback queue → audio_output task → codec.OutputData → 喇叭`

### 3.3 显示：`main/display/`

- `display.h/.cc`：`Display` 抽象基类（SetStatus/ShowNotification/SetEmotion/SetChatMessage/锁）+ `NoDisplay`。
- `oled_display.cc`：SSD1306 单色 OLED。
- `lcd_display.cc`（56KB）：SPI TFT（ST7789/ILI9341/GC9A01/ST7796 等）`SpiLcdDisplay`，含 LVGL 接入。
- `kyle_display.cc`（29KB）/`kyle_display.h`：kyle-s3-lcd 专属 `KyleLcdDisplay/KyleV1Display/KyleV2Display`（菜单、Home、主题、表情、Toast、触摸）。
- `emote_display.cc`：表情资产显示器。
- `lvgl_display/`：LVGL 适配（emoji_collection、lvgl_theme、lvgl_font、lvgl_image、gif、jpg 解码）。
- `menu/` + `home/`：kyle 菜单 UI 与首页 UI（`MenuItem` 描述结构 + 回调）。
- `display.cc`：`DisplayLockGuard` 等公共工具。

### 3.4 LED：`main/led/`

`led.h`（`Led` 接口 `OnStateChanged()`）、`single_led.cc`（单色 GPIO）、`gpio_led.cc`、`circular_strip.cc`（环形灯带 led_strip）。板子通过 `GetLed()` 注入。

### 3.5 协议层：`main/protocols/`

- `protocol.h/.cc`：`Protocol` 抽象基类 + `AudioStreamPacket`、`BinaryProtocol2/3` 结构体；`SendStartListening/SendStopListening/SendAbortSpeaking/SendWakeWordDetected/SendMcpMessage` 拼 JSON；120s 无数据判定超时。
- `websocket_protocol.cc`（9.8KB）：WebSocket 实现。`OpenAudioChannel` 读 NVS `"websocket"` ns 的 `url/token/version`，设置 4 个握手头（Authorization/Protocol-Version/Device-Id/Client-Id），连接后发 `hello`、等 `server hello`（10s 超时）。支持 v1（裸 Opus）/v2（带时间戳头）/v3（简化头）二进制协议。
- `mqtt_protocol.cc`（14KB）：MQTT + UDP 双通道，AES 加密音频，90s keepalive、60s 重连。

### 3.6 网络层：`main/boards/common/wifi_board.cc` + 组件 `78__esp-wifi-connect`

- `WifiBoard`：基于组件 `WifiManager/WifiStation/SsidManager` 实现 WiFi 配网与连接（SSID 前缀 "Xiaozhi"、60s 超时进配网、Hotspot/Blufi/声波三种配网方式由 Kconfig 选）。
- `NetworkInterface`/`EspNetwork` 抽象（来自组件）统一暴露 `CreateHttp/CreateWebSocket/CreateMqtt/CreateUdp`。
- 蜂窝板（ml307/nt26/rndis/dual）不参与保留板。

### 3.7 OTA：`main/ota.cc`（16KB）

- `CheckVersion()`：POST 系统信息 JSON 到 `CONFIG_OTA_URL`；解析响应里的 `activation`（激活码/挑战码）、**`mqtt` / `websocket` 配置（写入 NVS）**、`server_time`（校准 RTC）、`firmware`（版本比对 + force 标志）。
- `Activate()`：`{OTA_URL}/activate`，用 efuse HMAC-SHA256 签名挑战码。
- `Upgrade()`：流式下载 → 校验 app 描述头 → `esp_ota_begin/write/end/set_boot_partition`，支持进度/速度回调与回滚。
- `MarkCurrentVersionValid()`：OTA 成功标记，启用 rollback 保护。

### 3.8 MCP 服务：`main/mcp_server.cc`（23KB）

MCP（Model Context Protocol）服务端：注册工具、处理 JSON-RPC 2.0 `tools/call`、能力发现。内置 `AddCommonTools()`（如 self.light、screen、battery、speaker 等）与用户工具；支持异步线程回调、图像 content 的 base64。

### 3.9 支撑模块

- `settings.h/.cc`：NVS 键值封装（`Settings(ns, read_write)`，Get/Set string/int/bool，ns 名如 `"websocket"`、`"mqtt"`、`"wifi"`、`"board"`、`"display"`）。
- `system_info.h/.cc`：Flash/堆/MAC/芯片型号/User-Agent。
- `device_state.h` + `device_state_machine.cc`：10 个设备状态 + 硬编码合法转移矩阵（`IsValidTransition`）+ 监听器。
- `assets.cc`：资产分区管理（下载/校验/应用表情与字体）。
- `Kconfig.projbuild`（32KB）：全部项目配置入口（OTA_URL、Flash Assets、语言 choice、BOARD_TYPE choice、屏幕类型 choice、唤醒词 choice、AEC、WiFi 配网方式、音频调试 UDP 目标等）。

## 4. 连接后端的链接地址

**核心结论：代码里没有任何硬编码的 `ws://`/`wss://` WebSocket 地址；服务器地址完全由「OTA 激活接口动态下发」到 NVS，设备端只在首次激活时用编译期默认的 OTA 基地址。**

### 4.1 WebSocket 连接地址

- 读取位置：`main/protocols/websocket_protocol.cc:83-84`
  ```cpp
  Settings settings("websocket", false);          // NVS namespace = "websocket"
  std::string url = settings.GetString("url");     // 键 = "url"，无默认值（空串）
  ```
  `settings.h/.cc` 是 NVS 封装，即 WebSocket URL 存在 **NVS `websocket` namespace 的 `url` 键**里。
- 地址格式：`WebsocketProtocol::Connect(url)`，即 `ws://host:port/...` 或 `wss://...`（由服务器下发决定）。端口/路径完全由下发的 url 字符串决定。
- 握手请求头（`websocket_protocol.cc:100-109`）：
  - `Authorization: Bearer <token>`（NVS `websocket/token`，可为空）
  - `Protocol-Version: <version>`（NVS `websocket/version`，1/2/3）
  - `Device-Id: <MAC>`
  - `Client-Id: <UUID>`

### 4.2 HTTP/API 基地址（OTA）

- 编译期默认：`main/Kconfig.projbuild:3-5`
  ```
  config OTA_URL
      string "Default OTA URL"
      default "https://api.tenclass.net/xiaozhi/ota/"
  ```
  生成宏 **`CONFIG_OTA_URL`**，默认 **`https://api.tenclass.net/xiaozhi/ota/`**（小智官方服务器）。
- 读取与覆盖：`main/ota.cc:43-50`
  ```cpp
  Settings settings("wifi", false);
  std::string url = settings.GetString("ota_url");   // NVS "wifi"/ota_url 优先
  if (url.empty()) { url = CONFIG_OTA_URL; }          // 否则用编译期默认
  ```
- 激活接口：`ota.cc:445-450`，`{OTA_URL}/activate`（POST）。
- 即 HTTP/API 基地址 = NVS `wifi/ota_url`（可配）→ 兜底 `CONFIG_OTA_URL`。

### 4.3 MQTT 地址（备用协议）

`main/protocols/mqtt_protocol.cc:65-71`：读 NVS `"mqtt"` namespace 的 `endpoint` 键（格式 `host:port`，默认 TLS 8883），另 `client_id/username/password/keepalive/publish_topic` 均存 NVS。无编译期默认，同样依赖下发。

### 4.4 动态下发机制（重点）

1. 设备首次上电 → `CheckNewVersion()` → `Ota::CheckVersion()` POST 到 `CONFIG_OTA_URL`（默认官方）。
2. 服务器响应 JSON 中携带 `websocket` 与 `mqtt` 对象（`ota.cc:143-183`）：
   ```json
   {
     "websocket": { "url": "wss://...", "token": "...", "version": 1 },
     "mqtt": { "endpoint": "...", "client_id": "...", "username": "...", "password": "..." }
   }
   ```
3. `Ota::CheckVersion` 把这些键值**写入 NVS**（`Settings("websocket", true)` / `Settings("mqtt", true)`）。
4. `Application::InitializeProtocol()`（`application.cc:483-497`）根据 `ota_->HasWebsocketConfig()/HasMqttConfig()` 实例化协议。
5. 之后每次 `OpenAudioChannel()` 都从 NVS 读 url。

**结论**：
- 默认连的是**小智官方服务器**（`https://api.tenclass.net/xiaozhi/ota/` 是唯一的编译期默认地址，WebSocket 地址由它下发）。
- **可以自建**：改 Kconfig `CONFIG_OTA_URL` 指向自建激活服务器，由自己的服务器下发 `websocket.url`；或直接把自建地址写进 NVS（`wifi/ota_url`、`websocket/url`）。原项目并未把自建服务器的连接做成开箱配置，需要改编译宏或手动写 NVS。
