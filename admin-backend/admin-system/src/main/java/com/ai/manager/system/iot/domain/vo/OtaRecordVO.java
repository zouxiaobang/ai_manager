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

    private Long firmwareId;

    private String state;

    private Integer progress;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createTime;
}
