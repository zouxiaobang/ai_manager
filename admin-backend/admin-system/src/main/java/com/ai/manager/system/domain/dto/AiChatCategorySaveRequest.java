package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/重命名分类请求
 */
@Data
public class AiChatCategorySaveRequest {
    @NotBlank(message = "分类名称不能为空")
    private String name;
}
