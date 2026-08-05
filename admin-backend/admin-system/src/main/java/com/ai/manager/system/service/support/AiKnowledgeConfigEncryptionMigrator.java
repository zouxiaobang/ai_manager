package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 存量 ai_knowledge_config 明文迁移（P1-3）
 *
 * <p>启动时把 {@code model_config} / {@code embedding_config} 两个键位的历史明文 JSON 自动加密回写，
 * 保证库中不再残留明文 API Key（不依赖用户再次保存配置）。幂等：已加密值跳过。
 * {@code chat_usage} 等非密钥配置不在此列，保持原样。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiKnowledgeConfigEncryptionMigrator {

    private static final String[] CONFIG_KEYS = {"model_config", "embedding_config"};

    private final AiKnowledgeConfigMapper configMapper;
    private final ConfigCryptoService configCrypto;

    @EventListener(ApplicationReadyEvent.class)
    public void migratePlaintextConfigs() {
        for (String key : CONFIG_KEYS) {
            AiKnowledgeConfig row = configMapper.selectById(key);
            if (row != null && row.getConfigJson() != null && !row.getConfigJson().isBlank()
                    && !configCrypto.isEncrypted(row.getConfigJson())) {
                row.setConfigJson(configCrypto.encrypt(row.getConfigJson()));
                configMapper.updateById(row);
                log.info("已迁移明文配置为加密值：configKey={}", key);
            }
        }
    }
}
