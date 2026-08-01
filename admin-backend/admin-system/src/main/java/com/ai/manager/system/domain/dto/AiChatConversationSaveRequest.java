package com.ai.manager.system.domain.dto;

import lombok.Data;

/**
 * 更新对话请求
 */
@Data
public class AiChatConversationSaveRequest {
    private String title;
    private String messages;
}
