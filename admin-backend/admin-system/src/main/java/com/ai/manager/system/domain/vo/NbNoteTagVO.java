package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NbNoteTagVO {

    private Long id;

    private String name;

    private String color;

    private LocalDateTime createTime;
}
