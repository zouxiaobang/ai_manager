package com.ai.manager.system.service.support.llm;

import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.domain.vo.AiChatUsageVO;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LLM 用量追踪器。
 * <p>负责读写 {@code ai_knowledge_config} 表中的用量统计数据（chat_usage 键），
 * 包括总请求数、总 Tokens、今日请求数、今日 Tokens、累计消费金额。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageTracker {

    private static final String KEY_CHAT_USAGE = "chat_usage";

    private final AiKnowledgeConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    /**
     * 读取当前用量统计
     */
    public AiChatUsageVO getUsage() {
        AiKnowledgeConfig row = configMapper.selectById(KEY_CHAT_USAGE);
        if (row == null || row.getConfigJson() == null || row.getConfigJson().isBlank()) {
            return new AiChatUsageVO();
        }
        try {
            return objectMapper.readValue(row.getConfigJson(), AiChatUsageVO.class);
        } catch (JsonProcessingException e) {
            log.error("解析用量统计 JSON 失败", e);
            return new AiChatUsageVO();
        }
    }

    /**
     * 记录一次聊天用量
     *
     * @param tokens 本次消耗的 Token 数（可为 null）
     * @param cost   本次消费金额（可为 null）
     */
    public void recordUsage(Long tokens, BigDecimal cost) {
        AiChatUsageVO usage = getUsage();
        String today = LocalDate.now().toString();

        // 日期变更则重置今日计数
        if (!today.equals(usage.getLastDate())) {
            usage.setTodayRequests(0);
            usage.setTodayTokens(0L);
            usage.setLastDate(today);
        }

        usage.setTotalRequests(usage.getTotalRequests() + 1);
        usage.setTodayRequests(usage.getTodayRequests() + 1);

        if (tokens != null) {
            usage.setTotalTokens(usage.getTotalTokens() + tokens);
            usage.setTodayTokens(usage.getTodayTokens() + tokens);
        }
        if (cost != null) {
            usage.setTotalCost(usage.getTotalCost().add(cost));
        }

        saveUsage(usage);
    }

    /**
     * 持久化用量数据
     */
    private void saveUsage(AiChatUsageVO usage) {
        try {
            String json = objectMapper.writeValueAsString(usage);
            AiKnowledgeConfig row = configMapper.selectById(KEY_CHAT_USAGE);
            if (row == null) {
                row = new AiKnowledgeConfig();
                row.setConfigKey(KEY_CHAT_USAGE);
                row.setConfigJson(json);
                configMapper.insert(row);
            } else {
                row.setConfigJson(json);
                configMapper.updateById(row);
            }
        } catch (JsonProcessingException e) {
            log.error("保存用量统计 JSON 失败", e);
        }
    }
}
