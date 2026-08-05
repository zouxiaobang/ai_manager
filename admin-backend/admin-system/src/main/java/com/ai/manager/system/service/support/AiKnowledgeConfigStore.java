package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 知识库提供商配置存储
 *
 * <p>从 {@code AiKnowledgeServiceImpl} 中提取的模型配置键值读写：把多提供商模型配置
 * 以 JSON 形式持久化到单个 config 槽位（model_config），负责默认配置补全、旧格式迁移
 * 与 API Key 脱敏。服务主类通过本组件读写配置，不再直接接触 model_config 存储细节。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiKnowledgeConfigStore {

    private static final String KEY_MODEL_CONFIG = "model_config";

    private static final Map<String, AiKnowledgeConfigVO> DEFAULT_CONFIGS = buildDefaultConfigs();

    private final AiKnowledgeConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    private final ConfigCryptoService configCrypto;

    /** 某提供商的内置默认配置（未传 apiBaseUrl/embeddingModel 时的补齐来源），无则返回 null */
    public AiKnowledgeConfigVO defaultConfig(String provider) {
        return DEFAULT_CONFIGS.get(provider);
    }

    /** 取提供商默认配置，未知提供商回退到 openai */
    public AiKnowledgeConfigVO defaultForProvider(String provider) {
        return DEFAULT_CONFIGS.getOrDefault(provider, DEFAULT_CONFIGS.get("openai"));
    }

    /**
     * 读取所有提供商的完整配置：DB 缺失的用默认配置补全并写回（含旧格式单对象迁移）。
     */
    public Map<String, AiKnowledgeConfigVO> readAllConfigs() {
        Map<String, AiKnowledgeConfigVO> all = new HashMap<>();

        // 1. 先读数据库中的配置
        AiKnowledgeConfig row = configMapper.selectById(KEY_MODEL_CONFIG);
        // P1-3：已加密值先解密再解析；读到历史明文则标记 legacyPlaintext，下方自动回写加密迁移
        boolean legacyPlaintext = false;
        if (row != null && row.getConfigJson() != null && !row.getConfigJson().isBlank()) {
            String storedJson = row.getConfigJson();
            legacyPlaintext = !configCrypto.isEncrypted(storedJson);
            String json = configCrypto.decryptIfEncrypted(storedJson);
            try {
                all = objectMapper.readValue(json, new TypeReference<Map<String, AiKnowledgeConfigVO>>() {});
            } catch (JsonProcessingException e) {
                // 尝试解析为旧格式（单个 AiKnowledgeConfigVO）
                try {
                    AiKnowledgeConfigVO old = objectMapper.readValue(json, AiKnowledgeConfigVO.class);
                    if (old != null && old.getProvider() != null) {
                        all.put(old.getProvider(), old);
                        log.info("已迁移旧格式配置：provider={}", old.getProvider());
                    }
                } catch (JsonProcessingException ex) {
                    log.error("解析模型配置 JSON 失败，使用默认配置", ex);
                }
            }
        }

        // 2. 合并默认值：DB 中缺失的提供商用默认配置补上
        boolean needSave = false;
        for (Map.Entry<String, AiKnowledgeConfigVO> entry : DEFAULT_CONFIGS.entrySet()) {
            String key = entry.getKey();
            if (!all.containsKey(key)) {
                all.put(key, entry.getValue());
                needSave = true;
            }
        }

        // 3. 如有新增的默认配置、或读到历史明文（需迁移加密），写回 DB
        if (needSave || legacyPlaintext) {
            try {
                String json = configCrypto.encrypt(objectMapper.writeValueAsString(all));
                if (row == null) {
                    row = new AiKnowledgeConfig();
                    row.setConfigKey(KEY_MODEL_CONFIG);
                    row.setConfigJson(json);
                    configMapper.insert(row);
                } else {
                    row.setConfigJson(json);
                    configMapper.updateById(row);
                }
                log.info("已补全所有提供商的默认配置" + (legacyPlaintext ? "，并完成明文迁移加密" : ""));
            } catch (JsonProcessingException e) {
                log.error("写入默认配置失败", e);
            }
        }

        return all;
    }

    /** 将所有提供商的配置写入数据库 */
    public void writeAllConfigs(Map<String, AiKnowledgeConfigVO> configs) {
        try {
            String json = configCrypto.encrypt(objectMapper.writeValueAsString(configs));
            AiKnowledgeConfig row = configMapper.selectById(KEY_MODEL_CONFIG);
            if (row == null) {
                row = new AiKnowledgeConfig();
                row.setConfigKey(KEY_MODEL_CONFIG);
                row.setConfigJson(json);
                configMapper.insert(row);
            } else {
                row.setConfigJson(json);
                configMapper.updateById(row);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置序列化失败");
        }
    }

    /** 保存某个提供商的配置（读全量 → 覆盖 → 写回） */
    public void saveProviderConfig(String provider, AiKnowledgeConfigVO config) {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();
        all.put(provider, config);
        writeAllConfigs(all);
    }

    /** 脱敏 API Key（保留头尾，中间掩码），空值返回占位 */
    public AiKnowledgeConfigVO maskConfig(AiKnowledgeConfigVO config) {
        if (config == null) {
            return null;
        }
        AiKnowledgeConfigVO vo = new AiKnowledgeConfigVO();
        vo.setProvider(config.getProvider());
        vo.setApiKey(maskApiKey(config.getApiKey()));
        vo.setApiBaseUrl(config.getApiBaseUrl());
        vo.setModel(config.getModel());
        vo.setTemperature(config.getTemperature());
        vo.setMaxTokens(config.getMaxTokens());
        vo.setEmbeddingModel(config.getEmbeddingModel());
        vo.setDefaultProvider(config.getDefaultProvider());
        vo.setMaxContextMessages(config.getMaxContextMessages());
        return vo;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, 2) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private static Map<String, AiKnowledgeConfigVO> buildDefaultConfigs() {
        Map<String, AiKnowledgeConfigVO> map = new HashMap<>();
        map.put("openai", buildConfig("openai", "https://api.openai.com/v1", "gpt-4o", 0.7, 4096, "text-embedding-3-small", true, 10));
        map.put("claude", buildConfig("claude", "https://api.anthropic.com", "claude-sonnet-4-20250514", 0.7, 8192, "text-embedding-3-small", false, 10));
        map.put("deepseek", buildConfig("deepseek", "https://api.deepseek.com", "deepseek-chat", 0.7, 8192, "deepseek-embedding", false, 10));
        map.put("qwen", buildConfig("qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo", 0.7, 8192, "text-embedding-v2", false, 10));
        map.put("custom", buildConfig("custom", "", "", 0.7, 4096, "text-embedding-3-small", false, 10));
        return map;
    }

    private static AiKnowledgeConfigVO buildConfig(String provider, String apiBaseUrl, String model,
                                                   double temperature, int maxTokens, String embeddingModel,
                                                   boolean defaultProvider, int maxContextMessages) {
        AiKnowledgeConfigVO vo = new AiKnowledgeConfigVO();
        vo.setProvider(provider);
        vo.setApiKey("");
        vo.setApiBaseUrl(apiBaseUrl);
        vo.setModel(model);
        vo.setTemperature(temperature);
        vo.setMaxTokens(maxTokens);
        vo.setEmbeddingModel(embeddingModel);
        vo.setDefaultProvider(defaultProvider);
        vo.setMaxContextMessages(maxContextMessages);
        return vo;
    }
}
