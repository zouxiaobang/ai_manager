# firmware-ai-dog — kyle ESP32 全新架构固件（骨架）

从零重构kyle ESP32 固件，只保留 `supermini-c3`（ESP32-C3）与 `kyle-s3-lcd`（ESP32-S3）两块板。
核心原则：**业务/协议层为纯 C/C++ 逻辑、零 ESP-IDF 依赖、可在 PC host 上单元测试**；硬件差异收拢到
`boards/` 板级配置 + `hal/` 抽象接口。

> 架构依据：《new-architecture-design.md》第 1/2/3 章、《architecture-analysis.md》2.3/2.4 节。
> 当前为**基础骨架**：hal/core/boards/drivers/app 已就位，driver 为声明式桩（标注 TODO），core 已接 host 单测。

## 目录结构

```
firmware-ai-dog/
├─ CMakeLists.txt                # IDF 工程入口（project(kyle)）
├─ main/
│  ├─ CMakeLists.txt             # 按 CONFIG_BOARD_* 只编译一块板
│  ├─ Kconfig.projbuild          # 只保留 OTA_URL / BOARD choice / 语言 / 唤醒词
│  ├─ hal/                       # 纯抽象接口：IAudioCodec/IDisplay/ILed/IInput/IBacklight/IPower/INetwork/IBoard
│  ├─ core/                      # 纯逻辑（零 ESP-IDF）：wire_format / chat_session / ota_version / net_config / json_mini
│  ├─ boards/                    # board_factory + pin_map/pin_conflict + 两块板的 board_config.h 与组装
│  ├─ drivers/                   # 驱动桩：no_codec_i2s / ssd1306_oled / st7789_lcd / gpio_button / gpio_led / no_* 
│  └─ app/                       # app_main 入口 + Application 编排 + NvsStorage
└─ test/                         # host 单测（纯 CMake，不依赖 IDF）
   ├─ CMakeLists.txt             # 把 main/core + boards 纯逻辑直接编进可执行文件
   ├─ unity.h / unity.c          # 自包含极简 Unity 兼容框架
   ├─ core/                      # wire_format / chat_session / ota_version / net_config 单测
   ├─ board/                     # pin_conflict 单测
   └─ mocks/                     # MockStorage 等注入桩
```

## 分层依赖（单向向下）

```
app → core / hal
boards → hal / drivers
drivers → hal
core → 仅标准 C/C++（json_mini 自给）
```

## host 单元测试（重点，无需 ESP-IDF）

```bash
cd firmware-ai-dog
cmake -S test -B build          # 配置
cmake --build build             # 编译
./build/unit_tests              # 运行（当前 34 个用例全绿）
```

> 注意：host 测试与 `idf.py build` 都使用 `build/` 目录。两者在同一 checkout 下切换时会互相覆盖；
> 本地同时做两侧开发时，建议 host 测试用独立目录，如 `cmake -S test -B build_host`（已在 CI 中按 job 隔离）。

或直接用编译器：

```bash
g++ -std=c++17 -I test -I main -I main/core \
    test/unity.c test/core/*_test.cpp test/board/pin_conflict_test.cpp \
    main/core/*.cc main/boards/pin_conflict.cc -o unit_tests.exe
```

覆盖：`wire_format` 黄金字节断言（v1/v2/v3）、`chat_session` 状态转移矩阵、`ota_version` 版本比较/force、
`net_config` 默认值回退、`pin_conflict` 板级引脚冲突。

## ESP-IDF 固件构建

```bash
cd firmware-ai-dog
idf.py set-target esp32c3      # 或 esp32s3
idf.py menuconfig              # Board selection 二选一
idf.py build
idf.py -p COMx flash monitor
```

- 板型由 `main/Kconfig.projbuild` 的 `BOARD` choice 在构建时二选一。
- `board_factory.cc` 用 `CONFIG_BOARD_*` 宏 `#if` 返回对应 `IBoard*`。

## 协议要点（core/wire_format）

- **v1**：裸 Opus 帧。
- **v2** 头（16B 全大端）：`version(16) / type(16) / reserved(32) / timestamp(32) / payload_size(32)`。
- **v3** 头（4B）：`type(8) / reserved(8) / payload_size(16 大端)`。
- 消息 JSON：`hello` / `server_hello` / `listen` / `abort` / `stt` / `tts` / `llm` 组装辅助 + `ParseServerHello`。

## 遗留 TODO

- driver 层为声明式桩，待接入 ESP-IDF 真实驱动（I2S/SPI/I2C/GPIO/LEDC）。
- `Application` 事件循环待接 FreeRTOS 任务/`vTaskDelay`、网络回调、唤醒词链路。
- 背光（GpioBacklight）与触摸（Cst816s）驱动未创建，`KyleS3LcdBoard::backlight()` 返回空。
- NVS 的 `NvsStorage` 在设备侧验证（host 单测用 `MockStorage`）。
