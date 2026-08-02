package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocLibraryFolderSaveRequest {

    private Long parentId;

    /** 文件夹名称 */
    @NotBlank(message = "文件夹名称不能为空")
    private String name;

    private String icon;

    private String color;

    private Integer sortOrder;
}
