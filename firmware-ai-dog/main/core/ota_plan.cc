#include "core/ota_plan.h"

#include <string>

namespace kyle {

bool IsValidFirmwareUrl(const std::string& url) {
    // 只接受 http(s) 绝对地址；其它（如裸路径、ftp）不触发下载
    return url.rfind("http://", 0) == 0 || url.rfind("https://", 0) == 0;
}

OtaPlan PlanOtaUpgrade(const std::string& current_version, const FirmwareInfo& fw) {
    OtaPlan plan;
    if (!fw.present) {
        return plan;  // 未下发固件，不升级
    }
    if (!IsValidFirmwareUrl(fw.url)) {
        return plan;  // 下载链接无效，跳过（记录在案，等下次 check）
    }
    if (ShouldUpgrade(current_version, fw)) {
        plan.upgrade = true;
        plan.url = fw.url;
        plan.version = fw.version;
    }
    return plan;
}

}  // namespace kyle
