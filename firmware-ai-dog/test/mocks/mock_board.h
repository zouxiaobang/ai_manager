#ifndef KYLE_TEST_MOCKS_MOCK_BOARD_H
#define KYLE_TEST_MOCKS_MOCK_BOARD_H

#include <functional>
#include <memory>
#include <string>
#include <utility>
#include <vector>

#include "hal/board.h"
#include "hal/provisioning.h"

// 内存版板级依赖：记录 audio/led/display/power 调用，供 application host 单测。
// 各 mock 的接口实现只做记录，不涉及真实硬件。mock 设备经能力接口继承 IDevice，
// 与真实驱动一致可注册进 MockBoard 的设备列表（EnterSleep 遍历 Stop() 驱动它们）。

class MockAudio : public kyle::IAudioCodec {
public:
    int last_volume = -1;
    int volume_set_count = 0;
    int stop_calls = 0;
    bool Start() override { return true; }
    void Stop() override { stop_calls++; }
    size_t Read(int16_t* dst, size_t s) override { (void)dst; (void)s; return 0; }
    size_t Write(const int16_t* src, size_t s) override { (void)src; (void)s; return 0; }
    void SetOutputVolume(int v) override {
        last_volume = v;
        volume_set_count++;
    }
    int input_sample_rate() const override { return 16000; }
    int output_sample_rate() const override { return 24000; }
};

class MockLed : public kyle::ILed {
public:
    kyle::LedState last_state = kyle::LedState::kLedIdle;
    int set_count = 0;
    void SetState(kyle::LedState s) override {
        last_state = s;
        set_count++;
    }
    void Stop() override { SetState(kyle::LedState::kLedIdle); }
};

class MockDisplay : public kyle::IDisplay {
public:
    std::string last_toast;
    int display_sleep_calls = 0;
    void SetStatus(const char* s) override { (void)s; }
    void SetChatMessage(const char* r, const char* t) override { (void)r; (void)t; }
    void SetEmotion(const char* e) override { (void)e; }
    void ShowToast(const char* msg, int ms) override {
        (void)ms;
        last_toast = msg ? msg : "";
    }
    int width() const override { return 240; }
    int height() const override { return 240; }
    void DisplaySleep() override { display_sleep_calls++; }
    void Stop() override { DisplaySleep(); }
};

class MockPower : public kyle::IPower {
public:
    int deep_sleep_calls = 0;
    int reboot_calls = 0;
    int battery_percent() const override { return -1; }
    bool is_charging() const override { return false; }
    void DeepSleep() override { deep_sleep_calls++; }
    void Reboot() override { reboot_calls++; }
    void Stop() override { DeepSleep(); }
};

class MockBacklight : public kyle::IBacklight {
public:
    int last_brightness = -1;
    void SetBrightness(int v) override { last_brightness = v; }
    int brightness() const override { return last_brightness; }
    void Stop() override { SetBrightness(0); }
};

// K5.5 网络观测：HTTP/WS 调用记录 + 可注入响应，供 application 网络链路单测断言。
// 使用 shared_ptr<Log> 的原因：mock 对象会被 move 进 Application 私有持有，
// 测试端与 mock 共享同一份日志对象，mock 销毁后仍可读取调用记录。

struct HttpLog {
    std::string method;
    std::string url;
    std::string body;
    std::vector<std::pair<std::string, std::string>> headers;
    int status = 200;
    std::string response;
};

class MockHttp : public kyle::IHttp {
public:
    explicit MockHttp(std::shared_ptr<HttpLog> log) : log_(std::move(log)) {}
    bool Open(const std::string& method, const std::string& url) override {
        log_->method = method;
        log_->url = url;
        return true;
    }
    int status_code() const override { return log_->status; }
    std::string ReadAll() override { return log_->response; }
    void SetContent(const std::string& body) override { log_->body = body; }
    void SetHeader(const std::string& name, const std::string& value) override {
        log_->headers.emplace_back(name, value);
    }

private:
    std::shared_ptr<HttpLog> log_;
};

struct WsLog {
    std::string url;
    std::vector<std::pair<std::string, std::string>> headers;
    std::vector<std::string> sent_texts;
    bool connect_result = true;
};

class MockWebSocket : public kyle::IWebSocket {
public:
    explicit MockWebSocket(std::shared_ptr<WsLog> log) : log_(std::move(log)) {}
    bool Connect(const std::string& url) override {
        log_->url = url;
        return log_->connect_result;
    }
    void Close() override {}
    bool IsConnected() const override { return false; }
    bool SendText(const std::string& text) override {
        log_->sent_texts.push_back(text);
        return true;
    }
    bool SendBinary(const uint8_t*, size_t) override { return true; }
    void SetHeader(const std::string& name, const std::string& value) override {
        log_->headers.emplace_back(name, value);
    }
    void OnText(std::function<void(const std::string&)> cb) override { on_text = std::move(cb); }
    void OnBinary(std::function<void(const uint8_t*, size_t)>) override {}
    void OnDisconnected(std::function<void()> cb) override { on_disconnected = std::move(cb); }
    void OnConnected(std::function<void()> cb) override { on_connected = std::move(cb); }

    // 测试触发端：模拟 WS 客户端 task 事件（真实 WebSocketEsp 在 CONNECTED/DATA 事件里触发）
    std::function<void()> on_connected;
    std::function<void(const std::string&)> on_text;
    std::function<void()> on_disconnected;
    void EmitConnected() {
        if (on_connected) on_connected();
    }
    void EmitText(const std::string& t) {
        if (on_text) on_text(t);
    }
    void EmitDisconnected() {
        if (on_disconnected) on_disconnected();
    }

private:
    std::shared_ptr<WsLog> log_;
};

class MockNetwork : public kyle::INetwork {
public:
    std::string last_ssid;
    std::string last_password;
    int connect_calls = 0;
    int disconnect_calls = 0;
    kyle::WifiState state = kyle::WifiState::kDisconnected;
    std::function<void(kyle::WifiState)> cb;
    std::string mac = "aa:bb:cc:dd:ee:ff";  // 默认 MAC，测试可覆盖
    // K5.5：共享观测日志（CreateHttp/CreateWebSocket 时惰性补齐；测试可先注入设响应）
    std::shared_ptr<HttpLog> http_log;
    std::shared_ptr<WsLog> ws_log;
    MockWebSocket* last_ws = nullptr;  // CreateWebSocket 记录（对象 move 进 app，地址稳定）

    std::unique_ptr<kyle::IWebSocket> CreateWebSocket(int id) override {
        (void)id;
        if (!ws_log) ws_log = std::make_shared<WsLog>();
        auto mock = std::make_unique<MockWebSocket>(ws_log);
        last_ws = mock.get();
        return mock;
    }
    std::unique_ptr<kyle::IHttp> CreateHttp(int id) override {
        (void)id;
        if (!http_log) http_log = std::make_shared<HttpLog>();
        return std::make_unique<MockHttp>(http_log);
    }
    std::string mac_address() const override { return mac; }
    void ConnectWifi(const std::string& ssid, const std::string& password) override {
        last_ssid = ssid;
        last_password = password;
        connect_calls++;
        // 对齐真实 NetworkEsp：无凭据时驱动就绪但保持未连接（等待配网）；有凭据才进入连接中
        state = ssid.empty() ? kyle::WifiState::kDisconnected : kyle::WifiState::kConnecting;
    }
    void DisconnectWifi() override {
        disconnect_calls++;
        state = kyle::WifiState::kDisconnected;
    }
    kyle::WifiState wifi_state() const override { return state; }
    void OnWifiState(std::function<void(kyle::WifiState)> c) override { cb = std::move(c); }
    // ---- K5.6 SoftAP ----
    int start_ap_calls = 0;
    int stop_ap_calls = 0;
    std::string last_ap_ssid;
    bool start_ap_result = true;
    bool StartAp(const std::string& ssid) override {
        start_ap_calls++;
        last_ap_ssid = ssid;
        return start_ap_result;
    }
    void StopAp() override { stop_ap_calls++; }
    std::string ap_ip() const override { return "192.168.4.1"; }
};

// K5.6 配网服务 mock：记录 start/stop 与 ap_ssid；测试端可 EmitSaved 模拟 httpd 提交表单
class MockProvisioningServer : public kyle::IProvisioningServer {
public:
    int start_calls = 0;
    int stop_calls = 0;
    std::string last_ap_ssid;
    bool start_result = true;
    std::function<void(const kyle::ProvisionResult&)> on_saved;

    bool Start(const std::string& ap_ssid,
               std::function<void(const kyle::ProvisionResult&)> cb) override {
        start_calls++;
        last_ap_ssid = ap_ssid;
        on_saved = std::move(cb);
        return start_result;
    }
    void Stop() override { stop_calls++; }
    // 测试触发端：模拟 httpd task 收到表单后回调 on_saved（真实实现经 Schedule 回主循环）
    void EmitSaved(const kyle::ProvisionResult& r) {
        if (on_saved) {
            on_saved(r);
        }
    }
};

// 可手动触发的输入源：Application 注册回调后由测试 Emit 事件
class MockInput : public kyle::IInput {
public:
    std::function<void(const kyle::InputEvent&)> cb;
    void OnEvent(std::function<void(const kyle::InputEvent&)> c) override {
        cb = std::move(c);
    }
    void Emit(const kyle::InputEvent& ev) {
        if (cb) cb(ev);
    }
};

class MockBoard : public kyle::IBoard {
public:
    // 成员名带 _mock 后缀：避免与同名接口方法（audio()/led()/...）冲突
    MockAudio audio_mock;
    MockLed led_mock;
    MockDisplay display_mock;
    MockBacklight backlight_mock;
    MockPower power_mock;
    MockNetwork network_mock;
    MockProvisioningServer provisioning_mock;
    kyle::IInput* input_source = nullptr;

    const kyle::BoardInfo& info() const override {
        static const kyle::BoardInfo kInfo = {
            .name = "mock",
            .target = "host",
            .flash_size_mb = 0,
            .has_psram = false,
            .has_display = true,
            .has_touch = false,
            .has_battery = false,
            .has_backlight = false,
            .default_input_rate = 16000,
            .default_output_rate = 24000,
        };
        return kInfo;
    }
    kyle::IAudioCodec* audio() override { return &audio_mock; }
    kyle::IDisplay* display() override { return &display_mock; }
    kyle::ILed* led() override { return &led_mock; }
    kyle::IInput* input() override { return input_source; }
    kyle::IBacklight* backlight() override { return &backlight_mock; }
    kyle::IPower* power() override { return &power_mock; }
    kyle::INetwork* network() override { return &network_mock; }
    kyle::IProvisioningServer* provisioning() override { return &provisioning_mock; }
    void Init() override {}
    void RegisterDevice(kyle::IDevice* d) override {
        if (d != nullptr) {
            devices_.push_back(d);
        }
    }
    // 与真实板一致：遍历设备列表调 Stop()，驱动各 mock 记录副作用
    void EnterSleep() override {
        enter_sleep_calls++;
        for (kyle::IDevice* d : devices_) {
            if (d != nullptr) {
                d->Stop();
            }
        }
    }
    int enter_sleep_calls = 0;
    std::vector<kyle::IDevice*> devices_;
};

#endif  // KYLE_TEST_MOCKS_MOCK_BOARD_H
