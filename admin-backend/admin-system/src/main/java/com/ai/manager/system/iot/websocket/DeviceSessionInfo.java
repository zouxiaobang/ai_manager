package com.ai.manager.system.iot.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线设备会话元数据（WsSessionRegistry 维护）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSessionInfo {

    private String deviceId;

    private String mac;

    private String model;

    private String sessionId;

    private LocalDateTime startedAt;
}
