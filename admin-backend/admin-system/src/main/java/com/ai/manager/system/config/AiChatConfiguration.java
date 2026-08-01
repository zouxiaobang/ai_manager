package com.ai.manager.system.config;

import org.springframework.context.annotation.Configuration;

/**
 * AI 对话客户端配置
 *
 * <p>由于提供商的 API Key / Base URL 等配置存储在数据库中，
 * 运行时由 {@link com.ai.manager.system.service.impl.AiKnowledgeServiceImpl}
 * 动态创建 {@link org.springframework.ai.openai.OpenAiChatModel} 实例，
 * 因此此处无需声明静态 @Bean。</p>
 */
@Configuration
public class AiChatConfiguration {
    // 运行时通过 OpenAiApi.builder() + OpenAiChatModel.builder()
    // 动态创建 ChatModel 实例
}
