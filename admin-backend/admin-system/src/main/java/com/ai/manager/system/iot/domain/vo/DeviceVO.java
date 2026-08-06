package com.ai.manager.system.iot.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备管理视图对象（不含 deleted）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceVO {

    private Long id;

    private String uuid;

    private String clientId;

    private String mac;

    private String model;

    private String chip;

    private String firmwareVersion;

    private LocalDateTime activatedAt;

    private LocalDateTime lastSeenAt;

    private String status;

    private String sessionId;

    private String otaState;

    /** 是否在线（由 WsSessionRegistry 实时计算） */
    private Boolean online;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
