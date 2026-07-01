#pragma once

#include "esp_err.h"

/** 连接 WiFi（阻塞直到成功或超时）。未配置 SSID 时返回 ESP_ERR_INVALID_STATE。 */
esp_err_t wifi_sta_connect();

bool wifi_sta_is_connected();

/** 初始化 mDNS，使 *.local 主机名可解析。 */
void wifi_sta_init_mdns();
