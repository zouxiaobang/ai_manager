package com.ai.manager.system.iot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备侧 OTA 版本检查请求（POST /api/iot/ota/check）
 * <p>
 * 设备上电配网后 POST 系统信息，字段与设备端 CONFIG_OTA_URL 上报格式对齐（snake_case）。
 * </p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtaCheckRequest {

    /** 设备 UUID */
    private String uuid;

    /** 客户端 ID */
    private String clientId;

    /** MAC 地址 */
    private String mac;

    /** 机型 */
    private String model;

    /** 芯片 */
    private String chip;

    /** 板卡 ID（如 supermini-c3） */
    private String board;

    /** 语言 */
    private String language;

    /** 当前固件版本 */
    private String firmwareVersion;
}
