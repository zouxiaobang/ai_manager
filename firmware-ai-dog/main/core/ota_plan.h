#ifndef KYLE_CORE_OTA_PLAN_H
#define KYLE_CORE_OTA_PLAN_H

// OTA 升级计划决策：当前版本 + 服务端下发 → 是否升级 + 下载目标。
// 纯逻辑，零 ESP-IDF 依赖，可在 host 单测（复用 ShouldUpgrade 版本比较）。

#include <string>

#include "core/ota_version.h"

namespace kyle {

// 下载 URL 合法性校验：必须是 http(s) 绝对地址
bool IsValidFirmwareUrl(const std::string& url);

// 是否触发升级：固件下发存在 + force 或版本更新 + 下载链接有效
struct OtaPlan {
    bool upgrade = false;  // 需要升级
    std::string url;       // 下载地址（已校验有效）
    std::string version;   // 目标版本
};

OtaPlan PlanOtaUpgrade(const std::string& current_version, const FirmwareInfo& fw);

}  // namespace kyle

#endif  // KYLE_CORE_OTA_PLAN_H
