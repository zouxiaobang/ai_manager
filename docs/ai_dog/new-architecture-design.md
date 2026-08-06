# 小智 ESP32 全新架构设计（从零重构）

> 设计日期：2026-08-05
> 目标：只保留 `supermini-c3` 与 `kyle-s3-lcd` 两块板；从混乱的现状代码中重构出一套清晰、模块化、可测试的架构。

## 1. 架构概览

### 1.1 目标与原则

- 只保留 `supermini-c3` 与 `kyle-s3-lcd` 两块板；新增板只需加一个目录 + 一个 Kconfig 项，不再改 CMakeLists elseif 长链。
- 把「板级硬件差异」从「业务逻辑」彻底剥离开：业务代码只依赖抽象接口，编译期注入板级配置。
- 协议层、状态机、会话管理做成**纯 C/C++ 逻辑**，可在 PC（host）上跑单元测试，不依赖 ESP-IDF/硬件。
- 保留原项目的协议兼容性（WebSocket + Opus + hello/listen/tts/stt/mcp 消息），降低迁移风险。

### 1.2 分层模型

```
┌──────────────────────────────────────────────────────────┐
│  App 层  app_main / Application / DeviceStateMachine      │  组合业务、状态机、事件循环
├──────────────────────────────────────────────────────────┤
│  业务模块  ChatSession(会话) / WakeWordTrigger(唤醒触发)   │  不感知硬件
│            VoicePipeline(音频流水线) / OtaManager          │
├──────────────────────────────────────────────────────────┤
│  协议层  Protocol(抽象) / WebsocketProtocol / MqttProtocol │  纯逻辑，可 host 测试
│          + Codec(Opus封装) + WireFormat(二进制/JSON编码)   │
├──────────────────────────────────────────────────────────┤
│  HAL 硬件抽象  IAudioCodec / IDisplay / ILed / IInput      │  接口只描述能力，不含实现
│                 IBacklight / IPower / INetwork             │
├──────────────────────────────────────────────────────────┤
│  Driver 驱动   Es8311Codec / NoCodecI2s / St7789Lcd        │  每类硬件一个实现
│                 Ssd1306Oled / GpioLed / TouchCst816s        │
├──────────────────────────────────────────────────────────┤
│  Board 板级     board_config.h(引脚/外设枚举) + BoardInfo    │  一块板 = 一份静态配置
│                 由 Kconfig 在构建时选一                    │
└──────────────────────────────────────────────────────────┘
```

- **层间依赖方向单向向下**；上层只依赖下层抽象，不依赖具体驱动。
- 板级只做「组装」（依赖注入），不做业务判断。

### 1.3 关键接口签名示例

```cpp
// hal/audio_codec.h —— 音频硬件能力
class IAudioCodec {
public:
    virtual ~IAudioCodec() = default;
    virtual bool  Start() = 0;
    virtual void  Stop() = 0;
    virtual size_t Read(int16_t* dst, size_t samples) = 0;   // 麦克风 PCM
    virtual size_t Write(const int16_t* src, size_t samples) = 0; // 扬声器 PCM
    virtual void   SetOutputVolume(int v) = 0;   // 0..100
    virtual int    input_sample_rate() const = 0;
    virtual int    output_sample_rate() const = 0;
};

// hal/display.h —— 显示能力（LVGL 屏 / OLED / 无屏统一）
class IDisplay {
public:
    virtual ~IDisplay() = default;
    virtual void SetStatus(const char* s) = 0;
    virtual void SetChatMessage(const char* role, const char* text) = 0;
    virtual void SetEmotion(const char* e) = 0;
    virtual void ShowToast(const char* msg, int ms) = 0;
    virtual int  width() const = 0;
    virtual int  height() const = 0;
};

// hal/input.h —— 按键/触摸统一输入事件
struct InputEvent { enum { kClick, kDoubleClick, kLongPress } type; int button_id; };
class IInput {
public:
    virtual ~IInput() = default;
    virtual void OnEvent(std::function<void(const InputEvent&)> cb) = 0;
};

// hal/board.h —— 板级组装结果（单例工厂 + 只读信息）
struct BoardInfo {
    const char* name;            // "supermini-c3"
    const char* target;          // "esp32c3"
    int  flash_size_mb;
    bool has_psram;
    bool has_display, has_touch, has_battery, has_backlight;
    int  default_input_rate, default_output_rate;
};
class IBoard {
public:
    virtual ~IBoard() = default;
    virtual const BoardInfo& info() const = 0;
    virtual IAudioCodec*  audio() = 0;
    virtual IDisplay*     display() = 0;
    virtual ILed*         led() = 0;
    virtual IInput*       input() = 0;
    virtual IBacklight*   backlight() = 0;   // 可空
    virtual IPower*       power() = 0;       // 可空
    virtual INetwork*     network() = 0;
    virtual void          Init() = 0;        // 总线/GPIO/电源初始化
};
```

## 2. 新目录树与实施计划

### 2.1 新目录树

```
xiaozhi/
├─ CMakeLists.txt                  # project(xiaozhi)，只保留必要组件
├─ sdkconfig.defaults / .esp32c3 / .esp32s3
├─ idf_component.yml               # 精简依赖（按保留板裁剪）
├─ partitions/v2/{4m,16m}.csv
│
├─ main/
│  ├─ CMakeLists.txt               # 精简版：按 BOARD_ID glob 一块板 + 公共组件
│  ├─ Kconfig.projbuild            # 只保留：OTA_URL / BOARD choice(2) / 语言 / 唤醒词
│  ├─ app/
│  │  ├─ app_main.cc               # 极简入口：nvs → BoardFactory → Application
│  │  ├─ application.h/.cc         # 事件循环 + 状态机装配（瘦身版）
│  │  └─ device_state.h/.cc        # 状态机（可 host 测试）
│  ├─ core/                        # 纯逻辑（零 ESP-IDF 依赖，可 PC 单测）
│  │  ├─ chat_session.h/.cc        # 会话状态机：idle→connecting→listening→speaking
│  │  ├─ wire_format.h/.cc         # BinaryProtocol v1/v2/v3 编解码 + 消息 JSON 组装
│  │  ├─ audio_pipeline.h/.cc      # 队列模型、帧调度（不碰驱动）
│  │  ├─ ota_version.h/.cc         # 版本比较/配置解析（可测）
│  │  └─ net_config.h/.cc          # 服务器地址的 NVS 读写 + 默认值策略（可测）
│  ├─ protocol/
│  │  ├─ protocol.h                # 抽象：Start/Open/Close/SendAudio/SendText
│  │  ├─ websocket_protocol.h/.cc  # 只依赖 INetwork::CreateWebSocket
│  │  └─ transport/                # 可插拔传输（mock 注入）
│  ├─ voice/
│  │  ├─ audio_service.h/.cc       # 重采样/Opus/队列/任务（依赖 IAudioCodec）
│  │  ├─ wake_word.h/.cc           # 接口 + esp/afe/custom 三种实现
│  │  └─ aec.h/.cc
│  ├─ mcp/
│  │  └─ mcp_server.h/.cc          # JSON-RPC 2.0（纯逻辑 + 工具注册）
│  ├─ ota/
│  │  └─ ota_manager.h/.cc         # 检查/激活/升级（依赖 INetwork::CreateHttp）
│  ├─ hal/                         # 纯抽象接口（见 1.3）
│  │  ├─ audio_codec.h  display.h  led.h  input.h
│  │  ├─ backlight.h   power.h     network.h
│  │  └─ board.h
│  ├─ drivers/                     # 每类硬件一个目录，按 CONFIG 编译
│  │  ├─ codec/  no_codec_i2s.h/.cc   es8311.h/.cc
│  │  ├─ display/ st7789_lcd.h/.cc    ssd1306_oled.h/.cc   no_display.h
│  │  ├─ led/    gpio_led.h/.cc
│  │  ├─ input/  gpio_button.h/.cc    cst816s_touch.h/.cc
│  │  └─ power/  no_power.h/.cc
│  └─ boards/
│     ├─ board_factory.h/.cc       # create_board() → IBoard*（Kconfig 分支，只 2 个）
│     ├─ supermini-c3/
│     │  ├─ board_config.h         # 引脚宏（见现状文档 2.3）
│     │  └─ supermini_c3_board.cc  # 组装 NoCodecI2s + Ssd1306Oled + GpioButton
│     └─ kyle-s3-lcd/
│        ├─ board_config.h         # 引脚宏（见现状文档 2.4）
│        └─ kyle_s3_lcd_board.cc   # 组装 NoCodecI2s + St7789Lcd + Cst816s + LvglDisplay
│
└─ test/                          # host 单元测试（不连硬件）
   ├─ CMakeLists.txt               # idf.py build-targets / 或纯 CMake + Unity
   ├─ unity/  wire_format_test.cpp  chat_session_test.cpp  ota_version_test.cpp
   ├─ mocks/  mock_audio_codec.h mock_network.h mock_display.h
   └─ board/  pin_conflict_test.cpp  board_config_validity_test.cpp
```

### 2.2 实施步骤

1. 建立 `hal/` + 空实现（第 1 步即可编译）。
2. 收拢 `boards/`：删到 2 块 + `board_factory`，Kconfig choice 只留 2 项。
3. 抽 `core/` 纯逻辑并接上 Unity host 测试。
4. 协议/音频/OTA 逐步替换为依赖抽象的新实现。

## 3. 测试模块设计（重点）

### 3.1 纯逻辑/协议层 PC 单元测试

**框架选择（推荐 Unity + CMock）**：
- 原项目已在 `managed_components` 依赖 espressif 组件生态；ESP-IDF 官方 host-test 体系成熟但较重。
- 更轻方案：**纯 CMake + Unity（ThrowTheSwitch）** 单独构建 `test/`，把 `main/core/*.c` 直接编进测试可执行文件（不经过 IDF），在 GitHub Actions 的 ubuntu-latest 跑。
- 理由：`core/` 文件零 ESP-IDF 依赖，纯 CMake 构建最快、最稳，CI 无需装 ESP-IDF。

**被测对象与用例**：

| 被测文件 | 用例示例 |
|---|---|
| `core/wire_format.cc` | 编码 v1 Opus 帧字节长度；v2 头大小端（黄金字节向量对比）；v3 简化头；解析服务端 hello 提取 session_id/audio_params |
| `core/chat_session.cc` | 状态转移合法性（idle→connecting→listening→speaking→idle）；非法转移拒绝；唤醒打断 speaking → abort reason 正确 |
| `core/ota_version.cc` | `IsNewVersionAvailable("2.0.0","2.0.1")=true`；`"2.0.0"→"2.0.0"=false`；force 标志 |
| `core/net_config.cc` | url 为空回退 `CONFIG_OTA_URL`；NVS 有值优先；`websocket` 配置解析写入 |

```cpp
// test/unity/wire_format_test.cpp
#include "unity.h"
#include "core/wire_format.h"

TEST_CASE("v2 header big-endian encoding", "[wire]") {
    uint8_t buf[64];
    size_t n = wire_format_encode_v2(buf, sizeof(buf), 0x1234u, pcm, pcm_len);
    TEST_ASSERT_EQUAL(0x12, buf[4]);  // timestamp hi byte
    TEST_ASSERT_EQUAL(0x34, buf[5]);
    ...
}
```

### 3.2 硬件相关代码的 mock/桩隔离

- 驱动层薄封装 + **接口 mock**：用 CMock 生成 `IAudioCodec/INetwork/IDisplay` 的 mock；`audio_service`/`websocket_protocol` 单测时注入 mock。
- `core/audio_pipeline` 用 **fake codec**：`MockAudioCodec` 返回固定采样率，`Read` 喂正弦波，断言 `Write` 收到的样本数与帧时长匹配、队列不满。
- 例：`test/mocks/mock_network.h`
  ```cpp
  class MockNetwork : public INetwork {
  public:
      MOCK_METHOD(std::unique_ptr<IWebSocket>, CreateWebSocket, (int id));
      MOCK_METHOD(std::unique_ptr<IHttp>, CreateHttp, (int id));
  };
  ```
- 硬件中断/时序相关（I2S、SPI、I2C）不单测，只做**编译期冒烟**（在 IDF 构建中跑通 + 板级初始化日志断言）。

### 3.3 板级配置校验测试（引脚冲突检测）

把 `board_config.h` 变成可解析的数据，新增**静态断言 + host 测试**：
```cpp
// boards/supermini-c3/board_config.h
#define BOARD_PIN_MIC_WS   5
#define BOARD_PIN_MIC_SCK  6
#define BOARD_PIN_SPK_BCLK 6   // 与 MIC_SCK 共用（I2S 分时，合法）

// test/board/pin_conflict_test.cpp（host）
TEST_CASE("kyle-s3-lcd pin conflicts", "[board]") {
    // 收集 {name, pin} 列表，检测：
    //  1) 同一 pin 同时被「不同功能且不允许共享」占用
    //  2) 超出 GPIO 范围、负值误用（GPIO_NUM_NC 例外）
    TEST_ASSERT_TRUE(validate_pin_map(KYLE_BOARD_PINS));
    // 期望：SPI MOSI 47 / CLK 21 不冲突；触摸 SDA 2 / SCL 1 与显示/音频不冲突
}
```
- 编译期再补一层 `static_assert`：`BOARD_PIN_DISPLAY_CS != BOARD_PIN_DISPLAY_DC` 等。
- 每块板提供 `board_pins.c`（声明式表），校验逻辑是通用纯函数，host 可测。

### 3.4 CI 集成建议

新增 `.github/workflows/test.yml`：
```yaml
jobs:
  unit-tests:                      # 不装 ESP-IDF，最快反馈
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: cmake -S test -B build && cmake --build build
      - run: ./build/unit_tests     # Unity 运行器，失败即 CI 红
  firmware-builds:                 # 两块板交叉编译冒烟
    strategy:
      matrix: { board: [supermini-c3, kyle-s3-lcd] }
    steps:
      - uses: espressif/esp-idf-ci-action@v1
        with: { esp_idf_version: v5.5.2, target: ${{ matrix.target }} }
      - run: idf.py build
  board-config-check:              # 引脚冲突（可并入 unit-tests）
    run: ./build/unit_tests --group board
```
门禁：单元测试全绿 + 两块板编译通过才可合入；覆盖率用 gcov/lcov 对 `core/` 要求 ≥ 90%。

## 4. 迁移路径（从现有混乱代码分步改造）

### 阶段 0：基线冻结（1 天）
- 用现有 `sdkconfig` 各编译一遍 supermini-c3、kyle-s3-lcd，固件可运行作为回归基线。
- 提交前跑 `git status` 确认干净。

### 阶段 1：抽 HAL 接口（3~5 天）
- 新建 `hal/` 下 `IAudioCodec/IDisplay/ILed/IInput/IBacklight/IPower/INetwork/IBoard`。
- 把现有 `AudioCodec/Display/Led/Button/Backlight/...` 改成实现这些接口（或新增适配子类），**不改任何业务逻辑**，只加接口层。
- 验收：两块板行为不变，单测可以开始用 mock 编译 `core/` 之外新抽的纯函数。

### 阶段 2：收拢 boards（2~3 天）
- 删除 116 个 board 目录，只留 `supermini-c3/`、`kyle-s3-lcd/`、新增 `board_factory.cc`。
- 把 `main/CMakeLists.txt` 的 120 个 elseif 缩成 2 个 `if(CONFIG_BOARD_ID ...)`；新增 `config BOARD_ID` 或复用 `BOARD_TYPE_SUPERMINI_C3/KYLE_S3_LCD`。
- Kconfig.projbuild 清掉无关 `DISPLAY_LCD_TYPE` 等大盘 choice，改为 `BOARD_SCREEN_ST7789_240X240` 等板级宏随板联动。
- 验收：`idf.py menuconfig` 只有 2 块板可选，编译通过。

### 阶段 3：拆业务模块 + 纯逻辑下沉（1 周）
- 把 `application.cc` 里的 JSON 分发、会话切换逻辑抽到 `core/chat_session.cc`；把二进制协议编解码抽到 `core/wire_format.cc`；把 OTA 响应解析抽到 `core/ota_version.cc`。
- 同步搭 `test/`（Unity + CMock），给新抽的 core 代码补单测。
- `protocols/websocket_protocol.cc` 改为通过 `INetwork` 拿 socket，注入 mock 即可测。
- 验收：`core/` 覆盖率 ≥ 90%，两块板烧录后完成一次唤醒→对话→TTS 全流程。

### 阶段 4：清理与加固（2~3 天）
- 删 `application.cc` 中 kyle 专属硬编码（菜单在板内，不留在 Application）；`KyleBoard` 事件接口移入 `IBoard` 或板内部。
- 引入引脚冲突校验测试 + 编译期 static_assert。
- 接 CI（unit + 双板 build）。
- 文档：更新 `docs/` 为新架构分层说明。

### 验收标准
- `core/` 纯逻辑可在 CI 的 ubuntu 上独立跑 Unity 测试，无需 IDF。
- 两块板各生成一份固件，`supermini-c3`（C3/无 PSRAM/OLED）与 `kyle-s3-lcd`（S3/PSRAM/LCD+触摸）均完成：WiFi 配网 → OTA 激活 → 唤醒词 → WebSocket 对话 → TTS 播放 → 深睡唤醒。

## 5. 风险识别

| 风险 | 说明 | 应对 |
|---|---|---|
| 音频时序回归 | 重排任务优先级/队列后可能丢帧/卡顿 | 保留原 3 任务模型，只换接口；用音频调试器对比 |
| 协议不兼容 | 重构后 hello/二进制头/消息字段漂移 | `wire_format` 用黄金向量测试锁定原格式 |
| 触摸/菜单 UI 迁移成本高 | kyle-s3-lcd 的 KyleV1/V2 + MenuUI 高度耦合 LVGL | 先保留原 `display/menu` 代码作为驱动层实现，不重写 |
| 双板配置差异大（C3 无 PSRAM / S3 有） | 唤醒词/AFE 实现按芯片不同 | 用 `BOARD_HAS_PSRAM` 编译宏 + 编译期选择 wake_word 实现 |
| NVS 结构变更 | 旧设备升级后丢配置 | 保留原 NVS namespace（`websocket`/`mqtt`/`wifi`）不动 |
| 深睡/唤醒行为 | C3 ext0 与 S3 唤醒 GPIO 不同 | 板级配置提供 `sleep_wakeup_gpio` 与策略函数 |
| 硬件 mock 不充分 | 驱动代码无法覆盖 | 驱动层只做薄封装，逻辑上提到 core 测试 |

## 6. 模式推荐

- **编译期选择 + 运行时多态**：板子用 Kconfig 选一（`CONFIG_BOARD_ID`），`board_factory` 用 `#if` 分支返回不同 `IBoard`；业务永远拿到 `IBoard`。
- **依赖注入**：`Application` 构造函数接收 `IBoard&`，不用单例 `Board::GetInstance()`（去掉隐藏全局态，利于测试）。
- **接口最小化**：`IAudioCodec` 只含读写/音量/采样率；不含解码器（解码归 `core/audio_pipeline`）。
- **纯逻辑分离（Hexagonal）**：`core/` 不 include 任何 `esp_*.h`，所有 I/O 通过注入的接口——这是 host 单测的前提。
- **配置宏命名统一**：`CONFIG_BOARD_ID`、`BOARD_PIN_*`、`BOARD_HAS_*`、`BOARD_SCREEN_*`，避免原项目散落的 `DISPLAY_*`/`AUDIO_I2S_*` 命名混乱。

---


# 补充：自建后端设计（设备接入与语音服务，融入 admin-backend）

> 目标：设备端（supermini-c3 / kyle-s3-lcd）的连接后端由我们自己设计并编码，**直接融入现有 admin-backend**（Spring Boot 3.3.7 / Java 17 / MyBatis-Plus / Redis），作为后台新增的「IoT 设备」功能域，**不新增独立服务**。协议严格对齐设备端（WebSocket + Opus + MCP + OTA 激活），见《architecture-analysis.md》第 4 节与 `docs/websocket.md`。

## 7.1 功能清单

| # | 功能域 | 说明 |
|---|---|---|
| 1 | 设备接入与认证 | 设备注册/激活、token 签发与校验、WebSocket 握手鉴权（Authorization / Device-Id / Client-Id）、会话绑定、防重放 |
| 2 | OTA 与激活服务 | 版本检查接口、激活挑战-应答（设备端 efuse HMAC-SHA256 签名）、固件存储与下载、配置下发（websocket/mqtt/server_time）、强制升级 |
| 3 | 实时语音网关 | WebSocket 长连接管理（/ws/device）、session 生命周期、心跳/超时（设备 120s 无数据判超时）、Opus 二进制帧路由、协议 v1/v2/v3 编解码 |
| 4 | 语音流水线 | VAD 断句、ASR（STT）、TTS、可选服务器端 AEC（协议 v2 带时间戳）、流式输出与打断（barge-in） |
| 5 | LLM 编排 | 复用 spring-ai `LlmProviderStrategy`（可指向 claude-relay），多轮上下文、流式输出、情绪/表情映射（llm 消息）、中断控制 |
| 6 | MCP 网关 | 设备能力发现、工具注册、LLM→设备 tools/call 转发与结果回传（JSON-RPC 2.0） |
| 7 | 设备管理 | 设备列表/绑定/在线状态/远程控制（reboot、upgrade）/运行日志 |
| 8 | 管理后台 | admin-web 对接：设备、固件发布、配置模板、在线会话、统计 |
| 9 | 消息与任务 | 设备上下线事件、异步任务（OTA 发布、批量配置） |
| 10 | 可观测性 | 日志、指标（连接数/音频延迟/ASR/TTS 耗时） |

## 7.2 落位方案：融入 admin-backend（不新增服务）

**结论：功能作为 `com.ai.manager.system.iot` 功能域落在 admin-system**，与既有 PixelDog、番茄钟等并列，沿用 `controller → service/impl → mapper → domain(entity/dto/vo)` 分层，复用 `ApiResult`、全局异常、Redis、MyBatis-Plus、spring-ai 与 `client/` 外部 HTTP 模式。

- **为何融入而非独立 gateway 服务**：
  - 单一 Java 栈、单一 jar 部署；复用现有基建，零新增部署单元。
  - 设备量小（家庭/自用），Tomcat servlet 栈 + `spring-boot-starter-websocket` 足够扛几十路长连接。
  - 代价：实时音频/ASR 与后台 CRUD 同进程争 CPU；设备量增大时可再垂直切分出网关进程——只要 `protocol/`、会话状态机保持纯逻辑，抽离成本很低（这就是下文的取舍点）。
- **两个落位粒度（给选择 + 推荐）**：
  - **A（推荐）**：admin-system 内新增 `iot` 包 —— 改动最小，符合现有单 system 模块风格。
  - **B**：新增 Maven 模块 `admin-device`（依赖链 `admin-server → admin-system → admin-device`）—— 隔离更彻底，但要同步接入构建与覆盖率门禁，初期收益低。
- **与项目既有约定的关系**：本项目已有「实时推送用 SSE 替代 WebSocket」的约定（针对 admin-web 后台推送，见 `docs/笔记模块架构分析.md`）。设备通道 `/ws/device` 因 ESP32 协议强制要求 WebSocket，属例外，专门引入 `spring-boot-starter-websocket`；**后台 UI 推送仍继续用 SSE，不受影响**。

## 7.3 代码组织与新增依赖

### 新增依赖（admin-system/pom.xml）

- `org.springframework.boot:spring-boot-starter-websocket` —— 设备 WebSocket 通道（servlet 栈 Tomcat，已随 `spring-boot-starter-web` 就位）。
- `org.concentus:concentus` —— 纯 Java Opus 编解码（免 JNI），16k/24k 单声道。
- ASR/TTS：**不引重型 SDK**，走 HTTP client（沿用 `client/BaiduPanClient` 模式）对接云端或自建服务，换厂商只改配置。
- LLM：复用现有 `service/support/llm/LlmProviderStrategy`（spring-ai OpenAI 兼容，base-url 可指向 claude-relay）。

### 包结构（admin-system 内新增）

```text
com.ai.manager.system.iot
├─ config/        IotProperties(ai-manager.iot.*)、DeviceWebSocketConfig(WebSocketConfigurer)
├─ controller/    DeviceController、FirmwareController、DeviceOtaController(设备侧，免后台登录，走设备 token)
├─ websocket/     DeviceWsHandler(AbstractWebSocketHandler)、WsHandshakeInterceptor(token 校验)、WsSessionRegistry(Redis)
├─ protocol/      Hello/ServerHello/Listen/Abort/Stt/Tts/Llm/Mcp 消息 POJO + BinaryProtocol v1/v2/v3 编解码（纯逻辑，可单测）
├─ audio/         OpusCodec(Concentus 封装)、AudioPipeline(队列/VAD/帧调度)、Resampler(可选)
├─ mcp/           McpGateway、McpToolRegistry（tools/call 转发 + 结果回传）
├─ llm/           ChatOrchestrator（复用 LlmProviderStrategy，多轮上下文/流式/中断）
├─ service/       DeviceService、FirmwareService、OtaService、SessionService（+ impl）
├─ mapper/        IotDeviceMapper、IotFirmwareMapper、IotOtaRecordMapper、IotSessionMapper
├─ job/           OtaPublishJob、SessionTimeoutJob（可选）
└─ domain/
   ├─ entity/     IotDevice、IotFirmware、IotOtaRecord、IotSession
   ├─ dto/        OtaCheckRequest/Response、DeviceActivateRequest、McpForwardRequest ...
   └─ vo/         DeviceVO、FirmwareVO、OnlineSessionVO ...
```

## 7.4 对外协议（与设备端严格对齐）

### ① OTA 激活（HTTP，设备侧接口，免后台登录）

- `POST /api/iot/ota/check`：接收设备系统信息 JSON，返回配置下发：
  ```json
  {
    "activation": { },
    "websocket": { "url": "wss://host/ws/device", "token": "...", "version": 3 },
    "mqtt":      { "endpoint": "...", "client_id": "...", "username": "...", "password": "..." },
    "server_time": { "timestamp": 0, "timezone_offset": 0 },
    "firmware":  { "version": "2.2.1", "url": ".../api/iot/ota/download/12", "force": false }
  }
  ```
- `POST /api/iot/ota/activate`：校验设备 efuse HMAC-SHA256 签名，返回激活结果。
- `GET /api/iot/ota/download/{firmwareId}`：固件二进制下载（哈希校验）。
- 后台固件管理走 `/api/iot/firmware/*`（admin 鉴权）。

### ② WebSocket 实时通道（`/ws/device`）

- 握手校验 4 个头：`Authorization: Bearer <token>`、`Protocol-Version`、`Device-Id`（MAC）、`Client-Id`（UUID），由 `WsHandshakeInterceptor` 完成。
- `DeviceWsHandler`（`AbstractWebSocketHandler`）同时处理 text(JSON) 与 binary(Opus) 帧：
  - 设备消息：`hello` → 回 `server hello`（下发 `session_id`、`audio_params`）；`listen(start/stop/detect)`；`abort(reason)`；`mcp result`。
  - 下发消息：`stt(text)`、`llm(emotion,text)`、`tts(start/sentence_start/stop)` + Opus 音频、`mcp tools/call`、`system(reboot)`、`custom`。
- 二进制 v1 裸帧 / v2 时间戳头 / v3 简头（按握手 `version` 协商），编解码在 `protocol/` 纯逻辑层，可单测。
- 会话：`session_id` 贯穿；120s 无数据判超时；心跳用 WS Ping/Pong。

### ③ MCP（JSON-RPC 2.0，物联网控制唯一通道）

- 设备能力发现 `initialize` / `tools/list`。
- LLM 决定调用工具 → `ChatOrchestrator` → `McpGateway` 转发 `tools/call` 给设备 → 设备 `result` 回传 → 喂回 LLM。

## 7.5 核心流程设计

**① 激活/配网**
设备上电 → WiFi 配网 → POST 系统信息到 `CONFIG_OTA_URL`（指向本机 `/api/iot/ota/check`）→ 后端鉴权/注册 → 返回 `websocket.url` + `token` + 固件版本 → 设备存 NVS → 按需升级。

**② 一次语音会话（时序）**
```
设备                 admin-backend iot        ASR        LLM(LlmProviderStrategy)      TTS
 │ WS connect ───────►│
 │ hello ─────────────►│ 校验鉴权
 │◄──── server hello(session_id) │
 │ listen start ─────►│
 │ Opus 帧 ─► VAD ───►│──► ASR ──► stt(text) ──► LLM 流式
 │◄──── llm(emotion) ─│◄─────────────────────────────│
 │◄──── tts(start)+Opus◄── TTS 音频流 ◄────────────────│
 │ ……播放……
 │◄──── tts(stop) ────│
 │ listen start（自动续听）│
```

**③ 打断（barge-in）**
设备检测到新唤醒词 → 发 `abort(wake_word_detected)` 并提前发唤醒词 Opus → 后端停止 TTS 下发、ASR 重新进入监听。

**④ OTA 升级**
后台发布固件 → 设备下次激活检测到新版本 → 后端下发 `firmware.url` → 设备流式下载 → 校验 → 重启 → 激活上报新版本。

## 7.6 数据模型（新增 iot_ 表）

```sql
iot_device(id, uuid, client_id, mac, model, chip, firmware_version,
           ws_token, activated_at, last_seen_at, status, session_id, ota_state)
iot_firmware(id, version, file_path, file_hash, size, force, release_note, created_at)
iot_ota_record(id, device_id, firmware_id, state, progress, started_at, finished_at)
iot_session(id, device_id, session_id, started_at, ended_at, turn_count)
```

## 7.7 后端测试（对齐覆盖率门禁 ≥80%）

- **协议编解码单测**：v1/v2/v3 二进制头 + hello/tts/abort JSON + MCP JSON-RPC 组装 —— 纯逻辑，JUnit 直测，目标 ≥90%。
- **会话状态机单测**：idle→connecting→listening→speaking→idle 转移矩阵、非法转移拒绝、打断处理 —— 与固件 `core/chat_session` 用同一套黄金用例对拍。
- **WS 集成测试**：握手鉴权、hello 应答、音频帧回环、断线重连（真实 WS 客户端连内嵌 Tomcat）。
- **OTA 接口测试**：check/activate/token 签发（MockMvc standaloneSetup + GlobalExceptionHandler，按 admin-system 测试约定）。
- **MCP 转发测试**：tools/list 发现、tools/call 转发、超时/失败回传。
- 门禁：沿用 admin-system 对 `service.support` 纯逻辑类的 CLASS 级 80% 做法，`iot.protocol`/`iot.audio` 纯逻辑纳入 include。

## 7.8 配置与部署

- `application.yml` 新增 `ai-manager.iot:` 命名空间：ws url、token 过期、OTA 文件目录、ASR/TTS base-url、LLM provider（复用 spring-ai 配置）。
- 单 jar 部署 Pi 192.168.0.114（prod profile）；nginx 反代 `wss://` → admin-backend `/ws/device`；OTA 下载走接口/静态目录。
- 固件文件存本地卷/对象存储，哈希校验；Redis 存在线设备与 session 缓存。

## 7.9 admin-web 后台功能（新菜单「IoT 设备」）

- 设备管理：列表/详情/绑定/在线状态/远程 reboot。
- 固件管理：上传/发布/强制升级/OTA 记录。
- 在线会话：实时查看设备连接与会话轮次。
