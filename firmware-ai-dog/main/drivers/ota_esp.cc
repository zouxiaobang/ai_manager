#include "drivers/ota_esp.h"

#include <utility>

#include "esp_http_client.h"
#include "esp_log.h"
#include "esp_ota_ops.h"
#include "esp_system.h"

namespace kyle {

namespace {
constexpr const char* kTag = "OtaEsp";
}  // namespace

// pimpl：esp_ota 句柄与 http 事件上下文都在 Impl。
// esp_http_client 的 ON_DATA 回调在下载线程内直接 esp_ota_write，边下载边落盘。
struct OtaUpdaterEsp::Impl {
    esp_ota_handle_t ota_handle = 0;
    esp_err_t write_err = ESP_OK;
    bool got_data = false;
    std::size_t total_bytes = 0;      // 响应体总长（HTTP_EVENT_ON_HEADERS 取 Content-Length）
    std::size_t received_bytes = 0;   // 已下载字节（ON_DATA 累计）
    OtaProgressCallback on_progress;
    OtaDoneCallback on_done;

    static esp_err_t HandleHttpEvent(esp_http_client_event_t* evt) {
        Impl* self = static_cast<Impl*>(evt->user_data);
        if (evt->event_id == HTTP_EVENT_ON_HEADER) {
            // 每个 header 行触发一次；Content-Length 行解析后即可查（多次覆盖取最终值）。
            // ON_DATA 用 total_bytes 算下载进度百分比。
            self->total_bytes = esp_http_client_get_content_length(evt->client);
        } else if (evt->event_id == HTTP_EVENT_ON_DATA && evt->data_len > 0) {
            self->got_data = true;
            esp_err_t e = esp_ota_write(self->ota_handle, evt->data, evt->data_len);
            if (e != ESP_OK) {
                self->write_err = e;
                ESP_LOGE(kTag, "esp_ota_write failed: %s", esp_err_to_name(e));
            }
            self->received_bytes += evt->data_len;
            // 上报回调在下载线程内同步触发（与 esp_ota_write 同一上下文），实现须轻量
            if (self->on_progress) {
                self->on_progress(self->received_bytes, self->total_bytes);
            }
        }
        return ESP_OK;
    }
};

OtaUpdaterEsp::OtaUpdaterEsp() : impl_(std::make_unique<Impl>()) {}

OtaUpdaterEsp::~OtaUpdaterEsp() = default;

bool OtaUpdaterEsp::UpgradeFromUrl(const std::string& url,
                                   OtaProgressCallback on_progress,
                                   OtaDoneCallback on_done) {
    Impl& i = *impl_;
    i.on_progress = std::move(on_progress);
    i.on_done = std::move(on_done);
    i.total_bytes = 0;
    i.received_bytes = 0;
    // 终态上报统一出口：成功/失败各返回路径都经此发一次（收到多少报多少）
    auto report_done = [&i](bool ok) {
        if (i.on_done) {
            i.on_done(ok, i.received_bytes, i.total_bytes);
        }
    };

    // 1. 找空闲 OTA 分区：factory 为出厂版本，升级写 ota_0/ota_1 二选一
    const esp_partition_t* partition = esp_ota_get_next_update_partition(nullptr);
    if (partition == nullptr) {
        ESP_LOGE(kTag, "no OTA partition available");
        report_done(false);
        return false;
    }
    ESP_LOGI(kTag, "upgrade target partition: %s", partition->label);

    // 2. 开始 OTA 写入（大小未知，esp_ota 增量写入）
    esp_err_t err = esp_ota_begin(partition, OTA_SIZE_UNKNOWN, &i.ota_handle);
    if (err != ESP_OK) {
        ESP_LOGE(kTag, "esp_ota_begin failed: %s", esp_err_to_name(err));
        report_done(false);
        return false;
    }
    i.write_err = ESP_OK;
    i.got_data = false;

    // 3. 流式下载固件，ON_DATA 直接写 OTA 分区
    esp_http_client_config_t cfg = {};
    cfg.url = url.c_str();
    cfg.event_handler = &Impl::HandleHttpEvent;
    cfg.user_data = impl_.get();
    cfg.timeout_ms = 30000;
    esp_http_client_handle_t client = esp_http_client_init(&cfg);
    if (client == nullptr) {
        esp_ota_abort(i.ota_handle);
        report_done(false);
        return false;
    }
    esp_err_t http_err = esp_http_client_perform(client);
    int status = esp_http_client_get_status_code(client);
    esp_http_client_cleanup(client);

    // 4. 校验：HTTP 200 + 写入无错 + 确实收到数据
    if (http_err != ESP_OK || status != 200 || i.write_err != ESP_OK || !i.got_data) {
        ESP_LOGE(kTag, "download failed: http=%d(%s) write=%s got_data=%d", status,
                 esp_err_to_name(http_err), esp_err_to_name(i.write_err), i.got_data ? 1 : 0);
        esp_ota_abort(i.ota_handle);
        report_done(false);
        return false;
    }

    // 5. 结束 OTA（校验镜像完整性）并设置启动分区
    err = esp_ota_end(i.ota_handle);
    if (err != ESP_OK) {
        ESP_LOGE(kTag, "esp_ota_end failed: %s", esp_err_to_name(err));
        report_done(false);
        return false;
    }
    err = esp_ota_set_boot_partition(partition);
    if (err != ESP_OK) {
        ESP_LOGE(kTag, "esp_ota_set_boot_partition failed: %s", esp_err_to_name(err));
        report_done(false);
        return false;
    }

    ESP_LOGI(kTag, "OTA complete, rebooting...");
    report_done(true);
    esp_restart();
    return true;  // esp_restart 不返回，此处仅为编译满足
}

}  // namespace kyle
