# ai-housekeeper 改名触点清单

> 来源：xiaozhi-esp32 → ai-housekeeper 重命名的完整分析（基于 v2.2.1 fork）。
> 原则：**跨进程 / 跨仓库 / 跨机器的契约一律不改**（组件注册表名、协议 JSON / 二进制帧 / HTTP 头、ESP-SR 模型名），否则要么构建失败、要么握手失败、要么激活失败。

## 已完成的重命名（本次导入已执行）

| # | 位置 | 改动 |
|---|---|---|
| 1 | 根 `CMakeLists.txt` | `project(xiaozhi)` → `project(ai_housekeeper)`（决定 `app_desc->project_name`，会出现在系统信息 `application.name` 与 OTA 上报） |
| 2 | `main/Kconfig.projbuild` | 顶级菜单 `menu "Xiaozhi Assistant"` → `menu "AI Housekeeper"` |
| 3 | `main/Kconfig.projbuild` | 板名 `bool "Kyle S3 LCD (自定义开发板S3)"` → `bool "AI Housekeeper S3 (自定义开发板)"`（配置符号 `CONFIG_BOARD_TYPE_KYLE_S3_LCD` 不变） |
| 4 | `main/boards/common/wifi_board.cc` | 热点 SSID 前缀 `"Xiaozhi"` → `"AI-Housekeeper"` |
| 5 | `.github/workflows/build.yml` | CI 产物名 `xiaozhi_...` → `ai_housekeeper_...` |
| 6 | `scripts/versions.py` | OTA 固件文件名 `xiaozhi.bin` → `ai_housekeeper.bin`（OSS 对外下载名，**服务端 OTA URL 需同步指向新文件名**） |
| 7 | `scripts/sonic_wifi_config.html` | 「小智声波配网」→「AI管家声波配网」 |
| 8 | 头文件 guard × 5 | `XIAOZHI_CUSTOM_FONT_H / XIAOZHI_KYLEBOARD_H / XIAOZHI_KYLETOUCHMENUUI_H / XIAOZHI_MENUUI_H / XIAOZHI_EVENT_H` → `AI_HOUSEKEEPER_*` |
| 9 | `README.md / README_zh.md / README_ja.md` | 顶部加品牌说明横幅（注明基于 xiaozhi-esp32 二次开发） |

## (b) 建议改但需验证 —— 内部标识，不影响协议连通性

> 当前**未执行**。以下改动不会破坏协议，但需全仓同步或联动，建议在有 `idf.py build` 验证的环境下一次完成。

| # | 位置 | 内容 | 注意 |
|---|---|---|---|
| 1 | `main/Kconfig.projbuild` + 代码（`esp_video.cc` / `jpeg_to_image.c` / `image_to_jpeg.cpp`） | `CONFIG_XIAOZHI_CAMERA_*`（JPEG 编码 / 旋转 / 调试）等约 50 处编译符号 | 自包含 Kconfig 符号；改名需同步改定义 + 引用 + `sdkconfig.defaults*` |
| 2 | `main/CMakeLists.txt` | CMake 变量 `XIAOZHI_FONTS_PATH` | 内部变量；但匹配字符串 `"xiaozhi-fonts"` 是组件名，**不能改**（见 c 类） |
| 3 | `main/boards/*/README.md` | 各板编译教程里的 `Xiaozhi Assistant -> Board Type` 菜单名 | 菜单已改名，这些教程需同步；若不删其他板，属文档工作 |
| 4 | `main/Kconfig.projbuild` | 默认 `CONFIG_OTA_URL="https://api.tenclass.net/xiaozhi/ota/"` | 建议改为自家服务器；注意首次激活后 URL 存于 NVS，只影响新设备 |
| 5 | `main/display/emote_display.cc:149` | `strstr(content, "xiaozhi.me")` | 该分支把「含 xiaozhi.me 的 system 消息」按系统消息展示；若 emote 风格删除则无需管 |
| 6 | `main/boards/kyle-s3-lcd/`（目录名）与 `KyleS3LcdBroad` 类名 | kyle 品牌 | 纯内部；改名需同步 `config.json`(name)、`release.py` 变体名、CMake/Kconfig 映射、全仓类名引用 |
| 7 | `scripts/ogg_converter/xiaozhi_ogg_converter.py` | 工具标题「小智AI OGG…」 | 独立工具，改名无风险 |

## (c) 不要改 —— 协议 / 服务器 / 第三方依赖标识（改了会断连通性）

| # | 位置 | 内容 | 为什么不能改 |
|---|---|---|---|
| 1 | `main/idf_component.yml` | `78/xiaozhi-fonts`、`78/esp-ml307`、`78/esp-wifi-connect`、`78/uart-eth-modem` 等 | ESP-IDF **组件注册表依赖名**，由 idf 组件管理器从仓库拉取；改名后无法解析组件，编译直接失败 |
| 2 | `main/CMakeLists.txt` | `find_component_by_pattern("xiaozhi-fonts" ...)` | 匹配的是 `managed_components/78__xiaozhi-fonts` 目录（依赖管理器按 `idf_component.yml` 生成）；改名就匹配不到 |
| 3 | `websocket_protocol.cc` / `mqtt_protocol.cc` | hello 消息 JSON 键：`type`/`version`/`transport`/`audio_params.format.sample_rate.channels.frame_duration`/`features.mcp` | 与服务器的**协议握手契约**，服务器按这些字段协商；改名即握手失败 |
| 4 | `websocket_protocol.cc`、`ota.cc` | HTTP 头 `Protocol-Version`/`Device-Id`/`Client-Id`/`Activation-Version`/`Serial-Number` | 服务器身份校验用；改名导致激活 / 连接失败 |
| 5 | `protocol.h` | `BinaryProtocol2/3` 二进制帧结构 | 与服务器约定的二进制协议；结构不能动 |
| 6 | `mqtt_protocol.cc` | UDP 音频包格式 + AES-CTR | 与服务器约定的加密音频通道格式 |
| 7 | `sdkconfig.defaults*` / `sdkconfig` | `CONFIG_SR_WN_*`（如 `CONFIG_SR_WN_WN9_NIHAOXIAOZHI_TTS`） | 乐鑫 ESP-SR 模型符号，由 ESP-SR 组件提供，改名无法识别模型 |
| 8 | `managed_components/` | 目录名（`78__xiaozhi-fonts`、`espressif__esp-sr` 等） | 组件管理器生成物；手改会被覆盖且破坏构建 |
| 9 | `scripts/` | `--xiaozhi_fonts_path` 参数名 | 与 `build_default_assets.py` 及上游 assets 生成器约定；改需两头同步，不建议动 |
| 10 | README 中 `xiaozhi.me` / `api.tenclass.net` 链接 | 第三方服务地址 | 连官方服务器 / 激活依赖这些地址；换自家服务器时是「配置替换」而非「改名」 |

## 改名执行建议顺序（若后续做 (b) 类）

1. 先改项目名 / 菜单 / SSID / 板名（已在上文完成）
2. `idf.py fullclean && idf.py build` 验证
3. 再处理头文件 guard 与 Kconfig 符号（b 类），每改一类跑一次 build
4. (c) 类一律不动
