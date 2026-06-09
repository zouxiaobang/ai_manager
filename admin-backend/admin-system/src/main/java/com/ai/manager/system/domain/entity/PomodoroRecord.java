package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pomodoro_record")
public class PomodoroRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    /** WORK / SHORT_BREAK / LONG_BREAK */
    private String recordType;

    private Integer durationSec;

    private Integer roundIndex;

    private LocalDate statDate;

    private String source;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
