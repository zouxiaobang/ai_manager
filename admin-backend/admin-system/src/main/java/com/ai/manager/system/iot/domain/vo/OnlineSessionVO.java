package com.ai.manager.system.iot.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线会话视图对象（后台实时查看设备连接）
 */
@Data
public class OnlineSessionVO {

    private Long id;

    private Long deviceId;

    private String deviceMac;

    private String deviceModel;

    private String sessionId;

    private LocalDateTime startedAt;

    private Integer turnCount;
}
