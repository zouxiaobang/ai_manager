package com.ai.manager.system.iot.domain.dto;

import lombok.Data;

/**
 * 设备信息更新请求（PUT /api/iot/device/{id}），字段均可选、非空才更新。
 * <p>iot_device 表暂无独立名称列，设备展示名以后端 VO 的 mac/uuid 兜底。</p>
 */
@Data
public class DeviceUpdateRequest {

    private String model;

    private String chip;

    private String firmwareVersion;
}
