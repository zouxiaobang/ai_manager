package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库对话
 */
@Data
public class AiChatConversationVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
    private String title;
    private String messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
