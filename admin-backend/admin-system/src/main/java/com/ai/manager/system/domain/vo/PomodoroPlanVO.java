package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 番茄钟计划响应 VO
 * 对外暴露计划信息，剔除逻辑删除标记 deleted 等内部字段，避免实体直接泄露到前端。
 */
@Data
public class PomodoroPlanVO {

    private Long id;

    private String title;

    private Integer workDurationMin;

    private Integer shortBreakMin;

    private Integer longBreakMin;

    private Integer roundsBeforeLongBreak;

    private Integer dailyGoalRounds;

    private Integer dailyGoalMinutes;

    private Integer isDefault;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
