package com.ai.manager.system.domain.vo;

import lombok.Data;

/**
 * AI 知识库 - 提供商信息 VO
 *
 * <p>用于模型选择器下拉列表，数据来自数据库中各提供商的配置</p>
 */
@Data
public class AiKnowledgeProviderInfoVO {

    /** 提供商 key: openai / claude / deepseek / qwen / custom */
    private String provider;

    /** 模型名称（用户已配置的，或默认值） */
    private String model;

    /** 是否已配置（有 API Key） */
    private boolean configured;

    /** 是否为默认模型 */
    private boolean defaultProvider;

    /** 上下文消息数（记忆轮次） */
    private Integer maxContextMessages;

    /** 大模型上下文窗口大小（Token），如 65536 表示 64K */
    private Integer maxContextTokens;
}
