package com.ai.manager.system.iot.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OTA 升级记录视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtaRecordVO {

    private Long id;

    private Long deviceId;

    /** 设备展示名（取 MAC 兜底），后台列表展示用 */
    private String deviceName;

    private Long firmwareId;

    /** 固件版本号，后台列表展示用 */
    private String firmwareVersion;

    private String state;

    private Integer progress;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createTime;
}
