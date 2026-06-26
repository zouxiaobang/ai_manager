package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NbNoteTrashItemVO {

    private Long id;

    private String title;

    private Long notebookId;

    private String notebookName;

    private String contentExcerpt;

    private LocalDateTime updateTime;
}
