#ifndef KYLE_CORE_OTA_REPORT_H
#define KYLE_CORE_OTA_REPORT_H

// OTA 升级状态上报：进度节流 + 上报 body 组装。
// 纯逻辑，零 ESP-IDF 依赖，可在 host 单测（黄金向量锁定 JSON 格式）。
// 对齐后端 POST {ota_base}status（字段名与后端 OtaStatusRequest 一致）。

#include <cstddef>
#include <string>

namespace kyle {

// OTA 上报状态枚举（与后端 OTA 记录状态语义对齐：
// DOWNLOADING=下载中(progress 0-99)，SUCCESS/FAILED=终态)
enum class OtaReportState {
    kDownloading,  // 下载中
    kSuccess,      // 升级成功
    kFailed,       // 升级失败
};

// 组装 POST {ota_base}status 的 JSON body。
// mac 为小写去冒号（与后端 iot_device.mac 一致）；progress 0-100。
std::string BuildOtaStatusBody(const std::string& mac, OtaReportState state, int progress);

// 拼接 OTA 状态上报完整 URL：ota_base 形如 "http://host:port/api/iot/ota/"
// 返回 base + "status"。base 尾斜杠可缺省（自动补）。与 BuildOtaCheckUrl 同模式。
std::string BuildOtaStatusUrl(const std::string& ota_base);

// 计算下载进度百分比：total 未知（0）返回 0；received>=total 返回 100。
int ComputeOtaPercent(std::size_t received, std::size_t total);

// 进度上报节流器：只在进度跨越阈值（默认每 10%）或到终态时才允许上报，
// 避免下载过程中高频刷后端。纯逻辑，host 可测。
class OtaProgressThrottle {
public:
    explicit OtaProgressThrottle(int step_percent = 10);

    // 传入当前进度（0-100）与是否终态；若应上报返回 true，并内部记录上次上报值。
    bool ShouldReport(int progress, bool is_terminal);

private:
    int step_percent_;   // 每跨过多少个百分点上报一次
    int last_reported_;  // 上次已上报的进度（初值 -1 表示尚未上报）
};

}  // namespace kyle

#endif  // KYLE_CORE_OTA_REPORT_H
