#ifndef KYLE_CORE_PROVISIONING_H
#define KYLE_CORE_PROVISIONING_H

// SoftAP 配网纯逻辑：热点命名、配网页生成、表单解析。
// 零 ESP-IDF 依赖，可在 host 单测。真机流程：开热点 → 手机连热点 →
// 浏览器开 http://192.168.4.1 → 提交 WiFi 凭据 → 写 NVS + 重启。

#include <string>

namespace kyle {

// 配网表单解析结果（Application 据此写 NVS 并重启）
struct ProvisionResult {
    bool ok = false;
    std::string ssid;
    std::string password;
};

// 热点 SSID：kyle-<MAC 后 4 位>（去冒号小写，如 aa:bb:cc:dd:ee:ff → kyle-eeff）。
// SSID ≤ 32 字节安全（9 字节）；mac 过短/空时回退固定后缀避免只有前缀。
std::string BuildApSsid(const std::string& mac);

// 配网页 HTML：移动端友好、内联样式、无外网资源（配网时设备不能上网）。
// error 非空时表单上方显示错误提示（提交失败重试场景）。
std::string BuildProvisionPage(const std::string& ap_ssid,
                               const std::string& error = "");

// URL 解码：+ → 空格，%XX → 原字节（UTF-8 中文 SSID 原样保留）。
std::string UrlDecode(const std::string& s);

// 解析 application/x-www-form-urlencoded 表单（字段 ssid / password）。
// ssid 缺失或为空 → ok=false；多余字段忽略。
ProvisionResult ParseProvisionForm(const std::string& body);

}  // namespace kyle

#endif  // KYLE_CORE_PROVISIONING_H
