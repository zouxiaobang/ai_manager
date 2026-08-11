#include "core/wifi_config.h"

namespace kyle {

WifiConfig LoadWifiConfig(IStorage& storage) {
    WifiConfig cfg;
    cfg.ssid = storage.GetString("wifi", "ssid");
    cfg.password = storage.GetString("wifi", "password");
#ifdef CONFIG_WIFI_SSID
    if (cfg.ssid.empty()) cfg.ssid = CONFIG_WIFI_SSID;
#endif
#ifdef CONFIG_WIFI_PASSWORD
    if (cfg.password.empty()) cfg.password = CONFIG_WIFI_PASSWORD;
#endif
    return cfg;
}

}  // namespace kyle
