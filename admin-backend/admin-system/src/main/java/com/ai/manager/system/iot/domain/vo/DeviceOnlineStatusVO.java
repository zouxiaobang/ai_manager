package com.ai.manager.system.iot.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备在线状态探测结果（GET /api/iot/device/{id}/online）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceOnlineStatusVO {

    /** 是否在线（WS 会话注册表实时判断） */
    private Boolean online;

    /** 最近在线/上报时间 */
    private LocalDateTime lastSeenAt;
}
