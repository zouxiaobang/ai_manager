package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class DocLibraryFolderSaveRequest {

    private Long parentId;

    private String name;

    private String icon;

    private String color;

    private Integer sortOrder;
}
