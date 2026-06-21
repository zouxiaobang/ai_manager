package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class NbNoteSaveRequest {

    private Long notebookId;

    private String title;

    private String content;

    private String noteType;

    private Boolean pinned;

    private Boolean favorite;

    private Integer sortOrder;

    private String status;

    private List<Long> tagIds;
}
