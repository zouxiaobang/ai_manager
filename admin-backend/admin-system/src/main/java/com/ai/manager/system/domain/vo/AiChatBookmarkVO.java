package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库对话标记
 */
@Data
public class AiChatBookmarkVO {

    /** Long 序列化为字符串，避免前端 JS 精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    private String name;

    private String msgId;

    private Integer msgOffsetTop;

    private Integer scrollTop;

    /** 与既有 AiChatConversationVO 一致，对外统一用 createdAt（实体侧为 createTime） */
    private LocalDateTime createdAt;
}
