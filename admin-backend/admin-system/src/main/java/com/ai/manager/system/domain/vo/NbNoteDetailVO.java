package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NbNoteDetailVO {

    private Long id;

    private Long notebookId;

    private String title;

    private String content;

    private String contentExcerpt;

    private Long contentSize;

    private String syncStatus;

    private String syncError;

    private String noteType;

    private Integer isPinned;

    private Integer isFavorite;

    private Integer sortOrder;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<NbNoteTagVO> tags;
}
