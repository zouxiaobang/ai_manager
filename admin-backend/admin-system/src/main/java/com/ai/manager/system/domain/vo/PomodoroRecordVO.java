package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 番茄钟记录响应 VO
 * 对外暴露记录信息，剔除逻辑删除标记 deleted 等内部字段，避免实体直接泄露到前端。
 */
@Data
public class PomodoroRecordVO {

    private Long id;

    private Long planId;

    /** WORK / SHORT_BREAK / LONG_BREAK */
    private String recordType;

    private Integer durationSec;

    private Integer roundIndex;

    private LocalDate statDate;

    private String source;

    private String remark;

    private LocalDateTime createTime;
}
