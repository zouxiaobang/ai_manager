package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiKnowledgeConfigStore 单元测试
 * mock configMapper + 真实 ObjectMapper，覆盖默认配置补全写回、旧格式迁移、脱敏与写入分支。
 */
@ExtendWith(MockitoExtension.class)
class AiKnowledgeConfigStoreTest {

    @Mock private AiKnowledgeConfigMapper configMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
    private AiKnowledgeConfigStore store;

    @BeforeEach
    void setUp() {
        // ObjectMapper 与加密服务均非 mock，手动构造（@RequiredArgsConstructor 注入 mapper + objectMapper + configCrypto）
        store = new AiKnowledgeConfigStore(configMapper, objectMapper, crypto);
    }

    private AiKnowledgeConfigVO config(String provider, String apiKey) {
        AiKnowledgeConfigVO vo = new AiKnowledgeConfigVO();
        vo.setProvider(provider);
        vo.setApiKey(apiKey);
        vo.setModel("gpt-4o");
        return vo;
    }

    // ==================== readAllConfigs ====================

    @Test
    void readAllConfigs_shouldInsertDefaultsWhenNoRow() {
        when(configMapper.selectById("model_config")).thenReturn(null);

        Map<String, AiKnowledgeConfigVO> all = store.readAllConfigs();

        // 无任何存量时用内置默认补全五个提供商并写回（P1-3：写回为加密值）
        assertThat(all).containsKeys("openai", "claude", "deepseek", "qwen", "custom");
        ArgumentCaptor<AiKnowledgeConfig> captor = ArgumentCaptor.forClass(AiKnowledgeConfig.class);
        verify(configMapper).insert(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("model_config");
        assertThat(captor.getValue().getConfigJson()).startsWith(ConfigCryptoService.ENC_PREFIX);
        assertThat(crypto.decrypt(captor.getValue().getConfigJson())).contains("\"openai\"");
    }

    @Test
    void readAllConfigs_shouldParseExistingMapAndMergeDefaults() {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigKey("model_config");
        row.setConfigJson("{\"openai\":{\"provider\":\"openai\",\"model\":\"gpt-4o\"}}");
        when(configMapper.selectById("model_config")).thenReturn(row);

        Map<String, AiKnowledgeConfigVO> all = store.readAllConfigs();

        assertThat(all.get("openai").getModel()).isEqualTo("gpt-4o");
        assertThat(all).containsKey("claude");
        // 缺省提供商被补全 → 写回更新
        verify(configMapper).updateById(any(AiKnowledgeConfig.class));
    }

    @Test
    void readAllConfigs_shouldMigrateOldSingleObjectFormat() {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigJson("{\"provider\":\"openai\",\"model\":\"gpt-4o\",\"apiKey\":\"sk-123\"}");
        when(configMapper.selectById("model_config")).thenReturn(row);

        Map<String, AiKnowledgeConfigVO> all = store.readAllConfigs();

        // 旧格式是单对象而非 provider 映射，迁移到 openai 槽位
        assertThat(all).containsKey("openai");
        assertThat(all.get("openai").getProvider()).isEqualTo("openai");
        assertThat(all.get("openai").getApiKey()).isEqualTo("sk-123");
    }

    @Test
    void readAllConfigs_shouldNotWriteBackWhenAllDefaultsPresent() throws Exception {
        // 五个提供商已齐全，无需补全写回
        Map<String, AiKnowledgeConfigVO> complete = Map.of(
                "openai", config("openai", "sk-1"),
                "claude", config("claude", "sk-2"),
                "deepseek", config("deepseek", "sk-3"),
                "qwen", config("qwen", "sk-4"),
                "custom", config("custom", "sk-5"));
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        // 已迁移为加密值（非历史明文），无需迁移加密写回
        row.setConfigJson(crypto.encrypt(objectMapper.writeValueAsString(complete)));
        when(configMapper.selectById("model_config")).thenReturn(row);

        Map<String, AiKnowledgeConfigVO> all = store.readAllConfigs();

        assertThat(all).hasSize(5);
        verify(configMapper, never()).insert(any(AiKnowledgeConfig.class));
        verify(configMapper, never()).updateById(any(AiKnowledgeConfig.class));
    }

    @Test
    void readAllConfigs_历史明文_回写为加密值() {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigKey("model_config");
        row.setConfigJson("{\"openai\":{\"provider\":\"openai\",\"apiKey\":\"sk-123\",\"model\":\"gpt-4o\"}}");
        when(configMapper.selectById("model_config")).thenReturn(row);

        store.readAllConfigs();

        ArgumentCaptor<AiKnowledgeConfig> captor = ArgumentCaptor.forClass(AiKnowledgeConfig.class);
        verify(configMapper).updateById(captor.capture());
        String written = captor.getValue().getConfigJson();
        // 历史明文读取后自动加密回写
        assertThat(written).startsWith(ConfigCryptoService.ENC_PREFIX);
        assertThat(crypto.decrypt(written)).contains("\"sk-123\"");
    }

    // ==================== writeAllConfigs ====================

    @Test
    void writeAllConfigs_shouldInsertWhenNoRow() {
        when(configMapper.selectById("model_config")).thenReturn(null);

        store.writeAllConfigs(Map.of("openai", config("openai", "sk-1")));

        ArgumentCaptor<AiKnowledgeConfig> captor = ArgumentCaptor.forClass(AiKnowledgeConfig.class);
        verify(configMapper).insert(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("model_config");
    }

    @Test
    void writeAllConfigs_shouldUpdateWhenRowExists() {
        when(configMapper.selectById("model_config")).thenReturn(new AiKnowledgeConfig());

        store.writeAllConfigs(Map.of("openai", config("openai", "sk-1")));

        verify(configMapper).updateById(any(AiKnowledgeConfig.class));
        verify(configMapper, never()).insert(any(AiKnowledgeConfig.class));
    }

    // ==================== saveProviderConfig ====================

    @Test
    void saveProviderConfig_shouldMergeIntoAllAndWrite() {
        when(configMapper.selectById("model_config")).thenReturn(null);

        store.saveProviderConfig("custom", config("custom", "sk-c"));

        // readAll 补默认 insert 一次 + writeAllConfigs 再 insert 一次，至少触发一次写回
        verify(configMapper, atLeastOnce()).insert(any(AiKnowledgeConfig.class));
    }

    // ==================== readEmbeddingConfigs / writeEmbeddingConfigs ====================

    @Test
    void readEmbeddingConfigs_无记录返回空映射() {
        when(configMapper.selectById("embedding_config")).thenReturn(null);

        Map<String, AiKnowledgeConfigVO> all = store.readEmbeddingConfigs();

        assertThat(all).isEmpty();
    }

    @Test
    void readEmbeddingConfigs_解析perProvider映射且不补默认() throws Exception {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigKey("embedding_config");
        row.setConfigJson(crypto.encrypt(objectMapper.writeValueAsString(Map.of(
                "openai", config("openai", "sk-1"),
                "qwen", config("qwen", "sk-2")))));
        when(configMapper.selectById("embedding_config")).thenReturn(row);

        Map<String, AiKnowledgeConfigVO> all = store.readEmbeddingConfigs();

        // 仅返回实际保存过的提供商，不补默认（与 model_config 语义不同）
        assertThat(all).containsOnlyKeys("openai", "qwen");
        assertThat(all.get("qwen").getApiKey()).isEqualTo("sk-2");
    }

    @Test
    void readEmbeddingConfigs_旧单对象格式迁移为映射() {
        AiKnowledgeConfig row = new AiKnowledgeConfig();
        row.setConfigKey("embedding_config");
        row.setConfigJson("{\"provider\":\"qwen\",\"apiKey\":\"sk-qwen\"}");
        when(configMapper.selectById("embedding_config")).thenReturn(row);

        Map<String, AiKnowledgeConfigVO> all = store.readEmbeddingConfigs();

        assertThat(all).containsOnlyKeys("qwen");
        assertThat(all.get("qwen").getApiKey()).isEqualTo("sk-qwen");
    }

    @Test
    void writeEmbeddingConfigs_无记录时insert加密值() {
        when(configMapper.selectById("embedding_config")).thenReturn(null);

        store.writeEmbeddingConfigs(Map.of("openai", config("openai", "sk-1")));

        ArgumentCaptor<AiKnowledgeConfig> captor = ArgumentCaptor.forClass(AiKnowledgeConfig.class);
        verify(configMapper).insert(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("embedding_config");
        assertThat(captor.getValue().getConfigJson()).startsWith(ConfigCryptoService.ENC_PREFIX);
    }

    @Test
    void writeEmbeddingConfigs_有记录时update() {
        when(configMapper.selectById("embedding_config")).thenReturn(new AiKnowledgeConfig());

        store.writeEmbeddingConfigs(Map.of("openai", config("openai", "sk-1")));

        verify(configMapper).updateById(any(AiKnowledgeConfig.class));
        verify(configMapper, never()).insert(any(AiKnowledgeConfig.class));
    }

    // ==================== maskConfig ====================

    @Test
    void maskConfig_shouldMaskLongApiKeyKeepingHeadAndTail() {
        AiKnowledgeConfigVO masked = store.maskConfig(config("openai", "sk-abcdef1234567890"));

        assertThat(masked.getApiKey()).isEqualTo("sk****7890");
        assertThat(masked.getProvider()).isEqualTo("openai");
        assertThat(masked.getModel()).isEqualTo("gpt-4o");
    }

    @Test
    void maskConfig_shouldReturnPlaceholderForShortOrEmptyKey() {
        assertThat(store.maskConfig(config("openai", "ab")).getApiKey()).isEqualTo("****");
        assertThat(store.maskConfig(config("openai", null)).getApiKey()).isEqualTo("****");
    }

    @Test
    void maskConfig_shouldReturnNullForNull() {
        assertThat(store.maskConfig(null)).isNull();
    }

    // ==================== defaultConfig ====================

    @Test
    void defaultConfig_shouldReturnBuiltInDefaultForKnownProvider() {
        AiKnowledgeConfigVO cfg = store.defaultConfig("openai");

        assertThat(cfg).isNotNull();
        assertThat(cfg.getApiBaseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(store.defaultConfig("unknown")).isNull();
    }
}
