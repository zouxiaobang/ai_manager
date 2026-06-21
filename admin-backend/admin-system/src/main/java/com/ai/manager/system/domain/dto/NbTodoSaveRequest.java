package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NbTodoSaveRequest {

    private String content;

    private Boolean completed;

    private LocalDateTime dueTime;

    private Boolean clearDueTime;

    private LocalDateTime remindTime;

    private Boolean clearRemindTime;

    private String repeatType;

    private Integer repeatInterval;

    private LocalDateTime repeatUntil;

    private Boolean clearRepeatUntil;

    private Integer sortOrder;
}
