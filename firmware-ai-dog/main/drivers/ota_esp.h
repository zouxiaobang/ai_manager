#ifndef KYLE_DRIVERS_OTA_ESP_H
#define KYLE_DRIVERS_OTA_ESP_H

#include <cstddef>
#include <functional>
#include <memory>
#include <string>

namespace kyle {

// 下载进度回调：received/total 为已下载/总字节（total=0 表示 Content-Length 未知）。
// 在下载线程内触发（ON_DATA），实现须轻量——Application 用它算百分比+节流后上报后端。
using OtaProgressCallback = std::function<void(std::size_t received, std::size_t total)>;
// 下载终态回调：success 表示升级是否成功；received/total 为终止时进度（失败时用于上报已下载比例）。
using OtaDoneCallback = std::function<void(bool success, std::size_t received, std::size_t total)>;

// esp_ota 升级执行器：从 url 流式下载固件写入空闲 OTA 分区。
// 成功 → 设置启动分区并重启（调用不返回）；失败 → 返回 false（可重试）。
// 下载与写入共用 esp_http_client 事件回调（ON_DATA 直接 esp_ota_write），
// 避免大固件整块缓冲；pimpl 保持头文件零 ESP-IDF 依赖。
class OtaUpdaterEsp {
public:
    OtaUpdaterEsp();
    ~OtaUpdaterEsp();

    // 同步执行 OTA 升级。url 必须已通过 IsValidFirmwareUrl 校验。
    // on_progress 在 ON_DATA 触发（received/total 原始字节）；on_done 在终态触发一次。
    // 返回 false 表示失败（分区已 abort，系统未重启）。
    bool UpgradeFromUrl(const std::string& url,
                        OtaProgressCallback on_progress = nullptr,
                        OtaDoneCallback on_done = nullptr);

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace kyle

#endif  // KYLE_DRIVERS_OTA_ESP_H
