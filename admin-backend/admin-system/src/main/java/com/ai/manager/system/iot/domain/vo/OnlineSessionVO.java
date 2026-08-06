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

    /** 设备展示名（暂无独立名称列，取 MAC 兜底） */
    private String deviceName;

    private String deviceMac;

    private String deviceModel;

    private String sessionId;

    private LocalDateTime startedAt;

    /** 会话结束时间（null 表示仍在线） */
    private LocalDateTime endedAt;

    private Integer turnCount;

    /** 是否在线（endedAt 为空即在线） */
    private Boolean online;
}
