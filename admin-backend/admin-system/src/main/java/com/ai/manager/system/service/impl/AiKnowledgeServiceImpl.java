package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.RagProperties;
import com.ai.manager.system.domain.dto.AiChatCategorySaveRequest;
import com.ai.manager.system.domain.dto.AiChatConversationSaveRequest;
import com.ai.manager.system.domain.dto.AiChatUsageRecordRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeChatRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeConfigSaveRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeRagSearchRequest;
import com.ai.manager.system.domain.entity.AiChatCategory;
import com.ai.manager.system.domain.entity.AiChatConversation;
import com.ai.manager.system.domain.entity.AiChatMessage;
import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.domain.entity.RagChunk;
import com.ai.manager.system.domain.entity.RagDocument;
import com.ai.manager.system.domain.vo.AiChatCategoryVO;
import com.ai.manager.system.domain.vo.AiChatConversationVO;
import com.ai.manager.system.domain.vo.AiChatSearchResultVO;
import com.ai.manager.system.domain.vo.AiChatUsageVO;
import com.ai.manager.system.domain.vo.AiKnowledgeChatResponse;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.domain.vo.AiKnowledgeProviderInfoVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagDocumentVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagSearchResultVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagStatsVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagUploadResultVO;
import com.ai.manager.system.mapper.AiChatCategoryMapper;
import com.ai.manager.system.mapper.AiChatConversationMapper;
import com.ai.manager.system.mapper.AiChatMessageMapper;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import com.ai.manager.system.mapper.RagChunkMapper;
import com.ai.manager.system.mapper.RagDocumentMapper;
import com.ai.manager.system.service.AiKnowledgeService;
import com.ai.manager.system.service.support.llm.LlmChatResult;
import com.ai.manager.system.service.support.llm.LlmProviderStrategy;
import com.ai.manager.system.service.support.llm.LlmProviderStrategyFactory;
import com.ai.manager.system.service.support.llm.PromptBuilder;
import com.ai.manager.system.service.support.llm.UsageTracker;
import com.ai.manager.system.service.support.rag.ChunkingConfig;
import com.ai.manager.system.service.support.rag.ChunkingService;
import com.ai.manager.system.service.support.rag.ChunkingService.Chunk;
import com.ai.manager.system.service.support.rag.DocumentParser;
import com.ai.manager.system.service.support.rag.EmbeddingService;
import com.ai.manager.system.service.support.rag.PgVectorStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 知识库服务实现
 *
 * <p>利用 ai_knowledge_config 键值表持久化模型配置，
 * model_config 键存储所有提供商的配置映射（Map&lt;provider, AiKnowledgeConfigVO&gt;）。
 * 智能问答和 RAG 功能当前为桩实现，后续接入 AI 模型后替换。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeServiceImpl implements AiKnowledgeService {

    private static final String KEY_MODEL_CONFIG = "model_config";
    private static final String KEY_EMBEDDING_CONFIG = "embedding_config";

    /** 默认提供商配置（用于新用户首次使用） */
    private static final Map<String, AiKnowledgeConfigVO> DEFAULT_CONFIGS = buildDefaultConfigs();

    /** 各提供商/模型的上下文窗口大小（Token），用于计算当前会话上下文占用百分比 */
    private static final Map<String, Integer> MODEL_CONTEXT_MAP = Map.ofEntries(
            // OpenAI
            Map.entry("gpt-4o", 128000),
            Map.entry("gpt-4o-mini", 128000),
            Map.entry("o1", 200000),
            Map.entry("o3", 200000),
            // DeepSeek
            Map.entry("deepseek-chat", 128000),
            Map.entry("deepseek-reasoner", 128000),
            Map.entry("deepseek-v4-pro", 1000000),
            Map.entry("deepseek-v4-flash", 1000000),
            // Claude
            Map.entry("claude-sonnet-4-20250514", 200000),
            Map.entry("claude-opus-4", 200000),
            Map.entry("claude-haiku-4", 200000),
            // Qwen
            Map.entry("qwen-turbo", 131072),
            Map.entry("qwen-plus", 131072),
            Map.entry("qwen-max", 131072)
    );

    /** 各提供商默认上下文窗口（当模型不在 MODEL_CONTEXT_MAP 中时使用） */
    private static final Map<String, Integer> PROVIDER_DEFAULT_CONTEXT = Map.of(
            "openai", 128000,
            "deepseek", 128000,     // DeepSeek V3.x 系列
            "claude", 200000,
            "qwen", 131072,
            "custom", 8192
    );

    private final AiKnowledgeConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    private final AiChatCategoryMapper chatCategoryMapper;
    private final AiChatConversationMapper chatConversationMapper;
    private final AiChatMessageMapper chatMessageMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final LlmProviderStrategyFactory strategyFactory;
    private final PromptBuilder promptBuilder;
    private final UsageTracker usageTracker;
    private final DocumentParser documentParser;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final PgVectorStore pgVectorStore;
    private final RagProperties ragProperties;

    // ==================== 模型配置 ====================

    @Override
    public AiKnowledgeConfigVO getConfig() {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();

        // 优先返回标记为默认的提供商
        for (Map.Entry<String, AiKnowledgeConfigVO> entry : all.entrySet()) {
            AiKnowledgeConfigVO c = entry.getValue();
            if (Boolean.TRUE.equals(c.getDefaultProvider())) {
                return maskConfig(c);
            }
        }

        // 其次返回有 API Key 的
        for (Map.Entry<String, AiKnowledgeConfigVO> entry : all.entrySet()) {
            AiKnowledgeConfigVO c = entry.getValue();
            if (c.getApiKey() != null && !c.getApiKey().isBlank()) {
                return maskConfig(c);
            }
        }
        // 都不算有 key，返回第一个
        AiKnowledgeConfigVO first = all.values().iterator().next();
        return maskConfig(first);
    }

    @Override
    public List<AiKnowledgeProviderInfoVO> getProviders() {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();
        List<AiKnowledgeProviderInfoVO> list = new ArrayList<>();
        for (Map.Entry<String, AiKnowledgeConfigVO> entry : all.entrySet()) {
            AiKnowledgeConfigVO config = entry.getValue();
            AiKnowledgeProviderInfoVO info = new AiKnowledgeProviderInfoVO();
            info.setProvider(entry.getKey());
            info.setModel(config.getModel() != null ? config.getModel() : "");
            info.setConfigured(config.getApiKey() != null && !config.getApiKey().isBlank());
            info.setDefaultProvider(Boolean.TRUE.equals(config.getDefaultProvider()));
            info.setMaxContextMessages(config.getMaxContextMessages());
            // 优先按模型名查找，其次按提供商默认值
            String modelName = config.getModel();
            Integer ctx = modelName != null ? MODEL_CONTEXT_MAP.get(modelName) : null;
            if (ctx == null) {
                ctx = PROVIDER_DEFAULT_CONTEXT.getOrDefault(entry.getKey(), 4096);
            }
            info.setMaxContextTokens(ctx);
            list.add(info);
        }
        return list;
    }

    @Override
    public AiKnowledgeConfigVO saveConfig(AiKnowledgeConfigSaveRequest request) {
        String provider = request.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提供商不能为空");
        }

        // 读取现有所有配置
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();

        // 构建完整配置
        AiKnowledgeConfigVO config = new AiKnowledgeConfigVO();
        config.setProvider(provider);
        // 如果 API Key 是脱敏格式（包含 ****），则保留已存储的原始 key
        if (request.getApiKey() != null && request.getApiKey().contains("****")) {
            AiKnowledgeConfigVO existing = all.get(provider);
            if (existing != null && existing.getApiKey() != null && !existing.getApiKey().isBlank()
                    && !existing.getApiKey().contains("****")) {
                config.setApiKey(existing.getApiKey());
            } else {
                config.setApiKey(request.getApiKey());
            }
        } else {
            config.setApiKey(request.getApiKey());
        }
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setModel(request.getModel());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        config.setEmbeddingModel(request.getEmbeddingModel());
        config.setDefaultProvider(request.getDefaultProvider());
        config.setMaxContextMessages(request.getMaxContextMessages());

        // 如果前端未传 apiBaseUrl 或 embeddingModel，用该提供商的默认值补齐
        AiKnowledgeConfigVO defaults = DEFAULT_CONFIGS.get(provider);
        if (defaults != null) {
            if (config.getApiBaseUrl() == null || config.getApiBaseUrl().isBlank()) {
                config.setApiBaseUrl(defaults.getApiBaseUrl());
            }
            if (config.getEmbeddingModel() == null || config.getEmbeddingModel().isBlank()) {
                config.setEmbeddingModel(defaults.getEmbeddingModel());
            }
        }

        // 保存到该提供商的配置槽位
        all.put(provider, config);

        // 如果标记为默认，取消其他提供商的默认标记
        if (Boolean.TRUE.equals(request.getDefaultProvider())) {
            for (Map.Entry<String, AiKnowledgeConfigVO> e : all.entrySet()) {
                if (!e.getKey().equals(provider) && Boolean.TRUE.equals(e.getValue().getDefaultProvider())) {
                    e.getValue().setDefaultProvider(false);
                }
            }
        }

        writeAllConfigs(all);

        log.info("AI 知识库配置已保存：provider={}, model={}, defaultProvider={}",
                provider, request.getModel(), request.getDefaultProvider());
        return maskConfig(config);
    }

    // ==================== Embedding 配置（独立于 Chat 配置） ====================

    @Override
    public AiKnowledgeConfigVO getEmbeddingConfig() {
        AiKnowledgeConfig row = configMapper.selectById(KEY_EMBEDDING_CONFIG);
        if (row != null && row.getConfigJson() != null && !row.getConfigJson().isBlank()) {
            try {
                AiKnowledgeConfigVO config = objectMapper.readValue(row.getConfigJson(), AiKnowledgeConfigVO.class);
                return maskConfig(config);
            } catch (JsonProcessingException e) {
                log.error("解析 embedding 配置失败", e);
            }
        }
        // 无独立 embedding 配置时，返回当前默认 chat 配置（masked）
        AiKnowledgeConfigVO fallback = resolveDefaultConfig();
        return fallback != null ? maskConfig(fallback) : null;
    }

    @Override
    public void saveEmbeddingConfig(AiKnowledgeConfigSaveRequest request) {
        String provider = request.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "提供商不能为空");
        }

        // 读取所有提供商的完整配置，用于补充默认值
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();

        AiKnowledgeConfigVO config = new AiKnowledgeConfigVO();
        config.setProvider(provider);

        // API Key 脱敏处理
        if (request.getApiKey() != null && request.getApiKey().contains("****")) {
            AiKnowledgeConfig existingRow = configMapper.selectById(KEY_EMBEDDING_CONFIG);
            if (existingRow != null && existingRow.getConfigJson() != null) {
                try {
                    AiKnowledgeConfigVO existing = objectMapper.readValue(existingRow.getConfigJson(), AiKnowledgeConfigVO.class);
                    if (existing.getApiKey() != null && !existing.getApiKey().isBlank()
                            && !existing.getApiKey().contains("****")) {
                        config.setApiKey(existing.getApiKey());
                    } else {
                        config.setApiKey(request.getApiKey());
                    }
                } catch (JsonProcessingException e) {
                    config.setApiKey(request.getApiKey());
                }
            } else {
                config.setApiKey(request.getApiKey());
            }
        } else {
            config.setApiKey(request.getApiKey());
        }

        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setModel(request.getModel());
        config.setEmbeddingModel(request.getEmbeddingModel());

        // 默认值补齐
        AiKnowledgeConfigVO defaults = DEFAULT_CONFIGS.get(provider);
        if (defaults != null) {
            if (config.getApiBaseUrl() == null || config.getApiBaseUrl().isBlank()) {
                config.setApiBaseUrl(defaults.getApiBaseUrl());
            }
            if (config.getEmbeddingModel() == null || config.getEmbeddingModel().isBlank()) {
                config.setEmbeddingModel(defaults.getEmbeddingModel());
            }
            if (config.getModel() == null || config.getModel().isBlank()) {
                config.setModel(defaults.getModel());
            }
        }

        // 保存到 DB
        try {
            String json = objectMapper.writeValueAsString(config);
            AiKnowledgeConfig row = configMapper.selectById(KEY_EMBEDDING_CONFIG);
            if (row == null) {
                row = new AiKnowledgeConfig();
                row.setConfigKey(KEY_EMBEDDING_CONFIG);
                row.setConfigJson(json);
                configMapper.insert(row);
            } else {
                row.setConfigJson(json);
                configMapper.updateById(row);
            }
            log.info("Embedding 配置已保存：provider={}, embeddingModel={}", provider, config.getEmbeddingModel());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Embedding 配置序列化失败");
        }
    }

    /**
     * 获取实际使用的 Embedding 配置（内部使用，不脱敏）
     * 优先使用独立配置的 embedding 提供商，fallback 到默认 chat 配置
     */
    private AiKnowledgeConfigVO resolveEmbeddingConfig() {
        AiKnowledgeConfig row = configMapper.selectById(KEY_EMBEDDING_CONFIG);
        if (row != null && row.getConfigJson() != null && !row.getConfigJson().isBlank()) {
            try {
                return objectMapper.readValue(row.getConfigJson(), AiKnowledgeConfigVO.class);
            } catch (JsonProcessingException e) {
                log.error("解析 embedding 配置失败，使用默认配置", e);
            }
        }
        return resolveDefaultConfig();
    }

    // ==================== 智能问答 ====================

    @Override
    public AiKnowledgeChatResponse chat(AiKnowledgeChatRequest request) {
        String question = request.getQuestion();
        String providerName = request.getProvider();

        log.info("AI 问答：provider={}, question={}, useRag={}", providerName, question, request.getUseRag());

        // 解析提供商配置
        AiKnowledgeConfigVO config = resolveProviderConfig(providerName);
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先在「配置」标签页填写 API Key");
        }

        // RAG 模式：检索知识库并注入上下文
        List<AiKnowledgeChatResponse.RagSourceItem> sources = Collections.emptyList();
        List<org.springframework.ai.chat.messages.Message> messages;

        if (Boolean.TRUE.equals(request.getUseRag())) {
            AiKnowledgeRagSearchRequest ragReq = new AiKnowledgeRagSearchRequest();
            ragReq.setQuery(question);
            ragReq.setTopK(ragProperties.getSearch().getTopK());
            AiKnowledgeRagSearchResultVO ragResult = searchRag(ragReq);
            sources = ragResult.getSources();

            // 构建 RAG 上下文
            String context = buildRagContext(sources);
            messages = promptBuilder.buildRagMessages(request, context);
        } else {
            messages = promptBuilder.buildMessages(request);
        }

        // 通过策略工厂调用对应 LLM 提供商
        LlmProviderStrategy strategy = strategyFactory.getStrategy(config.getProvider());
        LlmChatResult result = strategy.chat(config, messages);

        // 自动记录 Token 用量（API 未返回则用文本长度估算）
        Long tokens = result.getTotalTokens();
        if (tokens == null || tokens <= 0) {
            tokens = Math.max(1, (long) Math.ceil(result.getText().length() * 0.4));
        }
        usageTracker.recordUsage(tokens, BigDecimal.ZERO);

        log.info("AI 问答完成：provider={}, model={}, tokens={}, ragSources={}",
                config.getProvider(), config.getModel(), result.getTotalTokens(), sources.size());

        AiKnowledgeChatResponse response = new AiKnowledgeChatResponse();
        response.setAnswer(result.getText());
        response.setSources(sources);
        response.setTotalTokens(tokens);
        return response;
    }

    /**
     * 将 RAG 搜索结果格式化为 Prompt 上下文
     */
    private String buildRagContext(List<AiKnowledgeChatResponse.RagSourceItem> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            AiKnowledgeChatResponse.RagSourceItem src = sources.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append("来源：").append(src.getFileName());
            if (src.getScore() != null) {
                sb.append("（相关度：").append(String.format("%.0f%%", src.getScore() * 100)).append("）");
            }
            sb.append("\n").append(src.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    // ==================== 流式智能问答 ====================

    @Override
    public void chatStream(AiKnowledgeChatRequest request, SseEmitter emitter) {
        String question = request.getQuestion();
        String providerName = request.getProvider();

        log.info("AI 流式问答：provider={}, question={}, useRag={}", providerName, question, request.getUseRag());

        AiKnowledgeConfigVO config = resolveProviderConfig(providerName);
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            try {
                emitter.send("请先在「配置」标签页填写 API Key");
            } catch (Exception e) {
                // SSE 通道发送失败通常是客户端已断开，记录告警而不是吞掉
                log.warn("SSE 发送配置提示失败（客户端可能已断开）", e);
            }
            emitter.complete();
            return;
        }

        LlmProviderStrategy strategy = strategyFactory.getStrategy(config.getProvider());
        var messages = promptBuilder.buildMessages(request);

        StringBuilder fullResponse = new StringBuilder();
        // 记录 API 返回的精确 Token 数（可能为 0）
        long[] realTokens = new long[]{0};

        strategy.chatStream(config, messages, tokens -> realTokens[0] = tokens)
                .subscribe(
                    chunk -> {
                        fullResponse.append(chunk);
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    error -> {
                        log.error("AI 流式调用失败：provider={}, model={}", config.getProvider(), config.getModel(), error);
                        try {
                            emitter.send("[ERROR] " + error.getMessage());
                        } catch (Exception e) {
                            log.warn("SSE 发送错误信息失败（客户端可能已断开）", e);
                        }
                        emitter.complete();
                    },
                    () -> {
                        try {
                            // 优先使用 API 返回的精确 Token 数，否则用文本长度估算
                            long tokens;
                            if (realTokens[0] > 0) {
                                tokens = realTokens[0];
                                log.debug("流式 Token（精确）：{}", tokens);
                            } else {
                                tokens = Math.max(1, (long) Math.ceil(fullResponse.length() * 0.4));
                                log.debug("流式 Token（估算）：{}", tokens);
                            }
                            // 发送 token 事件给前端（放在 [DONE] 之前）
                            emitter.send("[TOKENS] " + tokens);
                            emitter.send("[DONE]");
                            usageTracker.recordUsage(tokens, BigDecimal.ZERO);
                        } catch (Exception e) {
                            log.warn("SSE 发送完成事件或记录用量失败", e);
                        }
                        emitter.complete();
                    }
                );
    }

    private AiKnowledgeConfigVO resolveProviderConfig(String provider) {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();
        if (provider != null && !provider.isBlank()) {
            AiKnowledgeConfigVO c = all.get(provider);
            if (c != null) return c;
        }
        for (Map.Entry<String, AiKnowledgeConfigVO> e : all.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue().getDefaultProvider())) return e.getValue();
        }
        for (Map.Entry<String, AiKnowledgeConfigVO> e : all.entrySet()) {
            if (e.getValue().getApiKey() != null && !e.getValue().getApiKey().isBlank()) return e.getValue();
        }
        return all.isEmpty() ? null : all.values().iterator().next();
    }

    // ==================== RAG 知识库 ====================

    @Override
    public AiKnowledgeRagStatsVO getRagStats() {
        AiKnowledgeRagStatsVO stats = new AiKnowledgeRagStatsVO();
        stats.setTotalDocs(ragDocumentMapper.selectCount(null));
        stats.setReadyCount(ragDocumentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagDocument>()
                        .eq(RagDocument::getStatus, "ready")));
        stats.setProcessingCount(ragDocumentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagDocument>()
                        .eq(RagDocument::getStatus, "processing")));
        stats.setFailedCount(ragDocumentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagDocument>()
                        .eq(RagDocument::getStatus, "failed")));
        stats.setTotalChunks(ragChunkMapper.selectCount(null));
        return stats;
    }

    @Override
    public List<AiKnowledgeRagDocumentVO> listRagDocuments() {
        List<RagDocument> docs = ragDocumentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagDocument>()
                        .orderByDesc(RagDocument::getCreatedAt));
        List<AiKnowledgeRagDocumentVO> result = new ArrayList<>();
        for (RagDocument doc : docs) {
            AiKnowledgeRagDocumentVO vo = new AiKnowledgeRagDocumentVO();
            vo.setId(doc.getId());
            vo.setFileName(doc.getFileName());
            vo.setFileType(doc.getFileType());
            vo.setFileSize(doc.getFileSize());
            vo.setChunkCount(doc.getChunkCount());
            vo.setStatus(doc.getStatus());
            vo.setIndexedAt(doc.getIndexedAt());
            vo.setErrorMessage(doc.getErrorMessage());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeRagUploadResultVO uploadRagDocument(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }

        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalName.substring(dotIdx + 1).toLowerCase();
        }

        // 校验文件类型
        var supportedTypes = Set.of("pdf", "txt", "md", "html", "htm", "docx");
        if (!supportedTypes.contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "不支持的文件类型: " + ext + "，支持: " + String.join(", ", supportedTypes));
        }

        try {
            // 1. 存储文件到磁盘
            String uploadDir = ragProperties.getUploadPath();
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String storageName = System.currentTimeMillis() + "_" + originalName;
            File targetFile = new File(dir, storageName);
            file.transferTo(targetFile);

            // 2. 创建文档记录
            RagDocument doc = new RagDocument();
            doc.setFileName(originalName);
            doc.setFileType(ext);
            doc.setFileSize(file.getSize());
            doc.setFilePath(targetFile.getAbsolutePath());
            doc.setStatus("pending");
            doc.setChunkCount(0);
            ragDocumentMapper.insert(doc);

            // 3. 异步处理文档（解析 → 分块 → 嵌入 → 存储）
            final Long docId = doc.getId();
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            uw.eq(RagDocument::getId, docId).set(RagDocument::getStatus, "processing");
            ragDocumentMapper.update(null, uw);

            try {
                processDocument(docId, targetFile, ext);
            } catch (Exception e) {
                log.error("文档处理失败：docId={}, fileName={}", docId, originalName, e);
                ragDocumentMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument>()
                                .eq(RagDocument::getId, docId)
                                .set(RagDocument::getStatus, "failed")
                                .set(RagDocument::getErrorMessage, e.getMessage()));
            }

            return AiKnowledgeRagUploadResultVO.builder()
                    .documentId(docId)
                    .fileName(originalName)
                    .status("processing")
                    .message("文档已上传，后台处理中")
                    .build();

        } catch (Exception e) {
            log.error("文档上传失败：{}", originalName, e);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 处理文档：解析 → 分块 → 嵌入 → 存储向量
     */
    private void processDocument(Long docId, File file, String ext) {
        // 1. 解析文档为纯文本
        String text;
        try (InputStream is = new FileInputStream(file)) {
            text = documentParser.parse(is, ext);
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }

        if (text == null || text.isBlank()) {
            throw new RuntimeException("文档内容为空，无法处理");
        }

        // 2. 分块
        ChunkingConfig chunkConfig = ChunkingConfig.builder()
                .maxChunkSize(ragProperties.getChunk().getMaxSize())
                .chunkOverlap(ragProperties.getChunk().getOverlap())
                .strategy(ChunkingConfig.ChunkingStrategy.valueOf(
                        ragProperties.getChunk().getStrategy().toUpperCase()))
                .build();

        var chunks = chunkingService.chunk(text, chunkConfig);

        // 3. 获取嵌入向量对应的提供商配置（使用独立的 embedding 配置）
        AiKnowledgeConfigVO embedConfig = resolveEmbeddingConfig();
        if (embedConfig == null || embedConfig.getApiKey() == null || embedConfig.getApiKey().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先配置 API Key 以生成嵌入向量");
        }

        // 4. 批量生成嵌入向量
        List<String> chunkTexts = chunks.stream()
                .map(ChunkingService.Chunk::getContent)
                .collect(java.util.stream.Collectors.toList());

        List<float[]> embeddings = embeddingService.embed(chunkTexts, embedConfig);

        // 5. 保存分块记录和向量
        List<PgVectorStore.VectorRecord> vectorRecords = new ArrayList<>();
        int tokenCount = 0;

        for (int i = 0; i < chunks.size(); i++) {
            ChunkingService.Chunk chunk = chunks.get(i);

            RagChunk ragChunk = new RagChunk();
            ragChunk.setDocumentId(docId);
            ragChunk.setChunkIndex(chunk.getChunkIndex());
            ragChunk.setContent(chunk.getContent());
            ragChunk.setTokenCount(chunk.getTokenCount());
            ragChunkMapper.insert(ragChunk);

            tokenCount += chunk.getTokenCount();

            // 收集向量记录
            if (i < embeddings.size()) {
                vectorRecords.add(PgVectorStore.VectorRecord.builder()
                        .chunkId(ragChunk.getId())
                        .docId(docId)
                        .embedding(embeddings.get(i))
                        .content(chunk.getContent())
                        .build());
            }
        }

        // 6. 批量存储向量
        if (!vectorRecords.isEmpty()) {
            pgVectorStore.storeBatch(vectorRecords);
        }

        // 7. 更新文档状态
        ragDocumentMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument>()
                        .eq(RagDocument::getId, docId)
                        .set(RagDocument::getStatus, "ready")
                        .set(RagDocument::getChunkCount, chunks.size())
                        .set(RagDocument::getIndexedAt, java.time.LocalDateTime.now()));

        log.info("文档处理完成：docId={}, chunks={}, tokens={}", docId, chunks.size(), tokenCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryRagDocument(Long id) {
        RagDocument doc = ragDocumentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文档不存在");
        }

        // 清除旧的分块和向量
        List<RagChunk> oldChunks = ragChunkMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChunk>()
                        .eq(RagChunk::getDocumentId, id));
        if (!oldChunks.isEmpty()) {
            List<Long> chunkIds = oldChunks.stream().map(RagChunk::getId).collect(java.util.stream.Collectors.toList());
            pgVectorStore.deleteByChunkIds(chunkIds);
            ragChunkMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChunk>()
                            .eq(RagChunk::getDocumentId, id));
        }

        // 重新处理
        try {
            ragDocumentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument>()
                            .eq(RagDocument::getId, id)
                            .set(RagDocument::getStatus, "processing")
                            .set(RagDocument::getErrorMessage, null));
            File file = new File(doc.getFilePath());
            processDocument(id, file, doc.getFileType());
        } catch (Exception e) {
            log.error("重试文档失败：id={}", id, e);
            ragDocumentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument>()
                            .eq(RagDocument::getId, id)
                            .set(RagDocument::getStatus, "failed")
                            .set(RagDocument::getErrorMessage, e.getMessage()));
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重试失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRagDocument(Long id) {
        // 删除向量
        pgVectorStore.deleteByDocId(id);
        // 删除分块
        ragChunkMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChunk>()
                        .eq(RagChunk::getDocumentId, id));
        // 删除文档记录
        RagDocument doc = ragDocumentMapper.selectById(id);
        if (doc != null) {
            // 删除物理文件
            File file = new File(doc.getFilePath());
            if (file.exists()) file.delete();
        }
        ragDocumentMapper.deleteById(id);
        log.info("移除文档：id={}", id);
    }

    @Override
    public AiKnowledgeRagSearchResultVO searchRag(AiKnowledgeRagSearchRequest request) {
        AiKnowledgeRagSearchResultVO result = new AiKnowledgeRagSearchResultVO();
        result.setSources(Collections.emptyList());

        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            return result;
        }

        int topK = request.getTopK() != null && request.getTopK() > 0
                ? request.getTopK() : ragProperties.getSearch().getTopK();
        double threshold = ragProperties.getSearch().getSimilarityThreshold();

        // 1. 获取嵌入配置（使用独立的 embedding 配置）
        AiKnowledgeConfigVO embedConfig = resolveEmbeddingConfig();
        if (embedConfig == null || embedConfig.getApiKey() == null || embedConfig.getApiKey().isBlank()) {
            log.warn("搜索 RAG：未配置 API Key，无法生成查询向量");
            return result;
        }

        try {
            // 2. 生成查询向量
            float[] queryEmbedding = embeddingService.embedQuery(query, embedConfig);
            if (queryEmbedding.length == 0) {
                return result;
            }

            // 3. 向量相似度搜索
            List<PgVectorStore.SearchResult> searchResults =
                    pgVectorStore.similaritySearch(queryEmbedding, topK, threshold);

            // 4. 组装结果（关联文档信息）
            List<AiKnowledgeChatResponse.RagSourceItem> sources = new ArrayList<>();
            for (PgVectorStore.SearchResult sr : searchResults) {
                RagDocument doc = ragDocumentMapper.selectById(sr.getDocId());
                AiKnowledgeChatResponse.RagSourceItem item = new AiKnowledgeChatResponse.RagSourceItem();
                item.setDocumentId(sr.getDocId());
                item.setFileName(doc != null ? doc.getFileName() : "未知文档");
                item.setChunkIndex(null); // 需要查询 chunk 表获取 index
                item.setContent(sr.getContent());
                item.setScore(sr.getScore());
                sources.add(item);
            }

            result.setSources(sources);
            log.info("RAG 搜索完成：query={}, topK={}, 结果数={}", query, topK, sources.size());
        } catch (Exception e) {
            log.error("RAG 搜索失败：query={}", query, e);
        }

        return result;
    }

    @Override
    public void rebuildRagIndex() {
        List<RagDocument> docs = ragDocumentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagDocument>()
                        .eq(RagDocument::getStatus, "ready")
                        .or().eq(RagDocument::getStatus, "failed"));

        if (docs.isEmpty()) {
            log.info("重建索引：没有需要重建的文档");
            return;
        }

        log.info("重建索引开始：{} 个文档", docs.size());

        // 清空向量库
        pgVectorStore.truncateAll();

        for (RagDocument doc : docs) {
            try {
                // 删除旧分块
                ragChunkMapper.delete(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagChunk>()
                                .eq(RagChunk::getDocumentId, doc.getId()));
                // 重新处理
                File file = new File(doc.getFilePath());
                if (file.exists()) {
                    processDocument(doc.getId(), file, doc.getFileType());
                }
            } catch (Exception e) {
                log.error("重建索引失败：docId={}, fileName={}", doc.getId(), doc.getFileName(), e);
                ragDocumentMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagDocument>()
                                .eq(RagDocument::getId, doc.getId())
                                .set(RagDocument::getStatus, "failed")
                                .set(RagDocument::getErrorMessage, e.getMessage()));
            }
        }

        log.info("重建索引完成：{} 个文档", docs.size());
    }

    // ==================== 对话管理 ====================

    @Override
    public List<AiChatCategoryVO> getChatCategories() {
        List<AiChatCategory> cats = chatCategoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatCategory>()
                        .eq(AiChatCategory::getDeleted, 0)
                        .orderByAsc(AiChatCategory::getSortOrder));
        List<AiChatCategoryVO> result = new ArrayList<>();
        for (AiChatCategory cat : cats) {
            AiChatCategoryVO vo = new AiChatCategoryVO();
            vo.setId(cat.getId());
            vo.setName(cat.getName());
            List<AiChatConversation> convs = chatConversationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatConversation>()
                            .eq(AiChatConversation::getCategoryId, cat.getId())
                            .eq(AiChatConversation::getDeleted, 0)
                            .orderByAsc(AiChatConversation::getSortOrder));
            List<AiChatConversationVO> convVOs = convs.stream().map(c -> {
                AiChatConversationVO v = new AiChatConversationVO();
                v.setId(c.getId());
                v.setCategoryId(c.getCategoryId());
                v.setTitle(c.getTitle());
                // 从 ai_chat_message 表加载消息
                v.setMessages(loadMessagesJson(c.getId()));
                v.setCreatedAt(c.getCreateTime());
                v.setUpdatedAt(c.getUpdateTime());
                return v;
            }).collect(Collectors.toList());
            vo.setConversations(convVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatCategoryVO createChatCategory(AiChatCategorySaveRequest request) {
        AiChatCategory entity = new AiChatCategory();
        entity.setName(request.getName());
        entity.setSortOrder(0);
        chatCategoryMapper.insert(entity);
        AiChatCategoryVO vo = new AiChatCategoryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setConversations(Collections.emptyList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameChatCategory(Long id, AiChatCategorySaveRequest request) {
        AiChatCategory entity = chatCategoryMapper.selectById(id);
        if (entity == null) {
            throw new com.ai.manager.common.exception.BusinessException(
                    com.ai.manager.common.result.ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        entity.setName(request.getName());
        chatCategoryMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatCategory(Long id) {
        // 逻辑删除分类
        chatCategoryMapper.deleteById(id);
        // 级联逻辑删除该分类下的所有对话
        chatConversationMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatConversation>()
                        .eq(AiChatConversation::getCategoryId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatConversationVO createChatConversation(Long categoryId) {
        AiChatCategory cat = chatCategoryMapper.selectById(categoryId);
        if (cat == null) {
            throw new com.ai.manager.common.exception.BusinessException(
                    com.ai.manager.common.result.ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        AiChatConversation entity = new AiChatConversation();
        entity.setCategoryId(categoryId);
        entity.setTitle("");
        entity.setSortOrder(0);
        chatConversationMapper.insert(entity);
        AiChatConversationVO vo = new AiChatConversationVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setTitle(entity.getTitle());
        vo.setMessages("[]");
        vo.setCreatedAt(entity.getCreateTime());
        vo.setUpdatedAt(entity.getUpdateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChatConversation(Long id, AiChatConversationSaveRequest request) {
        AiChatConversation entity = chatConversationMapper.selectById(id);
        if (entity == null) {
            throw new com.ai.manager.common.exception.BusinessException(
                    com.ai.manager.common.result.ResultCode.NOT_FOUND.getCode(), "对话不存在");
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getMessages() != null) {
            // 保存消息到 ai_chat_message 表
            saveMessagesJson(id, request.getMessages());
        }
        chatConversationMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatConversation(Long id) {
        chatConversationMapper.deleteById(id);
        // 级联删除该对话的消息
        chatMessageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getConversationId, id));
    }

    @Override
    public List<AiChatSearchResultVO> searchChatConversations(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String like = "%" + keyword.trim() + "%";

        // key = conversationId, value = result (dedup by conversation)
        java.util.LinkedHashMap<Long, AiChatSearchResultVO> resultMap = new java.util.LinkedHashMap<>();

        // 1. 按分类名称匹配
        List<AiChatCategory> matchedCats = chatCategoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatCategory>()
                        .eq(AiChatCategory::getDeleted, 0)
                        .like(AiChatCategory::getName, keyword.trim()));
        for (AiChatCategory cat : matchedCats) {
            // 找到该分类下的所有对话
            List<AiChatConversation> convs = chatConversationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatConversation>()
                            .eq(AiChatConversation::getCategoryId, cat.getId())
                            .eq(AiChatConversation::getDeleted, 0));
            for (AiChatConversation conv : convs) {
                AiChatSearchResultVO r = new AiChatSearchResultVO();
                r.setCategoryId(cat.getId());
                r.setCategoryName(cat.getName());
                r.setConversationId(conv.getId());
                r.setConversationTitle(conv.getTitle());
                r.setMatchField("categoryName");
                r.setMatchSummary(cat.getName());
                resultMap.putIfAbsent(conv.getId(), r);
            }
        }

        // 2. 按对话标题匹配
        List<AiChatConversation> matchedByTitle = chatConversationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatConversation>()
                        .eq(AiChatConversation::getDeleted, 0)
                        .like(AiChatConversation::getTitle, keyword.trim()));
        for (AiChatConversation conv : matchedByTitle) {
            if (resultMap.containsKey(conv.getId())) continue;
            // 获取分类名
            AiChatCategory cat = chatCategoryMapper.selectById(conv.getCategoryId());
            AiChatSearchResultVO r = new AiChatSearchResultVO();
            r.setCategoryId(conv.getCategoryId());
            r.setCategoryName(cat != null ? cat.getName() : "");
            r.setConversationId(conv.getId());
            r.setConversationTitle(conv.getTitle());
            r.setMatchField("conversationTitle");
            r.setMatchSummary(conv.getTitle());
            resultMap.putIfAbsent(conv.getId(), r);
        }

        // 3. 按消息内容匹配
        List<AiChatMessage> matchedMsgs = chatMessageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatMessage>()
                        .like(AiChatMessage::getContent, keyword.trim()));
        for (AiChatMessage msg : matchedMsgs) {
            Long convId = msg.getConversationId();
            if (resultMap.containsKey(convId)) continue;
            AiChatConversation conv = chatConversationMapper.selectById(convId);
            if (conv == null || conv.getDeleted() != 0) continue;
            AiChatCategory cat = chatCategoryMapper.selectById(conv.getCategoryId());
            if (cat == null || cat.getDeleted() != 0) continue;
            AiChatSearchResultVO r = new AiChatSearchResultVO();
            r.setCategoryId(cat.getId());
            r.setCategoryName(cat.getName());
            r.setConversationId(conv.getId());
            r.setConversationTitle(conv.getTitle());
            r.setMatchField("messageContent");
            // 截取匹配内容摘要
            String content = msg.getContent();
            int kwIdx = content.indexOf(keyword.trim());
            if (kwIdx > 0) {
                int start = Math.max(0, kwIdx - 20);
                int end = Math.min(content.length(), kwIdx + keyword.trim().length() + 30);
                r.setMatchSummary((start > 0 ? "..." : "") + content.substring(start, end) + (end < content.length() ? "..." : ""));
            } else {
                r.setMatchSummary(content.length() > 60 ? content.substring(0, 60) + "..." : content);
            }
            resultMap.putIfAbsent(convId, r);
        }

        return new ArrayList<>(resultMap.values());
    }

    // ==================== 消息存储（分表） ====================

    /**
     * 从 ai_chat_message 表读取消息，组装成 ChatMessage JSON 数组
     */
    private String loadMessagesJson(Long conversationId) {
        List<AiChatMessage> msgs = chatMessageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getConversationId, conversationId)
                        .orderByAsc(AiChatMessage::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiChatMessage msg : msgs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", msg.getMsgId());
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            m.put("timestamp", msg.getCreateTime() != null
                    ? msg.getCreateTime().toEpochSecond(java.time.ZoneOffset.ofHours(8)) * 1000
                    : 0);
            result.add(m);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("序列化消息 JSON 失败：conversationId={}", conversationId, e);
            return "[]";
        }
    }

    /**
     * 将前端传来的 messages JSON 保存到 ai_chat_message 表
     * 先删除旧消息，再批量插入新消息
     */
    private void saveMessagesJson(Long conversationId, String messagesJson) {
        // 删除旧消息
        chatMessageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getConversationId, conversationId));
        if (messagesJson == null || messagesJson.isBlank()) return;
        try {
            List<Map<String, Object>> msgs = objectMapper.readValue(messagesJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            int order = 0;
            for (Map<String, Object> msg : msgs) {
                AiChatMessage entity = new AiChatMessage();
                entity.setConversationId(conversationId);
                entity.setMsgId(String.valueOf(msg.getOrDefault("id", "")));
                entity.setRole(String.valueOf(msg.getOrDefault("role", "")));
                entity.setContent(String.valueOf(msg.getOrDefault("content", "")));
                // 解析时间戳
                Object ts = msg.get("timestamp");
                if (ts instanceof Number) {
                    long millis = ((Number) ts).longValue();
                    entity.setCreateTime(java.time.LocalDateTime.ofEpochSecond(
                            millis / 1000, (int) ((millis % 1000) * 1000000),
                            java.time.ZoneOffset.ofHours(8)));
                }
                entity.setSortOrder(order++);
                chatMessageMapper.insert(entity);
            }
        } catch (Exception e) {
            log.error("保存消息失败：conversationId={}", conversationId, e);
        }
    }

    // ==================== 配置读写（内部方法） ====================

    /**
     * 从数据库读取所有提供商的配置映射，并合并默认值
     * 兼容旧格式（单个配置对象），自动迁移
     */
    private Map<String, AiKnowledgeConfigVO> readAllConfigs() {
        Map<String, AiKnowledgeConfigVO> all = new HashMap<>();

        // 1. 先读数据库中的配置
        AiKnowledgeConfig row = configMapper.selectById(KEY_MODEL_CONFIG);
        if (row != null && row.getConfigJson() != null && !row.getConfigJson().isBlank()) {
            try {
                all = objectMapper.readValue(row.getConfigJson(), new TypeReference<Map<String, AiKnowledgeConfigVO>>() {});
            } catch (JsonProcessingException e) {
                // 尝试解析为旧格式（单个 AiKnowledgeConfigVO）
                try {
                    AiKnowledgeConfigVO old = objectMapper.readValue(row.getConfigJson(), AiKnowledgeConfigVO.class);
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

        // 3. 如有新增的默认配置，写回 DB
        if (needSave) {
            try {
                String json = objectMapper.writeValueAsString(all);
                if (row == null) {
                    row = new AiKnowledgeConfig();
                    row.setConfigKey(KEY_MODEL_CONFIG);
                    row.setConfigJson(json);
                    configMapper.insert(row);
                } else {
                    row.setConfigJson(json);
                    configMapper.updateById(row);
                }
                log.info("已补全所有提供商的默认配置");
            } catch (JsonProcessingException e) {
                log.error("写入默认配置失败", e);
            }
        }

        return all;
    }

    /**
     * 将所有提供商的配置写入数据库
     */
    private void writeAllConfigs(Map<String, AiKnowledgeConfigVO> configs) {
        try {
            String json = objectMapper.writeValueAsString(configs);
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

    /**
     * 保存某个提供商的配置
     */
    private void saveProviderConfig(String provider, AiKnowledgeConfigVO config) {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();
        all.put(provider, config);
        writeAllConfigs(all);
    }

    /**
     * 默认配置构建
     */
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

    private AiKnowledgeConfigVO defaultForProvider(String provider) {
        return DEFAULT_CONFIGS.getOrDefault(provider, DEFAULT_CONFIGS.get("openai"));
    }

    /**
     * 脱敏 API Key
     */
    private AiKnowledgeConfigVO maskConfig(AiKnowledgeConfigVO config) {
        if (config == null) return null;
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

    // ==================== 用量统计 ====================

    @Override
    public AiChatUsageVO getChatUsage() {
        AiChatUsageVO usage = usageTracker.getUsage();
        try {
            BigDecimal balance = queryDefaultProviderBalance();
            usage.setRemainingBalance(balance);
        } catch (Exception e) {
            // 余额查询失败不阻断用量统计，记录告警后按无余额返回
            log.warn("查询默认供应商余额失败，按无余额处理", e);
        }
        return usage;
    }

    /**
     * 查询默认提供商的 API 余额
     */
    private BigDecimal queryDefaultProviderBalance() {
        AiKnowledgeConfigVO config = resolveDefaultConfig();
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return null;
        }
        LlmProviderStrategy strategy = strategyFactory.getStrategy(config.getProvider());
        return strategy.queryBalance(config);
    }

    /**
     * 解析默认配置（标记为 default 且有 API Key 的 → 标记为 default 的 → 第一个有 API Key 的 → 第一个）
     * <p>避免默认提供商未配置 API Key 时返回空 key 的配置</p>
     */
    private AiKnowledgeConfigVO resolveDefaultConfig() {
        Map<String, AiKnowledgeConfigVO> all = readAllConfigs();
        AiKnowledgeConfigVO defaultWithKey = null;
        AiKnowledgeConfigVO defaultNoKey = null;

        for (Map.Entry<String, AiKnowledgeConfigVO> e : all.entrySet()) {
            boolean isDefault = Boolean.TRUE.equals(e.getValue().getDefaultProvider());
            boolean hasKey = e.getValue().getApiKey() != null && !e.getValue().getApiKey().isBlank();

            if (isDefault && hasKey) return e.getValue();          // 最优：默认且有 Key
            if (isDefault) defaultNoKey = e.getValue();            // 次优：默认但无 Key
            if (hasKey && defaultWithKey == null) defaultWithKey = e.getValue();  // 备选：有 Key 的第一个
        }

        if (defaultNoKey != null) return defaultNoKey;   // 有默认但无 Key
        if (defaultWithKey != null) return defaultWithKey;  // 无默认但有 Key
        return all.isEmpty() ? null : all.values().iterator().next();
    }

    @Override
    public void recordChatUsage(AiChatUsageRecordRequest request) {
        usageTracker.recordUsage(request.getTokens(), request.getCost());
    }
}
