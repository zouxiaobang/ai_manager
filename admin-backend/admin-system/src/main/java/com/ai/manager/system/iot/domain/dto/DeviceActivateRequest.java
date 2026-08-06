package com.ai.manager.system.iot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备激活请求（POST /api/iot/ota/activate）
 * <p>
 * 设备端用 efuse HMAC-SHA256 对 mac:nonce:timestamp 签名后上报；后端按 activation-secret 校验。
 * </p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceActivateRequest {

    /** MAC 地址 */
    private String mac;

    /** 设备 UUID */
    private String uuid;

    /** 随机数（防重放） */
    private String nonce;

    /** 请求时间戳（秒） */
    private long timestamp;

    /** HMAC-SHA256 十六进制签名 */
    private String signature;
}
