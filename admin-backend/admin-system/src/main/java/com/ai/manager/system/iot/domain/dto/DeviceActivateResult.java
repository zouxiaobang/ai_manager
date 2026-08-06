package com.ai.manager.system.iot.domain.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备激活结果
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeviceActivateResult {

    private boolean success;

    private Long deviceId;

    private LocalDateTime activatedAt;
}
