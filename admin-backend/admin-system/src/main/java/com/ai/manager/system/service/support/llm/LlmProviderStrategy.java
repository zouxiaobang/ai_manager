package com.ai.manager.system.service.support.llm;

import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/**
 * LLM 提供商策略接口。
 * <p>每种 LLM 提供商（OpenAI、DeepSeek、Claude 等）实现该接口，
 * 通过 {@link #supports(String)} 声明自己负责哪些提供商。</p>
 */
public interface LlmProviderStrategy {

    /**
     * 当前策略是否支持指定的提供商
     *
     * @param providerName 提供商名称（如 "openai"、"deepseek"）
     * @return true 表示支持
     */
    boolean supports(String providerName);

    /**
     * 同步调用 LLM 聊天
     *
     * @param config   提供商配置（API Key、模型名、温度等）
     * @param messages Spring AI 消息列表（由 {@link PromptBuilder} 构建）
     * @return 包含回答文本和 Token 用量的结果
     */
    LlmChatResult chat(AiKnowledgeConfigVO config, List<Message> messages);

    /**
     * 流式调用 LLM 聊天，返回文本块 Flux
     *
     * @param config           提供商配置
     * @param messages         Spring AI 消息列表
     * @param tokenCallback    当 API 返回真实 Token 数时的回调（可能不从调用），传 null 表示不需要
     * @return 文本块的 Flux 流，每个元素为一段增量文本
     */
    Flux<String> chatStream(AiKnowledgeConfigVO config, List<Message> messages, Consumer<Long> tokenCallback);

    /**
     * 查询提供商账户余额
     *
     * @param config 提供商配置（需包含 API Key）
     * @return 余额（元），null 表示该提供商不支持余额查询
     */
    BigDecimal queryBalance(AiKnowledgeConfigVO config);
}
