#ifndef KYLE_CORE_OTA_CHECK_H
#define KYLE_CORE_OTA_CHECK_H

// OTA 版本检查客户端协议：请求 body 组装 + 下载 URL 拼接。
// 纯逻辑，零 ESP-IDF 依赖，可在 host 单测（黄金向量锁定格式）。
// 对齐后端 POST /api/iot/ota/check（OtaCheckRequest，snake_case 字段）。

#include <string>

namespace kyle {

// 设备系统信息（OTA check 上报；字段与后端 OtaCheckRequest 对齐）
struct DeviceInfo {
    std::string uuid;             // 设备 UUID（可空，后端缺省签发）
    std::string client_id;        // 客户端 ID
    std::string mac;              // MAC（小写去冒号，后端按此识别/注册设备）
    std::string model;            // 机型
    std::string chip;             // 芯片型号
    std::string board;            // 板卡 ID（如 kyle-s3-lcd）
    std::string language;         // 语言代码
    std::string firmware_version; // 当前固件版本
};

// 组装 POST /api/iot/ota/check 的 JSON body（snake_case，对齐后端反序列化）
std::string BuildOtaCheckBody(const DeviceInfo& info);

// 拼接 OTA check 完整 URL：ota_base 形如 "http://host:port/api/iot/ota/"
// 返回 base + "check"。base 尾斜杠可缺省（自动补）。
std::string BuildOtaCheckUrl(const std::string& ota_base);

}  // namespace kyle

#endif  // KYLE_CORE_OTA_CHECK_H
