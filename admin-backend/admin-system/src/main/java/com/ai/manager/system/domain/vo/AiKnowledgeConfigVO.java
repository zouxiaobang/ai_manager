package com.ai.manager.system.domain.vo;

import lombok.Data;

/**
 * AI 知识库 - 模型配置 VO
 */
@Data
public class AiKnowledgeConfigVO {

    /** 模型提供商: openai / claude / deepseek / qwen / custom */
    private String provider;

    /** API Key (脱敏返回，仅最后4位明文) */
    private String apiKey;

    /** API 地址 */
    private String apiBaseUrl;

    /** 模型名称 */
    private String model;

    /** 温度 */
    private Double temperature;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** Embedding 模型 */
    private String embeddingModel;

    /** 是否设为默认模型 */
    private Boolean defaultProvider;

    /** 上下文消息数（记忆轮次），决定发送给 AI 的历史消息条数，默认 10 */
    private Integer maxContextMessages;
}
