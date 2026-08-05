package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiKnowledgeConfigEncryptionMigrator 单测：存量明文开机加密迁移、已加密跳过、空行跳过。
 */
@ExtendWith(MockitoExtension.class)
class AiKnowledgeConfigEncryptionMigratorTest {

    @Mock private AiKnowledgeConfigMapper configMapper;
    private final ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
    private AiKnowledgeConfigEncryptionMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new AiKnowledgeConfigEncryptionMigrator(configMapper, crypto);
    }

    @Test
    void 明文配置_全部加密回写且可解密还原() {
        when(configMapper.selectById("model_config"))
                .thenReturn(row("model_config", "{\"openai\":{\"provider\":\"openai\",\"apiKey\":\"sk-123\"}}"));
        when(configMapper.selectById("embedding_config"))
                .thenReturn(row("embedding_config", "{\"provider\":\"qwen\",\"apiKey\":\"sk-qwen\"}"));

        migrator.migratePlaintextConfigs();

        ArgumentCaptor<AiKnowledgeConfig> captor = ArgumentCaptor.forClass(AiKnowledgeConfig.class);
        verify(configMapper, times(2)).updateById(captor.capture());
        for (AiKnowledgeConfig updated : captor.getAllValues()) {
            assertThat(updated.getConfigJson()).startsWith(ConfigCryptoService.ENC_PREFIX);
        }
        // 解密后可还原为原明文（密钥不丢失）
        assertThat(crypto.decrypt(captor.getAllValues().get(0).getConfigJson())).contains("sk-123");
        assertThat(crypto.decrypt(captor.getAllValues().get(1).getConfigJson())).contains("sk-qwen");
    }

    @Test
    void 已加密配置_跳过() {
        when(configMapper.selectById("model_config"))
                .thenReturn(row("model_config", crypto.encrypt("{\"x\":1}")));
        when(configMapper.selectById("embedding_config"))
                .thenReturn(row("embedding_config", crypto.encrypt("{\"y\":2}")));

        migrator.migratePlaintextConfigs();

        verify(configMapper, never()).updateById(any(AiKnowledgeConfig.class));
    }

    @Test
    void 空或缺失行_跳过() {
        when(configMapper.selectById("model_config")).thenReturn(null);
        when(configMapper.selectById("embedding_config")).thenReturn(row("embedding_config", ""));

        migrator.migratePlaintextConfigs();

        verify(configMapper, never()).updateById(any(AiKnowledgeConfig.class));
    }

    private AiKnowledgeConfig row(String key, String json) {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigKey(key);
        row.setConfigJson(json);
        return row;
    }
}
