package com.ai.manager.system.service.support.llm;

import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * OpenAI 兼容 API 的策略实现。
 * <p>支持所有使用 OpenAI 协议格式的提供商：openai、deepseek、qwen、custom。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleStrategy implements LlmProviderStrategy {

    /** 本策略支持的提供商名称 */
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("openai", "deepseek", "qwen", "custom");

    private static final String DEEPSEEK_BALANCE_URL = "https://api.deepseek.com/user/balance";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String providerName) {
        return SUPPORTED_PROVIDERS.contains(providerName);
    }

    @Override
    public LlmChatResult chat(AiKnowledgeConfigVO config, List<Message> messages) {
        OpenAiChatModel chatModel = buildChatModel(config);
        ChatResponse response = chatModel.call(new Prompt(messages));

        String text = response.getResult().getOutput().getText();

        // 提取真实 Token 用量
        Long totalTokens = extractTotalTokens(response);

        return new LlmChatResult(text != null ? text : "", totalTokens);
    }

    @Override
    public Flux<String> chatStream(AiKnowledgeConfigVO config, List<Message> messages, Consumer<Long> tokenCallback) {
        OpenAiChatModel chatModel = buildChatModel(config);
        return chatModel.stream(new Prompt(messages))
                .map(chunk -> {
                    // 尝试从每个 chunk 提取 Token 用量（通常仅在最后一个 chunk 有值）
                    if (tokenCallback != null) {
                        Long tokens = extractChunkTokens(chunk);
                        if (tokens != null) {
                            tokenCallback.accept(tokens);
                        }
                    }
                    String content = chunk.getResult().getOutput().getText();
                    return content != null ? content : "";
                })
                .filter(s -> !s.isEmpty());
    }

    @Override
    public BigDecimal queryBalance(AiKnowledgeConfigVO config) {
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return null;
        }
        // 只有 DeepSeek 支持余额查询
        if (!"deepseek".equals(config.getProvider())) {
            return null;
        }
        return queryDeepSeekBalance(config.getApiKey());
    }

    // ==================== 内部方法 ====================

    /**
     * 根据配置构建 OpenAiChatModel
     */
    private OpenAiChatModel buildChatModel(AiKnowledgeConfigVO config) {
        String modelName = config.getModel() != null ? config.getModel() : "unknown";
        String apiKey = config.getApiKey();
        String apiBaseUrl = normalizeBaseUrl(config.getApiBaseUrl());
        double temperature = config.getTemperature() != null ? config.getTemperature() : 0.7;
        int maxTokens = config.getMaxTokens() != null ? config.getMaxTokens() : 4096;

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(apiBaseUrl)
                .apiKey(apiKey)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .build();
    }

    /**
     * 标准化 API Base URL：去除末尾斜杠，空值时默认 DeepSeek
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl;
        }
        return "https://api.deepseek.com";
    }

    /**
     * 从 ChatResponse 中提取总 Token 数
     */
    private Long extractTotalTokens(ChatResponse response) {
        return extractChunkTokens(response);
    }

    /**
     * 从流式 chunk 中提取总 Token 数（可能为 null，大部分 chunk 没有 usage 信息）
     */
    private Long extractChunkTokens(ChatResponse chunk) {
        try {
            Usage usage = chunk.getMetadata().getUsage();
            if (usage != null) {
                Integer total = usage.getTotalTokens();
                if (total == null || total <= 0) {
                    Integer prompt = usage.getPromptTokens();
                    Integer completion = usage.getCompletionTokens();
                    if (prompt != null && completion != null) {
                        total = prompt + completion;
                    }
                }
                return (total != null && total > 0) ? total.longValue() : null;
            }
        } catch (Exception e) {
            // 流式过程中 usage 字段可能未赋值，忽略异常
        }
        return null;
    }

    /**
     * 查询 DeepSeek 账户余额
     */
    private BigDecimal queryDeepSeekBalance(String apiKey) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(DEEPSEEK_BALANCE_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(resp.body());
                // 新格式: {"balance_infos":[{"total_balance":"110.00","currency":"CNY",...}]}
                JsonNode balanceInfos = json.get("balance_infos");
                if (balanceInfos != null && balanceInfos.isArray() && balanceInfos.size() > 0) {
                    JsonNode totalBalance = balanceInfos.get(0).get("total_balance");
                    if (totalBalance != null && totalBalance.isTextual()) {
                        return new BigDecimal(totalBalance.asText());
                    }
                }
                // 旧格式兼容: {"balance": 99.00}
                JsonNode balance = json.get("balance");
                if (balance != null && balance.isNumber()) {
                    return BigDecimal.valueOf(balance.asDouble());
                }
            } else {
                log.warn("DeepSeek 余额查询失败: HTTP {}", resp.statusCode());
            }
        } catch (Exception e) {
            log.warn("DeepSeek 余额查询异常: {}", e.getMessage());
        }
        return null;
    }
}
