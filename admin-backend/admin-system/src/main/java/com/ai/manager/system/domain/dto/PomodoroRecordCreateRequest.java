package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class PomodoroRecordCreateRequest {

    private Long planId;

    /** WORK / SHORT_BREAK / LONG_BREAK */
    private String recordType;

    private Integer durationSec;

    private Integer roundIndex;

    /** ADMIN / DEVICE，默认 ADMIN */
    private String source;

    private String remark;
}
