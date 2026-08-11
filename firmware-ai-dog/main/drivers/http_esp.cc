#include "drivers/http_esp.h"

#include <utility>
#include <vector>

#include "esp_http_client.h"
#include "esp_log.h"

namespace kyle {

namespace {
constexpr const char* kTag = "HttpEsp";
}  // namespace

// pimpl：esp_http_client 句柄按 perform 时创建/销毁，不常驻。
// 事件回调收集响应体（user_data 传 Impl*），perform 后 response 完整落缓存。
struct HttpEsp::Impl {
    std::string method;
    std::string url;
    std::string body;
    std::vector<std::pair<std::string, std::string>> headers;
    bool opened = false;     // Open 已被调用（未 Open/URL 空时 ReadAll 直接返回空）
    bool performed = false;  // 已执行 perform（幂等，ReadAll 只真正发一次请求）
    int status = 0;
    std::string response;

    static esp_err_t HandleEvent(esp_http_client_event_t* evt) {
        if (evt->event_id == HTTP_EVENT_ON_DATA && evt->data && evt->data_len > 0) {
            Impl* self = static_cast<Impl*>(evt->user_data);
            self->response.append(static_cast<const char*>(evt->data), evt->data_len);
        }
        return ESP_OK;
    }
};

HttpEsp::HttpEsp() : impl_(std::make_unique<Impl>()) {}

HttpEsp::~HttpEsp() = default;

bool HttpEsp::Open(const std::string& method, const std::string& url) {
    impl_->method = method;
    impl_->url = url;
    impl_->opened = true;
    return true;
}

int HttpEsp::status_code() const { return impl_->status; }

std::string HttpEsp::ReadAll() {
    Impl& i = *impl_;
    if (!i.opened || i.url.empty()) {
        return "";
    }
    if (!i.performed) {
        i.performed = true;
        esp_http_client_config_t cfg = {};
        cfg.url = i.url.c_str();
        cfg.event_handler = &Impl::HandleEvent;
        cfg.user_data = impl_.get();
        cfg.timeout_ms = 10000;
        cfg.method = (i.method == "GET") ? HTTP_METHOD_GET : HTTP_METHOD_POST;

        esp_http_client_handle_t client = esp_http_client_init(&cfg);
        if (!client) {
            ESP_LOGE(kTag, "esp_http_client_init failed");
            return "";
        }
        for (const auto& h : i.headers) {
            esp_http_client_set_header(client, h.first.c_str(), h.second.c_str());
        }
        if (!i.body.empty()) {
            esp_http_client_set_post_field(client, i.body.c_str(), i.body.size());
        }

        esp_err_t err = esp_http_client_perform(client);
        i.status = esp_http_client_get_status_code(client);
        if (err != ESP_OK) {
            ESP_LOGW(kTag, "%s %s failed: %s", i.method.c_str(), i.url.c_str(),
                     esp_err_to_name(err));
        } else {
            // 成功路径也要留痕：OTA/上报链路的唯一可观测点
            ESP_LOGI(kTag, "%s %s -> %d (%zuB)", i.method.c_str(), i.url.c_str(), i.status,
                     i.response.size());
        }
        esp_http_client_cleanup(client);
    }
    return i.response;
}

void HttpEsp::SetContent(const std::string& body) { impl_->body = body; }

void HttpEsp::SetHeader(const std::string& name, const std::string& value) {
    impl_->headers.emplace_back(name, value);
}

}  // namespace kyle
