package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建对话请求
 */
@Data
public class AiChatConversationCreateRequest {
    @NotNull(message = "分类 ID 不能为空")
    private Long categoryId;
}
