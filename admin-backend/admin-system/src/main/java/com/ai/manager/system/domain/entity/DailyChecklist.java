package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_checklist")
public class DailyChecklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate checklistDate;

    private String itemKey;

    private Integer completed;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
