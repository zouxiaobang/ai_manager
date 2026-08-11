#include "drivers/provisioning_esp.h"

#include <cstring>

#include "esp_http_server.h"
#include "esp_log.h"

#include "core/provisioning.h"

namespace kyle {

namespace {
constexpr const char* kTag = "Provisioning";
constexpr size_t kMaxFormBody = 512;  // ssid+password 表单上限（超出截断）

// 配网页响应（error 非空时表单上方显示错误提示）
void SendPage(httpd_req_t* req, const std::string& ap_ssid, const std::string& error) {
    httpd_resp_set_type(req, "text/html; charset=utf-8");
    std::string page = BuildProvisionPage(ap_ssid, error);
    httpd_resp_send(req, page.c_str(), static_cast<int>(page.size()));
}
}  // namespace

// pimpl 完整定义（与头文件 kyle::ProvisioningEsp 内前向声明的 Impl 对应，必须是非匿名命名空间成员）。
// httpd handler 做成 Impl 静态成员：可直接访问私有嵌套类型，且能作为普通函数指针注册给 httpd。
struct ProvisioningEsp::Impl {
    httpd_handle_t server = nullptr;
    std::string ap_ssid;
    std::function<void(const ProvisionResult&)> on_saved;

    // GET / → 配网页。user_ctx 在 httpd_register_uri_handler 注册时传 Impl*（impl_.get()）。
    static esp_err_t HandleRoot(httpd_req_t* req) {
        auto* self = static_cast<Impl*>(req->user_ctx);
        SendPage(req, self->ap_ssid, "");
        return ESP_OK;
    }

    // POST /save → 解析表单。校验通过回调 on_saved（Application 写 NVS + 计划重启）。
    static esp_err_t HandleSave(httpd_req_t* req) {
        auto* self = static_cast<Impl*>(req->user_ctx);
        // body 可能分多次到达，httpd_req_recv 每次返回已读字节数；读到 0/负 表示结束
        std::string body;
        body.reserve(kMaxFormBody);
        char buf[128];
        int received;
        while ((received = httpd_req_recv(req, buf, sizeof(buf))) > 0) {
            body.append(buf, static_cast<size_t>(received));
            if (body.size() >= kMaxFormBody) {
                break;  // 超限截断（正常表单远小于此）
            }
        }
        ProvisionResult r = ParseProvisionForm(body);
        if (!r.ok) {
            ESP_LOGW(kTag, "配网表单无效（缺 ssid 或 body 为空）");
            SendPage(req, self->ap_ssid, "请填写 WiFi 名称");
            return ESP_OK;
        }
        if (self->on_saved) {
            self->on_saved(r);  // Application 写 NVS + 起 2s 重启定时器
        }
        // 成功页：提示设备即将重启（须先把响应发出去，否则手机看不到提示）
        constexpr const char* kOkPage =
            "<!doctype html><html><head><meta charset=utf-8>"
            "<meta name=viewport content='width=device-width,initial-scale=1'></head>"
            "<body style='font-family:sans-serif;text-align:center;padding-top:72px;color:#333'>"
            "<h2 style='color:#188038'>配置成功</h2>"
            "<p>设备即将重启并连接 WiFi，页面将在数秒后断开。</p></body></html>";
        httpd_resp_set_type(req, "text/html; charset=utf-8");
        httpd_resp_send(req, kOkPage, static_cast<int>(std::strlen(kOkPage)));
        ESP_LOGI(kTag, "配网成功，等待设备重启");
        return ESP_OK;
    }
};

ProvisioningEsp::ProvisioningEsp() : impl_(std::make_unique<Impl>()) {}

ProvisioningEsp::~ProvisioningEsp() { Stop(); }

bool ProvisioningEsp::Start(const std::string& ap_ssid,
                            std::function<void(const ProvisionResult&)> on_saved) {
    Impl& i = *impl_;
    if (i.server != nullptr) {
        return true;  // 已启动，幂等
    }
    i.ap_ssid = ap_ssid;
    i.on_saved = std::move(on_saved);

    httpd_config_t cfg = HTTPD_DEFAULT_CONFIG();
    cfg.lru_purge_enable = true;  // 释放关闭会话，避免 80 端口被残留连接占满
    cfg.max_uri_handlers = 4;
    if (httpd_start(&i.server, &cfg) != ESP_OK) {
        ESP_LOGE(kTag, "httpd 启动失败");
        return false;
    }

    httpd_uri_t root = {};
    root.uri = "/";
    root.method = HTTP_GET;
    root.handler = &Impl::HandleRoot;
    root.user_ctx = impl_.get();
    httpd_register_uri_handler(i.server, &root);

    httpd_uri_t save = {};
    save.uri = "/save";
    save.method = HTTP_POST;
    save.handler = &Impl::HandleSave;
    save.user_ctx = impl_.get();
    httpd_register_uri_handler(i.server, &save);

    ESP_LOGI(kTag, "配网服务已启动: http://192.168.4.1/");
    return true;
}

void ProvisioningEsp::Stop() {
    Impl& i = *impl_;
    if (i.server != nullptr) {
        httpd_stop(i.server);
        i.server = nullptr;
    }
}

}  // namespace kyle
