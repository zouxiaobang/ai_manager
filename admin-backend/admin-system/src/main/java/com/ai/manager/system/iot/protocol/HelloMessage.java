package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备上行 hello 消息（设备端 → 后端）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HelloMessage {

    /** 协议类型，固定 hello */
    private String type;

    /** 设备 UUID */
    private String uuid;

    /** MAC 地址 */
    private String mac;

    /** 固件版本 */
    private String version;

    /** 客户端 ID */
    private String clientId;

    /** 音频采样率 */
    private Integer sampleRate;
}
