package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NbTodoItemVO {

    private Long id;

    private String content;

    private Integer completed;

    private LocalDateTime dueTime;

    private LocalDateTime remindTime;

    private String repeatType;

    private Integer repeatInterval;

    private LocalDateTime repeatUntil;

    private String repeatDays;

    private Long seriesId;

    private Integer sortOrder;

    private Integer pinned;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
