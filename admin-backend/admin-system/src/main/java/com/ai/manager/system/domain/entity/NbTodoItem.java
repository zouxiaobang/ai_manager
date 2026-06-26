package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nb_todo_item")
public class NbTodoItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private Integer completed;

    private LocalDateTime dueTime;

    private LocalDateTime remindTime;

    private String repeatType;

    private Integer repeatInterval;

    private LocalDateTime repeatUntil;

    private String repeatDays;

    private Integer remindNotified;

    private Long seriesId;

    private Integer sortOrder;

    private Integer pinned;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
