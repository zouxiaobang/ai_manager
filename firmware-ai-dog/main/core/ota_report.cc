#include "core/ota_report.h"

#include <algorithm>
#include <cstddef>
#include <cstdio>

namespace kyle {

namespace {
// 与 ota_check.cc 同源的最小 JSON 字符串转义：引号/反斜杠/控制字符。
// 中文等多字节 UTF-8 原样输出（合法 JSON，后端 Jackson 直接解码）。
std::string EscapeJson(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (char c : s) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            case '\b': out += "\\b"; break;
            case '\f': out += "\\f"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    // 其余控制字符用 \uXXXX
                    const char* hex = "0123456789abcdef";
                    out += "\\u00";
                    out += hex[(c >> 4) & 0xF];
                    out += hex[c & 0xF];
                } else {
                    out += c;
                }
        }
    }
    return out;
}

const char* StateToString(OtaReportState s) {
    switch (s) {
        case OtaReportState::kDownloading: return "DOWNLOADING";
        case OtaReportState::kSuccess:     return "SUCCESS";
        case OtaReportState::kFailed:      return "FAILED";
    }
    return "FAILED";
}
}  // namespace

std::string BuildOtaStatusBody(const std::string& mac, OtaReportState state, int progress) {
    // 字段顺序固定，snake_case 对齐后端 OtaStatusRequest（黄金向量锁定格式）。
    // progress 钳位 0-100，防驱动误传越界值污染记录。
    progress = std::clamp(progress, 0, 100);
    char progress_buf[8];
    std::snprintf(progress_buf, sizeof(progress_buf), "%d", progress);
    return std::string("{")
        + "\"mac\":\"" + EscapeJson(mac) + "\","
        + "\"state\":\"" + StateToString(state) + "\","
        + "\"progress\":" + progress_buf
        + "}";
}

std::string BuildOtaStatusUrl(const std::string& ota_base) {
    // 与 BuildOtaCheckUrl 同一拼接模式：去掉尾斜杠统一拼 "/status"
    std::string base = ota_base;
    while (!base.empty() && base.back() == '/') {
        base.pop_back();
    }
    return base + "/status";
}

int ComputeOtaPercent(size_t received, size_t total) {
    if (total == 0) {
        return 0;  // Content-Length 未知（chunked 等），无法算百分比
    }
    if (received >= total) {
        return 100;
    }
    return static_cast<int>(received * 100 / total);
}

OtaProgressThrottle::OtaProgressThrottle(int step_percent)
    : step_percent_(step_percent), last_reported_(-1) {
    // 非法阈值兜底为 10%（避免 0/负数导致每次进度都上报）
    if (step_percent_ <= 0) {
        step_percent_ = 10;
    }
}

bool OtaProgressThrottle::ShouldReport(int progress, bool is_terminal) {
    progress = std::clamp(progress, 0, 100);
    if (is_terminal) {
        // 终态（成功/失败）无条件上报，不再受阈值限制
        last_reported_ = progress;
        return true;
    }
    if (last_reported_ < 0) {
        // 首次上报：跨过第一个里程碑（step%）才发，避免下载刚启动就刷一条 0%
        if (progress < step_percent_) {
            return false;
        }
    } else if (progress - last_reported_ < step_percent_) {
        // 距上次上报不足 step%，节流跳过
        return false;
    }
    last_reported_ = progress;
    return true;
}

}  // namespace kyle
