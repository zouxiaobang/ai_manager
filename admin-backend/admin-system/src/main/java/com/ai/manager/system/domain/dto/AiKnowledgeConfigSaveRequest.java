package com.ai.manager.system.domain.dto;

import lombok.Data;

/**
 * AI 知识库 - 保存模型配置请求 DTO
 */
@Data
public class AiKnowledgeConfigSaveRequest {

    /** 模型提供商: openai / claude / deepseek / qwen / custom */
    private String provider;

    /** API Key */
    private String apiKey;

    /** API 地址 */
    private String apiBaseUrl;

    /** 模型名称 */
    private String model;

    /** 温度 (0-2) */
    private Double temperature;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** Embedding 模型 */
    private String embeddingModel;

    /** 是否设为默认模型 */
    private Boolean defaultProvider;

    /** 上下文消息数（记忆轮次） */
    private Integer maxContextMessages;
}
