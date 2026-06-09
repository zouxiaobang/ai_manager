package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class PomodoroPlanSaveRequest {

    private String title;

    private Integer workDurationMin;

    private Integer shortBreakMin;

    private Integer longBreakMin;

    private Integer roundsBeforeLongBreak;

    private Integer dailyGoalRounds;

    private Integer dailyGoalMinutes;

    private Boolean asDefault;

    private String status;
}
