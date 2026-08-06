package com.ai.manager.system.iot.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台绑定设备请求
 */
@Data
public class DeviceBindRequest {

    @NotBlank(message = "MAC 地址不能为空")
    private String mac;

    /** 设备 UUID（可选，缺省时后端签发） */
    private String uuid;

    /** 机型 */
    private String model;

    /** 芯片 */
    private String chip;

    /** 当前固件版本 */
    private String firmwareVersion;
}
