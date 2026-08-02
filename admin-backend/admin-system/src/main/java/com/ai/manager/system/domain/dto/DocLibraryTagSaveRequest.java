package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocLibraryTagSaveRequest {

    /** 标签名称 */
    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String color;
}
