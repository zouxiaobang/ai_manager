# ai-housekeeper 架构文档

> 基于 xiaozhi-esp32 v2.2.1 二次开发（AI 管家 / ai-housekeeper）。
> 更新时间：2026-08-13

---

## 0. 重要说明：本仓库不是原版小智

本仓库并非上游原版，而是已被用户 **kyle** 深度定制过的 fork，额外包含：

- 自定义板 `kyle-s3-lcd`（`main/boards/kyle-s3-lcd/`），含触摸菜单、Home 页、电源/电池管理
- `KyleV1Display / KyleV2Display`（LVGL 表情 + 菜单 + Home 页 UI，`main/display/kyle_display/`）
- 电源 / 电池管理 `kyle_power_manager.h`（ADC 电量、充电检测、深睡省电）
- `my_emojis/`：146 张自定义表情 PNG
- 触摸菜单 UI `KyleTouchMenuUI`（电源 / 显示 / 音量 / 网络 / 表情 / 主题）

当前 `sdkconfig` 配置基线：`CONFIG_BOARD_TYPE_KYLE_S3_LCD=y`、target `esp32s3`、语言 `zh-CN`、唤醒词模型 `CONFIG_SR_WN_WN9_NIHAOXIAOZHI_TTS=y`（唤醒词「你好小智」，由 ESP-SR 模型决定，可通过自定义唤醒词更换）。

---

## 1. 项目简介与定位

**项目本质**：基于 ESP-IDF 的端侧语音交互固件。设备端采集麦克风 → OPUS 编码 → 推流到云端大模型 → 流式 TTS 回传 → 本地解码播放，同时通过 MCP（Model Context Protocol）协议把设备自身（音量 / 屏幕 / 摄像头 / GPIO）变成大模型的「可调工具」，实现语音控制万物。

**与「家庭 AI 管家」定位的匹配度**：

- 优势：已具备语音唤醒（ESP-SR）、流式 ASR + LLM + TTS、断句打断、OTA、多端控制（MCP）、可定制表情 / 菜单 / Home 页——天然适合做桌面 / 墙上语音管家。
- 需裁剪：70+ 板级支持、4G / ML307、摄像头 / 视频、机器人（Otto / 电子狗）、38 种语言，对单一家用场景是冗余（见第 6 节「可删减项」）。

**技术基线**：ESP-IDF `>=5.5.2`（`main/idf_component.yml:125`）、C++17（Google 代码风格）、LVGL 9.3、ESP-SR 2.2、乐鑫 `esp_audio_codec` / `esp_opus_enc/dec`。`PROJECT_VER "2.2.1"`（根 `CMakeLists.txt`）。

---

## 2. 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│  main.cc (app_main)  初始化 NVS → Application::GetInstance()        │
├─────────────────────────────────────────────────────────────────────┤
│  Application (application.cc/.h)  — 单例 · 主事件循环 · 状态机         │
│   · EventGroup 15 个事件位 (MAIN_EVENT_*)                            │
│   · Schedule() 跨线程回调注入主循环                                   │
│   · DeviceStateMachine: Starting→Idle→Connecting→Listening/Speaking │
├──────────────┬──────────────────────────┬───────────────────────────┤
│  Board 板级抽象  │  Protocol 通信协议       │  AudioService 音频服务     │
│ (boards/common/ │ (protocols/)            │ (audio/)                 │
│  board.h 工厂)  │  ├ WebsocketProtocol     │  ├ AudioCodec 编解码器    │
│  ├ WifiBoard    │  └ MqttProtocol          │  ├ AudioProcessor(AFE)   │
│  ├ Ml307Board   │    (MQTT控制+UDP加密音频) │  ├ WakeWord (ESP-SR)     │
│  ├ KyleS3Lcd 板  │    AES-128-CTR + 0x01头  │  └ OPUS 编解码 4 队列     │
│  └ 70+ 其它板    │                         │                          │
├──────────────┬──────────────────────────┼───────────────────────────┤
│  Display     │  McpServer (mcp_server)   │  Ota (ota.cc)             │
│  LVGL 9.3    │  JSON-RPC 2.0 设备端工具    │  查版本/激活/升级/回滚      │
│  Kyle UI     │  self.* 系列工具           │                          │
│  OLED/Emote  │  摄像头/主题/快照等         │                          │
├──────────────┴──────────────────────────┴───────────────────────────┤
│  基础设施: Settings(NVS) · Assets(分区资源:字体/表情/唤醒词/音效) ·      │
│            SystemInfo(堆/任务统计) · Led · Backlight · 电源/电池        │
└─────────────────────────────────────────────────────────────────────┘
```

**启动流程**：

1. `app_main`（`main.cc:15`）初始化 NVS → `Application::Initialize()`（`application.cc:61`）。
2. `Initialize` 依次：`Board::GetInstance()`（工厂 `create_board()`，`board.h:63`）→ 取 Display 打印 UA → `audio_service_.Initialize(codec)` 起 3 个任务（audio_input / audio_output / opus_codec）→ 注册音频 / 网络回调 → `McpServer::AddCommonTools() + AddUserOnlyTools()` → `board.StartNetwork()` 异步联网。
3. `Run()`（`application.cc:168`）把主任务提到优先级 10，进入 `xEventGroupWaitBits` 死循环，按事件位分发。
4. 网络连上 → `HandleNetworkConnectedEvent` 起 `activation` 任务：`CheckAssetsVersion` → `CheckNewVersion`（OTA 激活）→ `InitializeProtocol`（按 OTA 返回选 WebSocket 或 MQTT）→ 置 `ACTIVATION_DONE` → 状态回 Idle。
5. Idle 后：按键 / 唤醒词 → `OpenAudioChannel()` → `hello` 握手 → 建 UDP / WS 音频通道 → `SendStartListening` → 语音上行 OPUS、下行 OPUS 播放。

**事件总线**：不是通用总线，而是「Application 单例 + FreeRTOS EventGroup + `Schedule()` 队列」三件套。跨线程（音频任务、网络回调、MCP）都通过置事件位或 `Schedule([=]{...})` 把逻辑搬回主循环串行执行，避免锁竞争（`application.cc:905`）。

---

## 3. 目录结构说明

| 目录 | 职责 | 备注 |
|---|---|---|
| `main/` | 单组件 `main`，全部业务代码 | CMakeLists 用 `MINIMAL_BUILD ON`（根 CMakeLists） |
| `main/application.cc/.h` | 主应用 / 状态机 / 事件循环 | 44KB，核心 |
| `main/boards/common/` | 板级基类：`board / wifi_board / ml307_board / nt26_board / dual_network_board` + 电源(AXP2101/SY6970/ADC 电池)、背光、按键、旋钮、`KyleBoard.h` 接口、`kyle_power_manager.h`、`lamp_controller.h` | |
| `main/boards/<board>/` | 每板一个目录：`config.h`(GPIO) + `<board>.cc` | 70+ 板；`kyle-s3-lcd/` 是用户自定义板 |
| `main/audio/` | `audio_service` + `codecs/(es8311/es8374/es8388/…)` + `processors/(afe/no)` + `wake_words/(afe/custom/esp)` | |
| `main/protocols/` | `websocket_protocol` / `mqtt_protocol` / `protocol.h`(帧结构) | |
| `main/display/` | `display.h`(基类) + `lvgl_display/`(LVGL 主题/字体/图片/GIF/JPEG) + `kyle_display`(自定义 UI) + `menu/`(KyleTouchMenuUI) + `home/`(kyle_home_ui) + `oled_display` + `emote_display` + `lcd_display` | |
| `main/led/` | `single_led / circular_strip / gpio_led` | |
| `main/assets/` | `locales/<38 语言>/language.json + *.ogg`、`icons/`(C 数组图标)、`custom_font.h`、`lang_config.h`(生成) | |
| `main/mcp_server.cc/.h` | 设备端 MCP JSON-RPC 2.0 | |
| `main/ota.cc/.h` | 版本检查 / 激活 / OTA 升级 | |
| `main/settings.cc/.h` | NVS 封装（key-value） | |
| `main/system_info.cc/.h` | MAC / Flash / 堆 / 任务 CPU 统计 | |
| `partitions/v1, v2/` | 分区表；默认 `v2/16m.csv`（ota_0/1 各 4M + assets 8M） | v1/v2 不兼容 |
| `managed_components/` | 依赖管理器拉取的组件（勿手改） | |
| `my_emojis/` | 用户自定义 146 张 PNG 表情（`DEFAULT_ASSETS_EXTRA_FILES`） | |
| `scripts/` | `build_default_assets.py`(资源打包)、`gen_lang.py`(语言头生成)、`release.py`(CI 批量编译)、`versions.py`(OSS 发布)、`ogg_converter / acoustic_check / Image_Converter` 等工具 | |
| `.github/workflows/build.yml` | CI：矩阵编译所有板变体，`espressif/idf:v5.5.2` | |

---

## 4. 功能清单（逐模块，含文件引用）

### 4.1 主应用与状态机
- 11 种设备状态：`main/device_state.h:4-16`（Unknown / Starting / WifiConfiguring / Idle / Connecting / Listening / Speaking / Upgrading / Activating / AudioTesting / FatalError）。
- 状态迁移校验 + 观察者回调：`main/device_state_machine.cc`；`Application::HandleStateChangedEvent`（`application.cc:838`）统一驱动 LED 与显示（Idle 恢复待机、Listening 开语音处理、Speaking 复位解码器）。
- 打断机制：`AbortSpeaking(kAbortReasonNone | WakeWordDetected)`（`application.cc:913`）。

### 4.2 通信协议
- WebSocket 协议：`websocket_protocol.cc`。握手 `hello`（type / version / features{mcp,aec} / transport / audio_params），二进制帧支持 v2（`BinaryProtocol2`）与 v3（`BinaryProtocol3`）头（`protocol.h:17-31`）；请求头 `Authorization` / `Protocol-Version` / `Device-Id` / `Client-Id`（`websocket_protocol.cc:107-109`）。
- MQTT + UDP 协议：`mqtt_protocol.cc`。MQTT 传控制 JSON，UDP 传 AES-128-CTR 加密 OPUS；UDP 包格式 `|type 1u|flags 1u|payload_len 2u|ssrc 4u|timestamp 4u|sequence 4u|payload|`（`mqtt_protocol.cc:236-258`），密钥 / 随机数由服务端 hello 下发（`mqtt_protocol.cc:341-358`）。
- 会话状态回调：`OnIncomingAudio / Json / Connected / Disconnected / AudioChannelOpened / Closed / NetworkError`（`protocol.h:58-64`）。

### 4.3 音频流水线（核心）
双数据流（`audio_service.h:28-37`）：

```
上行: MIC →[AFE/降噪]→ 编码队列 → OPUS编码 → 发送队列 → Server
下行: Server → 解码队列 → OPUS解码 → 播放队列 → Speaker
```

- 3 个 RTOS 任务：`audio_input`(prio 8)、`audio_output`(prio 4)、`opus_codec`(prio 2)（`audio_service.cc:133-166`）。
- OPUS 帧 60ms、16000Hz、单声道、DTX / VBR（`audio_service.h:39,65-76`）；解码端支持动态采样率切换 + 重采样（`SetDecodeSampleRate`，`audio_service.cc:456`）。
- 电源管理：15 秒无输入 / 输出自动关 codec（`CheckAndUpdateAudioPowerState`，`audio_service.cc:764`）。
- 内置音效（提示音 / 激活码数字）直接内嵌 OGG，由 `PlaySound` 手写 Ogg 页解析（`audio_service.cc:641-736`）。
- 唤醒词：S3/P4 用 `AfeWakeWord` / `CustomWakeWord`，C3 用 `EspWakeWord`（`audio_service.cc:779-805`）；`EnableVoiceProcessing / EnableWakeWordDetection` 切换（`audio_service.cc:557-612`）。
- 音频调试：`USE_AUDIO_DEBUGGER` 通过 UDP 回传原始 PCM（`processors/audio_debugger.cc`）。

### 4.4 唤醒词（ESP-SR）
- Kconfig 三选一：`USE_AFE_WAKE_WORD` / `USE_CUSTOM_WAKE_WORD` / `USE_ESP_WAKE_WORD`（`Kconfig.projbuild:622-652`）。
- 当前选中 `CONFIG_SR_WN_WN9_NIHAOXIAOZHI_TTS=y`（「你好小智」模型）。
- 自定义唤醒词：`CUSTOM_WAKE_WORD`（拼音空格分隔）+ `CUSTOM_WAKE_WORD_DISPLAY`（服务端收到的文案）+ 阈值（`Kconfig.projbuild:654-674`）。
- 唤醒后可把唤醒词音频一并上传（`CONFIG_SEND_WAKE_WORD_DATA`，`application.cc:816-823`）。

### 4.5 设备端 MCP（大模型可调工具）
- JSON-RPC 2.0：`initialize` / `tools/list` / `tools/call`（`mcp_server.cc:353-436`）；分页上限 8000 字节（`mcp_server.cc:456`）。
- 公共工具（对 AI 可见）：`self.get_device_status`、`self.audio_speaker.set_volume`、`self.screen.set_brightness`、`self.screen.set_theme`、`self.camera.take_photo`（`mcp_server.cc:45-122`）。
- 用户工具（仅 App 可见，AI 不可见）：`self.get_system_info`、`self.reboot`、`self.upgrade_firmware`、`self.screen.get_info`、`self.screen.snapshot`、`self.screen.preview_image`、`self.assets.set_download_url`（`mcp_server.cc:128-301`）。
- 扩展点：板级 `InitializeTools()`（kyle 板注册了 `LampController` 台灯控制，`kyle-s3-lcd.cc`）。

### 4.6 OTA / 激活
- `Ota::CheckVersion` POST 系统信息 JSON 到 `CONFIG_OTA_URL`，解析返回 `activation / mqtt / websocket / server_time / firmware` 字段（`ota.cc:74-242`）。
- 激活：HMAC-SHA256 签名（`ota.cc:402-437`）；efuse 序列号（`ota.cc:26-38`）。
- 升级：`esp_ota_begin / write / end / set_boot_partition`，逐块下载（`ota.cc:264-368`）；回滚保护 `MarkCurrentVersionValid`（`ota.cc:244`）。
- 资源 OTA：`Assets::Download` 下载新的 assets.bin 到 assets 分区（`application.cc:345-403`）。

### 4.7 显示（LVGL）与表情
- 基类 `Display`（`display/display.h`）；LVGL 实现 `LvglDisplay`（状态栏 / 通知 / 低电量弹窗，`lvgl_display.h`）。
- 用户自定义 UI：`KyleLcdDisplay`（顶部栏 / 状态栏 / 内容区 / 底部栏 / 表情区 / 聊天标签 / 图片预览 / 主题，`kyle_display.h:23-91`）、`KyleV1Display`(按键导航菜单) 与 `KyleV2Display`(触摸版，`kyle_display.h:93-131`）。
- 菜单：`KyleTouchMenuUI`（`display/menu/`）+ `kyle-s3-lcd.cc` 定义菜单树（电源 / 显示 / 音量 / 网络 / 表情 / 主题，含亮度分级、休眠时间、表情包切换猪 / 兔 / 狗、白天黑夜主题）。
- Home 页：`display/home/kyle_home_ui.cc`（默认 `twt` 风格，可关）。
- 表情资源：`assets/emotions/` + `my_emojis/` + `emoji_collection.cc`；GIF 动图（`lvgl_gif`）、JPEG（`jpg/`）。
- 字体：内置 `lv_font_han_18.c` / `lv_font_han_24.c` + `xiaozhi-fonts` 组件（`font_puhui_*`）。

### 4.8 网络与配网
- `WifiBoard`：WiFi 直连 + 热点配网 + 声波配网（`afsk_demod`）+ BluFi（可选）；热点 SSID 前缀 `"AI-Housekeeper"`（已改名，`wifi_board.cc`）。
- 4G：`Ml307Board` / `Nt26Board` / `DualNetworkBoard`（ML307 / EC801E 模组）。
- 网络事件统一回调 `NetworkEvent`（扫描 / 连接 / 断开 / 模组错误，`board.h:20-33`）。

### 4.9 电源 / 电池 / 睡眠
- `PowerManager`（`boards/common/kyle_power_manager.h`）：ADC 电池电量（均值滤波 + 查表）、充电检测、片内温度、1 秒周期定时。
- `PowerSaveTimer`（`boards/common/power_save_timer.cc`）：亮屏 → 暗屏 → 深度睡眠三级；深睡用 `ext0` 按键唤醒（`kyle-s3-lcd.cc`）。
- 板级 `SetPowerSaveLevel`（LOW_POWER / BALANCED / PERFORMANCE，`board.h:36-40`）。

### 4.10 多语言
- 38 种语言目录（`assets/locales/`），`gen_lang.py` 生成 `lang_config.h`；非 en-US 语言缺失音效自动回退 en-US。
- 当前 `CONFIG_LANGUAGE_ZH_CN=y`。

### 4.11 构建 / 发布 / CI
- `scripts/release.py`：解析各板 `config.json` → `idf.py set-target + build + merge-bin + zip`；`_AUTO_SELECT_RULES` 处理 Kconfig 隐式依赖。
- `scripts/versions.py`：发布产物命名 `ai_housekeeper.bin`（已改名）上传 OSS 并 POST 版本服务。
- CI：`.github/workflows/build.yml` 全板矩阵编译，产物名 `ai_housekeeper_${name}_${sha}.bin`（已改名）。

---

## 5. 可优化项

| 位置 | 问题 | 建议 |
|---|---|---|
| `application.cc` | 下载进度回调里 `std::thread(...).detach()` 创建一次性线程去刷新显示 | 改用 `Application::Schedule()` 回主循环，避免线程抖动与栈开销 |
| `main/CMakeLists.txt` | 6 个 codec（es8311/es8374/es8388/es8389/box/dummy）全部编入，实际每板只用 1 个 | 按 `BOARD_TYPE` / Kconfig 条件编译，只留 kyle 板用到的 codec，减小固件体积与编译时间 |
| `audio_service.cc:641-736` | `PlaySound` 手写 Ogg 页解析，逐包 Push 解码队列 | 可预解码一次缓存 PCM，或改用 `esp_audio` 标准 API，减少每次播放的 CPU / 内存 |
| `system_info.cc` | `vTaskList` 需要 1000B 栈上缓冲 | 改为动态分配或限制条数 |
| `application.cc` | activation 任务栈 8192B | 实测后尝试降到 4096，节省内部 SRAM |
| `mcp_server.cc:456` | `tools/list` 8000B 上限硬编码 | 若加工具多，改为按 `nextCursor` 迭代合理切分 |
| `kyle-s3-lcd.cc` 及 `kyle_*` 系列 | 类名 `KyleS3LcdBroad`（Broad 拼写错误）、混用中英注释、非 Google 风格 | 改名 + 统一命名 / 注释（随本次改名为 ai-housekeeper 一并做） |
| `boards/common/KyleBoard.cpp` | 几乎空文件，`KyleBoard` 是纯虚接口 | 合并进头文件或删除，减少一个编译单元 |
| `application.cc` | `dynamic_cast<SpiLcdDisplay*>` 与 `dynamic_cast<KyleBoard*>` 硬编码派生的定制钩子 | 抽象成 Board 虚接口（如 `Board::HandleMenu()`），去掉对具体类的强耦合 |
| `wifi_board.cc` | 热点 SSID 前缀硬编码 | 提为 Kconfig 配置（本次已改名，建议进一步做成可配置项） |
| `settings.cc` | NVS 每次对象析构 `nvs_commit` | 高频写场景（音量）可批量提交，减少 flash 磨损 |
| 电源 | 待机时 `esp_timer` 时钟 tick 每秒唤醒来刷状态栏 | 评估是否可延长 tick 间隔或仅在有变化时刷新，进一步省电 |

---

## 6. 可删减项（面向「家庭 AI 管家」）

| 模块 | 删除方法 | 影响 | 风险 |
|---|---|---|---|
| 70+ 非 kyle 板 | 只保留 `boards/kyle-s3-lcd/` + `boards/common/`，删除其余板目录；CMakeLists 大量 `elseif` 一并清理 | 固件只认 kyle 板，menuconfig 板型列表变干净 | 中：需同步清理 `release.py` 变体发现（遍历 `main/boards`）与 CI |
| ESP32-C3/P4/C6 平台 | 删除 `sdkconfig.defaults.esp32*`（除 s3）、CMake 中 `CONFIG_IDF_TARGET_ESP32*` 分支、`esp_wake_word.cc`、`nt26_board` 等 | 少维护 4 个平台 | 低 |
| 4G / 双网（ML307/NT26/dual_network） | 删除 `ml307_board.cc/h`、`nt26_board.cc/h`、`dual_network_board.cc/h`、`78/esp-ml307`、`78/uart-eth-modem` 依赖 | 去掉串口 4G 逻辑，`NetworkEvent::Modem*` 可删 | 低 |
| 摄像头 / 视频 | 删除 `esp_video.cc`、`esp32_camera.cc`、`jpg/`、`image_player`/`esp_emote_*`、Kconfig `XIAOZHI_CAMERA_*` | 若无视觉需求，省 100KB+ | 低（若未来要「看家」再恢复） |
| Emote 表情风格 | 删除 `emote_display.cc`、`USE_EMOTE_MESSAGE_STYLE` 分支 | kyle 板用 LVGL 风格，不受影响 | 低 |
| 多余 codec | 见上节 | 省 flash | 低 |
| 38 种语言 | 只留 `zh-CN`(+`en-US` 回退)，删其余 `locales/` 与 CMake 分支 | 音效 / 资源分区更小 | 低 |
| 机器人（Otto / 电子狗） | 删除 `otto-robot` / `electron-bot` 板、`servo_dog_ctrl`、`otto-emoji-gif-component` 依赖 | 无关功能 | 低 |
| BluFi 配网 | 不选 `USE_ESP_BLUFI_WIFI_PROVISIONING`，删 `blufi.cpp` | 保留热点 + 声波配网即可 | 低 |
| v1 分区表 | 删 `partitions/v1/`（v1→v2 不可 OTA） | 明确只用 v2 | 低 |
| 低内存板配置（4m/8m） | 只留 `16m.csv` / `32m.csv` | 减少维护 | 低 |
| CI 全板矩阵 | `build.yml` 的 `prepare` 改为只编译 `kyle-s3-lcd` 变体 | CI 快一个量级 | 低 |

---

## 7. 如何自定义

- **唤醒词**：`idf.py menuconfig → Xiaozhi Assistant → Wake Word Implementation Type` 选 `Multinet model (Custom Wake Word)`，`CUSTOM_WAKE_WORD` 填拼音（空格分隔，如 `ai guan jia`）、`CUSTOM_WAKE_WORD_DISPLAY` 填显示词（如 `AI管家`）、阈值默认 20。也可换 ESP-SR 内置模型（`CONFIG_SR_WN_*`）。新模型可通过 assets 分区热更新。
- **表情 / 字体 / 背景**：`my_emojis/`（146 张 PNG，板配置 `DEFAULT_ASSETS_EXTRA_FILES`）→ 由 `scripts/build_default_assets.py` 打包进 assets 分区；在线自定义可用上游 `xiaozhi-assets-generator`。菜单 / Home 页的图标在 `main/assets/icons/`。
- **板级硬件配置**：改 `main/boards/kyle-s3-lcd/config.h`（麦克风 / 喇叭 I2S 引脚、按键、背光、屏幕 ST7789 型号、`LAMP_GPIO`）与 `kyle-s3-lcd.cc`。
- **MCP 工具扩展**：在 `kyle-s3-lcd.cc` 的 `InitializeTools()` 注册（参考 `LampController`，`boards/common/lamp_controller.h`），或改 `mcp_server.cc` 的 `AddCommonTools / AddUserOnlyTools`。工具即大模型可调函数。
- **云端服务 / 大模型**：改 `CONFIG_OTA_URL` 默认值，指向自己的 xiaozhi-server（Python / Java / Go 均有实现）；服务端返回 `mqtt` 或 `websocket` 配置即切换协议；大模型 / 角色人设在服务端配置。
- **多语言**：`assets/locales/<lang>/language.json` + 音效 `.ogg`，`gen_lang.py` 自动生成头文件；改 `CONFIG_LANGUAGE_*`。
- **音效**：`assets/locales/zh-CN/*.ogg`（提示音 / 激活码数字）；脚本 `scripts/ogg_converter/` 批量转换。

---

## 8. 编译烧录步骤（Windows）

1. **安装 ESP-IDF**：ESP-IDF 5.5.2+（官方离线 / 在线安装器，勾选 ESP32-S3）。安装后使用「ESP-IDF 5.5 CMD」快捷方式（`%USERPROFILE%\esp\v5.5.2\idf_cmd_init.bat`）。
2. **选 target**：`idf.py set-target esp32s3`（当前配置已是 s3）。
3. **配置**：`idf.py menuconfig` → `AI Housekeeper` → Board Type 选 `AI Housekeeper S3 (自定义开发板)`；语言、唤醒词、Flash Assets 均在菜单里。
4. **构建**：`idf.py build`。产物 `build/ai_housekeeper.bin`（改名后）。
5. **烧录 + 监控**：`idf.py -p COM<端口> flash monitor`（波特率 115200）。退出监控 `Ctrl+]`。
6. **分区表**：默认 `partitions/v2/16m.csv`（`CONFIG_PARTITION_TABLE_CUSTOM_FILENAME`）；板子是 16MB flash。assets 分区首次启动会按需下载 / 内置。
7. **批量发布**：`python scripts/release.py kyle-s3-lcd --name kyle-s3-lcd`；上传 OSS 用 `scripts/versions.py`（需环境变量 `OSS_*`、`VERSIONS_*`）。注意 OSS 固件文件名已改为 `ai_housekeeper.bin`，服务端 OTA 返回的下载 URL 需同步指向新文件名。
8. **常见问题**：
   - 端口占用 / 驱动：装 CP210x / CH340 驱动；`idf.py -p COMx flash`。
   - `MINIMAL_BUILD ON` 下改依赖需 `idf.py fullclean` 再 build。
   - Python 环境：用 ESP-IDF 自带 Python，勿用系统 Python。
   - CI 容器内构建用 `espressif/idf:v5.5.2`。
   - 首次拉取依赖：`managed_components/` 由 `dependencies.lock` 锁定版本自动生成，勿手改。

---

## 9. 版本与许可证

- **版本**：`PROJECT_VER "2.2.1"`；上游 v1 分支 1.9.2，v1 维护到 2026-02。本 fork 基于 v2 分区体系。
- **许可证**：MIT（`LICENSE`），Copyright (c) 2025 Shenzhen Xinzhi Future Technology Co., Ltd. & Project Contributors。**导入并改名后，若对外发布，建议保留上游版权声明（MIT 要求保留原版权行），并在 README 注明「基于 xiaozhi-esp32 二次开发」。**
- **依赖锁定**：`dependencies.lock`（提交入库）+ `main/idf_component.yml`；`managed_components/` 是生成物，勿手改。
- **上游协议文档**：`docs/websocket.md`、`docs/mqtt-udp.md`、`docs/mcp-protocol.md`、`docs/mcp-usage.md`、`docs/custom-board.md`。
