// Application 装配测试：输入事件 → 会话状态机 + LED 联动 + 音量 + 深睡；
// K5.5 起含网络引导（WiFi → OTA check → WS 建连 → hello 握手）。
// 纯 host 测试，IBoard/IAudioCodec/ILed/IPower/INetwork 全部用 mock_board.h 的内存实现。

#include <cstring>
#include <memory>

#include "app/application.h"
#include "core/chat_session.h"
#include "hal/input.h"
#include "mocks/mock_board.h"
#include "mocks/mock_storage.h"
#include "unity.h"

using namespace kyle;

namespace {

// 局部构造并初始化一个 Application，返回其输入源以便触发事件
struct Harness {
    MockBoard board;
    MockInput input;
    MockStorage storage;
    Application app;
    Harness() : app(board, storage) {
        board.input_source = &input;
        // 复刻板级注册：按关断顺序注册（灭灯→停音频→背光→屏→电源深睡）
        board.RegisterDevice(&board.led_mock);
        board.RegisterDevice(&board.audio_mock);
        board.RegisterDevice(&board.backlight_mock);
        board.RegisterDevice(&board.display_mock);
        board.RegisterDevice(&board.power_mock);
        app.Initialize();
    }
    void Emit(const InputEvent& ev) { input.Emit(ev); }
};

}  // namespace

TEST_CASE("Init leaves session idle and LED idle", "[application]") {
    Harness h;
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("Boot click starts session and drives LED connecting", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedConnecting),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("Touch click also toggles session", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kTouchButtonId});
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
}

TEST_CASE("Click while active stops session and LED returns idle", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});   // 开始
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
    h.Emit(InputEvent{InputEvent::kClick, kButtonBoot});   // 停止
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
}

TEST_CASE("UP click raises volume and shows toast", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});
    TEST_ASSERT_EQUAL_INT(80, h.board.audio_mock.last_volume);       // 70 + 10
    TEST_ASSERT_EQUAL_STRING("音量 80%", h.board.display_mock.last_toast.c_str());
}

TEST_CASE("DOWN click lowers volume", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolDown});
    TEST_ASSERT_EQUAL_INT(60, h.board.audio_mock.last_volume);       // 70 - 10
}

TEST_CASE("Volume clamps at 0 and 100", "[application]") {
    Harness h;
    for (int i = 0; i < 10; ++i) {
        h.Emit(InputEvent{InputEvent::kClick, kButtonVolDown});  // 70 → 0
    }
    TEST_ASSERT_EQUAL_INT(0, h.board.audio_mock.last_volume);
    for (int i = 0; i < 15; ++i) {
        h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});    // 0 → 100
    }
    TEST_ASSERT_EQUAL_INT(100, h.board.audio_mock.last_volume);
    h.Emit(InputEvent{InputEvent::kClick, kButtonVolUp});        // 100 不再涨
    TEST_ASSERT_EQUAL_INT(100, h.board.audio_mock.last_volume);
}

TEST_CASE("Boot long press shuts down peripherals then deep sleeps", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kButtonBoot});
    // 应用层只触发板级 EnterSleep()，关断序列委托给板；副作用由 mock 板复刻并逐项断言
    TEST_ASSERT_EQUAL_INT(1, h.board.enter_sleep_calls);
    TEST_ASSERT_EQUAL_INT(1, h.board.power_mock.deep_sleep_calls);
    TEST_ASSERT_EQUAL_INT(static_cast<int>(kyle::LedState::kLedIdle),
                          static_cast<int>(h.board.led_mock.last_state));
    TEST_ASSERT_EQUAL_INT(1, h.board.audio_mock.stop_calls);
    TEST_ASSERT_EQUAL_INT(0, h.board.backlight_mock.last_brightness);
    TEST_ASSERT_EQUAL_INT(1, h.board.display_mock.display_sleep_calls);
}

TEST_CASE("Touch long press does not deep sleep", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kTouchButtonId});
    TEST_ASSERT_EQUAL_INT(0, h.board.power_mock.deep_sleep_calls);
}

TEST_CASE("Unmapped double click is ignored", "[application]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kDoubleClick, kButtonBoot});
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());       // 状态不变
    TEST_ASSERT_EQUAL_INT(0, h.board.power_mock.deep_sleep_calls);
}

TEST_CASE("Initialize injects stored wifi credentials into network", "[application][wifi]") {
    MockBoard board;
    MockStorage storage;
    storage.SetString("wifi", "ssid", "home_wifi");
    storage.SetString("wifi", "password", "pw123");
    Application app(board, storage);
    app.Initialize();
    // 网络层收到配网凭据并进入连接中
    TEST_ASSERT_EQUAL_INT(1, board.network_mock.connect_calls);
    TEST_ASSERT_EQUAL_STRING("home_wifi", board.network_mock.last_ssid.c_str());
    TEST_ASSERT_EQUAL_STRING("pw123", board.network_mock.last_password.c_str());
}

TEST_CASE("Initialize uses Kconfig fallback when NVS has no wifi config", "[application][wifi]") {
    MockBoard board;
    MockStorage storage;  // NVS 无 wifi 配置 → 走 CONFIG_WIFI_SSID 兜底
    Application app(board, storage);
    app.Initialize();
    TEST_ASSERT_EQUAL_INT(1, board.network_mock.connect_calls);
    TEST_ASSERT_EQUAL_STRING(CONFIG_WIFI_SSID, board.network_mock.last_ssid.c_str());
}

TEST_CASE("Empty wifi credentials keep driver armed but disconnected", "[application][wifi]") {
    // 对齐老代码 wifi_manager_init：boot 无条件调用 ConnectWifi 拉起驱动；
    // 空凭据（NVS 与 CONFIG_WIFI_SSID 均空）时驱动就绪但保持未连接、等待配网。
    MockBoard board;
    board.network_mock.ConnectWifi("", "");
    TEST_ASSERT_EQUAL_INT(1, board.network_mock.connect_calls);
    TEST_ASSERT_EQUAL_INT(static_cast<int>(kyle::WifiState::kDisconnected),
                          static_cast<int>(board.network_mock.wifi_state()));
}

// ---- K5.6 SoftAP 配网 ----

TEST_CASE("Long-press DOWN enters provisioning mode", "[application][provisioning]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kButtonVolDown});
    // 热点开启：SSID 由 mock MAC 后 4 位派生 kyle-eeff
    TEST_ASSERT_EQUAL_INT(1, h.board.network_mock.start_ap_calls);
    TEST_ASSERT_EQUAL_STRING("kyle-eeff", h.board.network_mock.last_ap_ssid.c_str());
    // 配网服务启动（同一 SSID）
    TEST_ASSERT_EQUAL_INT(1, h.board.provisioning_mock.start_calls);
    TEST_ASSERT_EQUAL_STRING("kyle-eeff", h.board.provisioning_mock.last_ap_ssid.c_str());
    // 屏幕提示配网热点名
    TEST_ASSERT_NOT_NULL(strstr(h.board.display_mock.last_toast.c_str(), "kyle-eeff"));
}

TEST_CASE("Provision form save writes wifi credentials to storage", "[application][provisioning]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kButtonVolDown});  // 进入配网并注册 on_saved
    // 模拟 httpd 收到有效表单：ok + 凭据
    kyle::ProvisionResult r;
    r.ok = true;
    r.ssid = "home_wifi";
    r.password = "pw123";
    h.board.provisioning_mock.EmitSaved(r);
    h.app.RunOnce();  // 派发 Schedule → OnProvisionSaved → 写 NVS
    TEST_ASSERT_EQUAL_STRING("home_wifi", h.storage.GetString("wifi", "ssid").c_str());
    TEST_ASSERT_EQUAL_STRING("pw123", h.storage.GetString("wifi", "password").c_str());
}

TEST_CASE("Provision save with invalid form does not write storage", "[application][provisioning]") {
    Harness h;
    h.Emit(InputEvent{InputEvent::kLongPress, kButtonVolDown});
    kyle::ProvisionResult r;  // ok=false（缺 ssid）
    h.board.provisioning_mock.EmitSaved(r);
    h.app.RunOnce();
    TEST_ASSERT_TRUE(h.storage.GetString("wifi", "ssid").empty());
    TEST_ASSERT_TRUE(h.storage.GetString("wifi", "password").empty());
}

// ---- K5.5 网络引导 ----

TEST_CASE("WiFi connected triggers OTA check then websocket connect", "[application][network]") {
    Harness h;
    // 预置 OTA 响应：下发 WS 配置、无新固件
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 200;
    h.board.network_mock.http_log->response =
        R"({"websocket":{"url":"ws://192.168.0.114:8080/ws/device","token":"tok-1","version":3}})";

    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();  // 派发 → OnWifiStateChanged → RunOtaCheck

    // OTA check 请求：URL = 默认 OTA base + /check；Content-Type json；body 携带归一化 MAC/版本
    TEST_ASSERT_EQUAL_STRING("POST", h.board.network_mock.http_log->method.c_str());
    TEST_ASSERT_EQUAL_STRING("https://api.tenclass.net/kyle/ota/check",
                             h.board.network_mock.http_log->url.c_str());
    bool has_json_ct = false;
    for (const auto& kv : h.board.network_mock.http_log->headers) {
        if (kv.first == "Content-Type" && kv.second == "application/json") has_json_ct = true;
    }
    TEST_ASSERT_TRUE(has_json_ct);
    TEST_ASSERT_NOT_NULL(strstr(h.board.network_mock.http_log->body.c_str(),
                                "\"mac\":\"aabbccddeeff\""));
    TEST_ASSERT_NOT_NULL(strstr(h.board.network_mock.http_log->body.c_str(),
                                "\"firmware_version\":\"2.2.2\""));
    TEST_ASSERT_NOT_NULL(strstr(h.board.network_mock.http_log->body.c_str(),
                                "\"language\":\"zh-CN\""));

    // 服务端配置已写回存储（ApplyOtaResponse → websocket.url/token/version）
    TEST_ASSERT_EQUAL_STRING("ws://192.168.0.114:8080/ws/device",
                             h.app.net_config().websocket().url.c_str());
    TEST_ASSERT_EQUAL_STRING("tok-1", h.app.net_config().websocket().token.c_str());
    TEST_ASSERT_EQUAL_INT(3, h.app.net_config().websocket().version);

    // WS 已建连：URL + 4 个鉴权 header（Device-Id 归一化小写去冒号）
    TEST_ASSERT_NOT_NULL(h.board.network_mock.ws_log.get());
    TEST_ASSERT_EQUAL_STRING("ws://192.168.0.114:8080/ws/device",
                             h.board.network_mock.ws_log->url.c_str());
    TEST_ASSERT_EQUAL_INT(4, static_cast<int>(h.board.network_mock.ws_log->headers.size()));
    bool has_device_id = false;
    for (const auto& kv : h.board.network_mock.ws_log->headers) {
        if (kv.first == "Device-Id" && kv.second == "aabbccddeeff") has_device_id = true;
    }
    TEST_ASSERT_TRUE(has_device_id);

    // 建连即进入 connecting（server hello 前）
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());
}

TEST_CASE("Server hello after ws connected opens channel to listening", "[application][network]") {
    Harness h;
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 200;
    h.board.network_mock.http_log->response =
        R"({"websocket":{"url":"ws://host/ws/device","token":"tok","version":3}})";
    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();

    auto* ws = h.board.network_mock.last_ws;
    TEST_ASSERT_NOT_NULL(ws);

    // 通道建立 → 应用发 hello
    ws->EmitConnected();
    h.app.RunOnce();
    TEST_ASSERT_EQUAL_INT(1, static_cast<int>(h.board.network_mock.ws_log->sent_texts.size()));
    TEST_ASSERT_NOT_NULL(
        strstr(h.board.network_mock.ws_log->sent_texts[0].c_str(), "\"type\":\"hello\""));
    TEST_ASSERT_NOT_NULL(
        strstr(h.board.network_mock.ws_log->sent_texts[0].c_str(), "\"device_id\":\"aabbccddeeff\""));

    // 服务端 server hello → 通道就绪（connecting → listening）
    ws->EmitText(R"({"type":"hello","session_id":"sess-1","transport":"websocket",
                     "audio_params":{"sample_rate":16000,"frame_duration":16}})");
    h.app.RunOnce();
    TEST_ASSERT_EQUAL_INT(kListening, h.app.session().state());
}

TEST_CASE("Newer firmware response triggers OTA plan and skips ws connect", "[application][network]") {
    Harness h;
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 200;
    h.board.network_mock.http_log->response =
        R"({"firmware":{"version":"3.0.0","url":"https://example.com/kyle/fw.bin"}})";
    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();

    // host 下升级分支只做决策（不真正下载/重启）：不建 WS、会话不动
    TEST_ASSERT_NULL(h.board.network_mock.ws_log.get());
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
}

TEST_CASE("OTA check non-200 skips ws bootstrap", "[application][network]") {
    Harness h;
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 500;  // 后端不可达
    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();
    TEST_ASSERT_NULL(h.board.network_mock.ws_log.get());
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
}

TEST_CASE("Websocket connect failure resets session to idle", "[application][network]") {
    Harness h;
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 200;
    h.board.network_mock.http_log->response =
        R"({"websocket":{"url":"ws://host/ws/device","token":"tok","version":3}})";
    h.board.network_mock.ws_log = std::make_shared<WsLog>();
    h.board.network_mock.ws_log->connect_result = false;
    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
}

TEST_CASE("Ws disconnect during session resets to idle", "[application][network]") {
    Harness h;
    h.board.network_mock.http_log = std::make_shared<HttpLog>();
    h.board.network_mock.http_log->status = 200;
    h.board.network_mock.http_log->response =
        R"({"websocket":{"url":"ws://host/ws/device","token":"tok","version":3}})";
    h.board.network_mock.cb(kyle::WifiState::kConnected);
    h.app.RunOnce();
    TEST_ASSERT_EQUAL_INT(kConnecting, h.app.session().state());

    h.board.network_mock.last_ws->EmitDisconnected();
    h.app.RunOnce();
    TEST_ASSERT_EQUAL_INT(kIdle, h.app.session().state());
}
