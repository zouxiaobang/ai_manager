package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 知识库对话分类（含对话列表）
 */
@Data
public class AiChatCategoryVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private List<AiChatConversationVO> conversations;
}
