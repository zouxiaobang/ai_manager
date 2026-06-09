package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PomodoroDailyStatVO {

    private LocalDate statDate;

    private Integer workRounds;

    private Integer workMinutes;

    private Integer breakMinutes;

    private Integer totalMinutes;
}
