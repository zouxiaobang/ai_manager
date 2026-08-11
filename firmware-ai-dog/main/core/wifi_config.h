#ifndef KYLE_CORE_WIFI_CONFIG_H
#define KYLE_CORE_WIFI_CONFIG_H

// WiFi 连接配置：NVS 优先，Kconfig 兜底。纯逻辑，零 ESP-IDF 依赖，可在 host 单测。

#include <string>

#include "core/net_config.h"  // IStorage

namespace kyle {

// WiFi 凭据。configured() 为 false 表示无可用配网信息（需进入配网流程）。
struct WifiConfig {
    std::string ssid;
    std::string password;

    bool configured() const { return !ssid.empty(); }
};

// 从 IStorage 读 wifi/ssid、wifi/password；为空则回退编译期 Kconfig 兜底值。
// 返回值的 configured() 决定设备是否跳过配网直接连接。
WifiConfig LoadWifiConfig(IStorage& storage);

}  // namespace kyle

#endif  // KYLE_CORE_WIFI_CONFIG_H
