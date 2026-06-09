package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pomodoro_plan")
public class PomodoroPlan {

    @TableId(type = IdType.AUTO)
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

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
