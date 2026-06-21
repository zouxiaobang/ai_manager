package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NbNoteListMetaVO {

    private Long id;

    private String contentExcerpt;

    private Long contentSize;

    private String syncStatus;

    private LocalDateTime createTime;
}
