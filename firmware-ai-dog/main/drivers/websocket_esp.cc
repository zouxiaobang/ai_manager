#include "drivers/websocket_esp.h"

#include <utility>
#include <vector>

#include "esp_log.h"
#include "esp_websocket_client.h"

namespace kyle {

namespace {
// WebSocket 帧 op_code（RFC 6455）
constexpr int kOpText = 0x1;
constexpr int kOpBinary = 0x2;
constexpr const char* kTag = "WebSocketEsp";
}  // namespace

// pimpl：esp_websocket_client 句柄、事件回调与待握手 header 都藏在 Impl。
// 事件回调从 WS 客户端 task 上下文触发（arg 即 Impl*，由 register_events 传入）。
struct WebSocketEsp::Impl {
    esp_websocket_client_handle_t client = nullptr;
    bool connected = false;
    std::vector<std::pair<std::string, std::string>> pending_headers;  // Connect 前收集
    std::function<void(const std::string&)> on_text;
    std::function<void(const uint8_t*, size_t)> on_binary;
    std::function<void()> on_disconnected;
    std::function<void()> on_connected;

    static void HandleEvent(void* arg, esp_event_base_t base, int32_t id, void* data) {
        Impl* self = static_cast<Impl*>(arg);
        switch (id) {
            case WEBSOCKET_EVENT_CONNECTED:
                self->connected = true;
                ESP_LOGI(kTag, "WS 已连接（握手成功）");
                if (self->on_connected) {
                    self->on_connected();
                }
                break;
            case WEBSOCKET_EVENT_DISCONNECTED: {
                // 仅通知曾经连上后的掉线（握手失败等首次断开不打扰上层）
                bool was_connected = self->connected;
                self->connected = false;
                ESP_LOGI(kTag, "WS 断开（此前已连接=%d）", was_connected ? 1 : 0);
                if (was_connected && self->on_disconnected) {
                    self->on_disconnected();
                }
                break;
            }
            case WEBSOCKET_EVENT_DATA: {
                esp_websocket_event_data_t* evt =
                    static_cast<esp_websocket_event_data_t*>(data);
                if (evt->data_ptr == nullptr || evt->data_len == 0) {
                    break;
                }
                if (evt->op_code == kOpText && self->on_text) {
                    self->on_text(
                        std::string(static_cast<const char*>(evt->data_ptr), evt->data_len));
                } else if (evt->op_code == kOpBinary && self->on_binary) {
                    self->on_binary(reinterpret_cast<const uint8_t*>(evt->data_ptr),
                                    evt->data_len);
                }
                break;
            }
            default:
                break;
        }
        (void)base;
    }
};

WebSocketEsp::WebSocketEsp() : impl_(std::make_unique<Impl>()) {}

WebSocketEsp::~WebSocketEsp() { Close(); }

bool WebSocketEsp::Connect(const std::string& url) {
    Impl& i = *impl_;
    if (i.client) {
        esp_websocket_client_destroy(i.client);
        i.client = nullptr;
    }
    i.connected = false;

    esp_websocket_client_config_t cfg = {};
    cfg.uri = url.c_str();
    cfg.reconnect_timeout_ms = 3000;
    cfg.network_timeout_ms = 5000;
    cfg.disable_auto_reconnect = false;  // 客户端内置自动重连

    i.client = esp_websocket_client_init(&cfg);
    if (!i.client) {
        return false;
    }
    // 握手 header 必须在 start 前设置：append_header 写入 config，首次握手即带上
    for (const auto& h : i.pending_headers) {
        esp_websocket_client_append_header(i.client, h.first.c_str(), h.second.c_str());
    }
    esp_websocket_register_events(i.client, WEBSOCKET_EVENT_ANY, &Impl::HandleEvent,
                                  impl_.get());
    esp_err_t err = esp_websocket_client_start(i.client);
    if (err != ESP_OK) {
        esp_websocket_client_destroy(i.client);
        i.client = nullptr;
        return false;
    }
    return true;
}

void WebSocketEsp::Close() {
    Impl& i = *impl_;
    if (i.client) {
        esp_websocket_client_close(i.client, pdMS_TO_TICKS(1000));
        esp_websocket_client_destroy(i.client);
        i.client = nullptr;
    }
    i.connected = false;
}

bool WebSocketEsp::IsConnected() const { return impl_->connected; }

bool WebSocketEsp::SendText(const std::string& text) {
    if (impl_->client == nullptr) {
        ESP_LOGW(kTag, "send_text: 客户端未创建");
        return false;
    }
    // 返回发送字节数，>=0 表示成功（未连接/失败返回 -1）
    const int n = esp_websocket_client_send_text(impl_->client, text.c_str(), text.size(),
                                                 pdMS_TO_TICKS(1000));
    if (n < 0) {
        ESP_LOGW(kTag, "send_text 失败: %d", n);
    }
    return n >= 0;
}

bool WebSocketEsp::SendBinary(const uint8_t* data, size_t len) {
    if (impl_->client == nullptr) {
        return false;
    }
    return esp_websocket_client_send_bin(impl_->client, reinterpret_cast<const char*>(data),
                                         static_cast<int>(len), pdMS_TO_TICKS(1000)) >= 0;
}

void WebSocketEsp::SetHeader(const std::string& name, const std::string& value) {
    Impl& i = *impl_;
    i.pending_headers.emplace_back(name, value);
    // 已启动的客户端：追加到现有 config，下一次重连握手生效
    if (i.client) {
        esp_websocket_client_append_header(i.client, name.c_str(), value.c_str());
    }
}

void WebSocketEsp::OnText(std::function<void(const std::string&)> cb) {
    impl_->on_text = std::move(cb);
}

void WebSocketEsp::OnBinary(std::function<void(const uint8_t*, size_t)> cb) {
    impl_->on_binary = std::move(cb);
}

void WebSocketEsp::OnDisconnected(std::function<void()> cb) {
    impl_->on_disconnected = std::move(cb);
}

void WebSocketEsp::OnConnected(std::function<void()> cb) {
    impl_->on_connected = std::move(cb);
}

}  // namespace kyle
