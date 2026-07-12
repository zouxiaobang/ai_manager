#pragma once

#include "esp_err.h"

/** 连接 WiFi（阻塞直到成功或超时）。未配置 SSID 时返回 ESP_ERR_INVALID_STATE。 */
esp_err_t wifi_sta_connect();

bool wifi_sta_is_connected();

/** 初始化 mDNS，使 *.local 主机名可解析。 */
void wifi_sta_init_mdns();

/**
 * Wait for the WiFi heavy init (phy_init + esp_wifi_start) to complete.
 * Returns true within timeout_ms, false on timeout.
 * Use this to re-enable power-hungry peripherals (e.g. backlight) after
 * the WiFi RF calibration spike has passed.
 */
bool wifi_sta_wait_heavy_init_done(uint32_t timeout_ms);
