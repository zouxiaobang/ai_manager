package com.ai.manager.system.service.support.llm;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 提供商策略工厂。
 * <p>自动注入所有 {@link LlmProviderStrategy} Bean，按 provider 名称查找对应策略。</p>
 */
@Component
public class LlmProviderStrategyFactory {

    private final List<LlmProviderStrategy> strategies;

    public LlmProviderStrategyFactory(List<LlmProviderStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 根据提供商名称获取对应的策略
     *
     * @param providerName 提供商名称（如 "openai"、"deepseek"）
     * @return 匹配的策略
     * @throws BusinessException 如果没有策略支持该提供商
     */
    public LlmProviderStrategy getStrategy(String providerName) {
        return strategies.stream()
                .filter(s -> s.supports(providerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.BAD_REQUEST.getCode(),
                        "不支持的 LLM 提供商: " + providerName));
    }
}
