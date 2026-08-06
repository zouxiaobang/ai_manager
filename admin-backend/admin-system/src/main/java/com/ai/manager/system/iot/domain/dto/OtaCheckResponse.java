package com.ai.manager.system.iot.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

/**
 * OTA 版本检查响应（严格对齐设备端协议，见《new-architecture-design.md》7.4）
 * <p>
 * 顶层字段固定为 activation / websocket / mqtt / server_time / firmware，snake_case 序列化。
 * </p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtaCheckResponse {

    /** 激活扩展信息（当前空对象占位，序列化为 {}） */
    private Map<String, Object> activation = Collections.emptyMap();

    /** WebSocket 通道配置 */
    private WebsocketConfig websocket;

    /** MQTT 通道配置（未启用时 endpoint 为空串） */
    private MqttConfig mqtt;

    /** 服务器时间 */
    private ServerTime serverTime;

    /** 固件升级信息（无新版本时为 null，不序列化） */
    private FirmwareInfo firmware;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WebsocketConfig {
        private String url;
        private String token;
        private int version;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MqttConfig {
        private String endpoint;
        private String clientId;
        private String username;
        private String password;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServerTime {
        private long timestamp;
        private int timezoneOffset;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FirmwareInfo {
        private String version;
        private String url;
        private boolean force;
    }
}
