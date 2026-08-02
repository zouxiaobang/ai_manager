package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NbNotebookSaveRequest {

    private Long parentId;

    /** 笔记本名称 */
    @NotBlank(message = "笔记本名称不能为空")
    private String name;

    private String icon;

    private String color;

    private Integer sortOrder;
}
