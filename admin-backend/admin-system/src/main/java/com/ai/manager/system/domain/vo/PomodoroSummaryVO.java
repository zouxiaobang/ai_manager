package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class PomodoroSummaryVO {

    private Integer totalWorkRounds;

    private Integer totalWorkMinutes;

    private Integer totalBreakMinutes;

    private Integer activeDays;

    private Double avgWorkMinutesPerDay;
}
