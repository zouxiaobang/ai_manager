package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocLibrarySearchRequest {

    private String keyword;

    private Long folderId;

    private String extension;

    private Long tagId;

    private LocalDateTime dateFrom;

    private LocalDateTime dateTo;

    private Integer page = 1;

    private Integer size = 20;
}
