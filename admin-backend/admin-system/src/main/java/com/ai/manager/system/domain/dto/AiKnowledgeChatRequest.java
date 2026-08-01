package com.ai.manager.system.domain.dto;

import lombok.Data;

/**
 * AI 知识库 - 智能聊天请求 DTO
 */
@Data
public class AiKnowledgeChatRequest {

    /** 用户问题 */
    private String question;

    /** 使用的模型提供商（为空则使用默认配置） */
    private String provider;

    /** 是否基于 RAG 知识库回答 */
    private Boolean useRag;

    /** 历史消息 (可选) */
    private java.util.List<ChatHistoryItem> history;

    @Data
    public static class ChatHistoryItem {
        private String role;
        private String content;
    }
}
