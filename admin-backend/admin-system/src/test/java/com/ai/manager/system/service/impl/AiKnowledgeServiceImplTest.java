package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.RagProperties;
import com.ai.manager.system.domain.dto.AiKnowledgeChatRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeConfigSaveRequest;
import com.ai.manager.system.domain.dto.AiKnowledgeRagSearchRequest;
import com.ai.manager.system.domain.entity.AiKnowledgeConfig;
import com.ai.manager.system.domain.entity.RagChunk;
import com.ai.manager.system.domain.entity.RagDocument;
import com.ai.manager.system.domain.vo.AiKnowledgeChatResponse;
import com.ai.manager.system.domain.vo.AiKnowledgeConfigVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagDocumentVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagSearchResultVO;
import com.ai.manager.system.domain.vo.AiKnowledgeRagUploadResultVO;
import com.ai.manager.system.mapper.AiChatCategoryMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.ai.manager.system.mapper.AiChatConversationMapper;
import com.ai.manager.system.mapper.AiChatMessageMapper;
import com.ai.manager.system.mapper.AiKnowledgeConfigMapper;
import com.ai.manager.system.mapper.RagChunkMapper;
import com.ai.manager.system.mapper.RagDocumentMapper;
import com.ai.manager.system.service.support.AiKnowledgeConfigStore;
import com.ai.manager.system.service.support.ApiBaseUrlValidator;
import com.ai.manager.system.service.support.ConfigCryptoService;
import com.ai.manager.system.service.support.llm.LlmProviderStrategy;
import com.ai.manager.system.service.support.llm.LlmProviderStrategyFactory;
import com.ai.manager.system.service.support.llm.PromptBuilder;
import com.ai.manager.system.service.support.llm.UsageTracker;
import com.ai.manager.system.service.support.rag.ChunkingConfig;
import com.ai.manager.system.service.support.rag.ChunkingService;
import com.ai.manager.system.service.support.rag.DocumentParser;
import com.ai.manager.system.service.support.rag.EmbeddingService;
import com.ai.manager.system.service.support.rag.PgVectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AiKnowledgeServiceImpl 单测
 *
 * <p>重点覆盖 RAG 文档上传：验证上传文件确实写入配置的上传目录、记录的文件路径指向该目录、
 * 以及不合法文件类型被拒绝。processDocument 的嵌入管线失败会被服务吞掉（置 status=failed），
 * 因此无需 mock 解析/分块/嵌入整条链路即可断言文件写入行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class AiKnowledgeServiceImplTest {

    @Mock
    private AiKnowledgeConfigMapper configMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AiKnowledgeConfigStore configStore;
    @Mock
    private AiChatCategoryMapper chatCategoryMapper;
    @Mock
    private AiChatConversationMapper chatConversationMapper;
    @Mock
    private AiChatMessageMapper chatMessageMapper;
    @Mock
    private RagDocumentMapper ragDocumentMapper;
    @Mock
    private RagChunkMapper ragChunkMapper;
    @Mock
    private LlmProviderStrategyFactory strategyFactory;
    @Mock
    private PromptBuilder promptBuilder;
    @Mock
    private UsageTracker usageTracker;
    @Mock
    private DocumentParser documentParser;
    @Mock
    private ChunkingService chunkingService;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private PgVectorStore pgVectorStore;
    @Mock
    private Executor ragProcessExecutor;
    @Mock
    private ConfigCryptoService configCrypto;
    @Mock
    private ApiBaseUrlValidator apiBaseUrlValidator;

    private RagProperties ragProperties;
    private AiKnowledgeServiceImpl service;
    private Path uploadDir;

    /** 纯单测无 Spring 上下文：为 MyBatis-Plus 的 LambdaUpdateWrapper 初始化实体 lambda 缓存 */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RagDocument.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        ragProperties = new RagProperties();
        uploadDir = Files.createTempDirectory("rag-upload-test");
        ragProperties.setUploadPath(uploadDir.toString());

        service = new AiKnowledgeServiceImpl(
                configMapper, objectMapper, configStore, configCrypto, apiBaseUrlValidator,
                chatCategoryMapper, chatConversationMapper, chatMessageMapper, ragDocumentMapper,
                ragChunkMapper, strategyFactory, promptBuilder, usageTracker, documentParser,
                chunkingService, embeddingService, pgVectorStore, ragProcessExecutor, ragProperties);
        service.initRagUploadDir();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Stream<Path> stream = Files.walk(uploadDir)) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void initRagUploadDir_创建配置的上传目录() {
        assertThat(Files.isDirectory(uploadDir)).isTrue();
    }

    @Test
    void uploadRagDocument_合法文件写入上传目录并创建记录() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello rag".getBytes(StandardCharsets.UTF_8));
        // 模拟 MyBatis-Plus 回填主键
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));

        AiKnowledgeRagUploadResultVO result = service.uploadRagDocument(file);

        assertThat(result.getFileName()).isEqualTo("note.txt");
        assertThat(result.getDocumentId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("processing");

        // 文件确实写入 uploadDir 下
        try (Stream<Path> stream = Files.list(uploadDir)) {
            assertThat(stream.filter(Files::isRegularFile)).hasSize(1);
        }

        // 记录的文件路径为相对上传根目录的相对路径（P1-4：不暴露服务器绝对路径）
        ArgumentCaptor<RagDocument> captor = ArgumentCaptor.forClass(RagDocument.class);
        verify(ragDocumentMapper).insert(captor.capture());
        String storedPath = captor.getValue().getFilePath();
        Path resolved = uploadDir.resolve(storedPath);
        assertThat(resolved).isRegularFile();
        assertThat(resolved.startsWith(uploadDir.toAbsolutePath())).isTrue();
        assertThat(captor.getValue().getFileName()).isEqualTo("note.txt");
    }

    @Test
    void uploadRagDocument_不支持的文件类型抛业务异常() {
        MultipartFile file = new MockMultipartFile("file", "note.exe", "application/octet-stream",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.uploadRagDocument(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");

        verifyNoInteractions(ragDocumentMapper);
        try (Stream<Path> stream = Files.list(uploadDir)) {
            assertThat(stream.filter(Files::isRegularFile)).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void uploadRagDocument_提交异步处理任务并立即返回() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello rag".getBytes(StandardCharsets.UTF_8));
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));

        AiKnowledgeRagUploadResultVO result = service.uploadRagDocument(file);

        assertThat(result.getStatus()).isEqualTo("processing");
        assertThat(result.getDocumentId()).isEqualTo(1L);
        // 立即返回：不应同步执行解析/分块/嵌入等重活
        verifyNoInteractions(documentParser, chunkingService, embeddingService);
        // 异步任务已提交到线程池
        verify(ragProcessExecutor).execute(any(Runnable.class));
    }

    @Test
    void 异步处理成功置ready并存储向量() throws Exception {
        // 上传时同步执行提交的任务，便于断言整条处理链路
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(ragProcessExecutor).execute(any(Runnable.class));
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        // doProcessDocument 开头会幂等清理历史分块
        when(ragChunkMapper.selectList(any())).thenReturn(List.of());
        when(documentParser.parse(any(InputStream.class), anyString())).thenReturn("hello world");
        ChunkingService.Chunk chunk = ChunkingService.Chunk.builder()
                .chunkIndex(0).content("hello world").tokenCount(8).build();
        when(chunkingService.chunk(anyString(), any(ChunkingConfig.class))).thenReturn(List.of(chunk));
        AiKnowledgeConfigVO cfg = new AiKnowledgeConfigVO();
        cfg.setApiKey("sk-test");
        when(configStore.readAllConfigs()).thenReturn(Map.of("openai", cfg));
        when(embeddingService.embed(anyList(), any(AiKnowledgeConfigVO.class)))
                .thenReturn(List.of(new float[]{0.1f, 0.2f}));
        doAnswer(inv -> {
            RagChunk c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 10L);
            return 1;
        }).when(ragChunkMapper).insert(any(RagChunk.class));
        // 文档记录仍存在（插入分块/向量前的 4.5 复核守卫）
        when(ragDocumentMapper.selectById(1L)).thenReturn(docRecord(1L));

        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));
        service.uploadRagDocument(file);

        verify(pgVectorStore).storeBatch(anyList());
        ArgumentCaptor<Wrapper<RagDocument>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ragDocumentMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<RagDocument> wrapper = (LambdaUpdateWrapper<RagDocument>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs()).containsValue("ready");
        // 成功后重置重试次数：参数值为 0（其余参数为 chunk_count=1 / indexed_at，故 0 即 retry_count）
        assertThat(wrapper.getParamNameValuePairs().values()).contains(0);
    }

    @Test
    void 异步处理期间文档被删除则中止不产生孤儿向量() throws Exception {
        // 上传时同步执行提交的任务
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(ragProcessExecutor).execute(any(Runnable.class));
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        when(ragChunkMapper.selectList(any())).thenReturn(List.of());
        when(documentParser.parse(any(InputStream.class), anyString())).thenReturn("hello world");
        ChunkingService.Chunk chunk = ChunkingService.Chunk.builder()
                .chunkIndex(0).content("hello world").tokenCount(8).build();
        when(chunkingService.chunk(anyString(), any(ChunkingConfig.class))).thenReturn(List.of(chunk));
        AiKnowledgeConfigVO cfg = new AiKnowledgeConfigVO();
        cfg.setApiKey("sk-test");
        when(configStore.readAllConfigs()).thenReturn(Map.of("openai", cfg));
        when(embeddingService.embed(anyList(), any(AiKnowledgeConfigVO.class)))
                .thenReturn(List.of(new float[]{0.1f, 0.2f}));
        // 守卫点：异步处理期间文档被删除 → selectById 返回 null
        when(ragDocumentMapper.selectById(1L)).thenReturn(null);

        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));
        service.uploadRagDocument(file);

        // 中止：不插 chunk、不存向量、不改状态，避免 deleteByDocId 之后向量被插回成孤儿
        verify(ragChunkMapper, never()).insert(any(RagChunk.class));
        verify(pgVectorStore, never()).storeBatch(anyList());
        verify(ragDocumentMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void 异步处理失败置failed并清理孤儿分块() throws Exception {
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(ragProcessExecutor).execute(any(Runnable.class));
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        when(documentParser.parse(any(InputStream.class), anyString()))
                .thenThrow(new RuntimeException("parse boom"));
        RagChunk orphan = new RagChunk();
        ReflectionTestUtils.setField(orphan, "id", 10L);
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(orphan));

        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));
        service.uploadRagDocument(file);

        // 失败后置 failed
        ArgumentCaptor<Wrapper<RagDocument>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ragDocumentMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<RagDocument> wrapper = (LambdaUpdateWrapper<RagDocument>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs()).containsValue("failed");
        // 清理孤儿分块 + 向量：doProcessDocument 开头幂等清理一次，失败兜底再清理一次
        verify(pgVectorStore, times(2)).deleteByChunkIds(List.of(10L));
        verify(ragChunkMapper, times(2)).delete(any());
        // 失败累计重试次数（SQL 原子自增）
        assertThat(wrapper.getSqlSet()).contains("retry_count = retry_count + 1");
    }

    @Test
    void 异步失败时清理向量抛错不阻断置failed() {
        // 上传时同步执行提交的任务，便于断言整条处理链路
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(ragProcessExecutor).execute(any(Runnable.class));
        doAnswer(inv -> {
            RagDocument doc = inv.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", 1L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        when(documentParser.parse(any(InputStream.class), anyString()))
                .thenThrow(new RuntimeException("parse boom"));
        RagChunk orphan = new RagChunk();
        ReflectionTestUtils.setField(orphan, "id", 10L);
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(orphan));
        // 向量清理抛错：cleanupOrphanChunks 内部兜底，不得阻断置 failed / 累计重试
        doThrow(new RuntimeException("pg down")).when(pgVectorStore).deleteByChunkIds(anyList());

        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));
        service.uploadRagDocument(file);

        ArgumentCaptor<Wrapper<RagDocument>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ragDocumentMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<RagDocument> wrapper = (LambdaUpdateWrapper<RagDocument>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs()).containsValue("failed");
        assertThat(wrapper.getSqlSet()).contains("retry_count = retry_count + 1");
    }

    @Test
    void resumePendingRagDocuments_重投失败与中断文档() throws Exception {
        Path file = Files.createTempFile("resume", ".txt");
        Files.writeString(file, "content");
        RagDocument doc = new RagDocument();
        ReflectionTestUtils.setField(doc, "id", 1L);
        doc.setFileName("a.txt");
        doc.setFilePath(file.toString());
        doc.setStatus("failed");
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(doc));

        service.resumePendingRagDocuments();

        // 置 processing 后重新提交异步处理（旧分块清理在 doProcessDocument 开头幂等完成）
        ArgumentCaptor<Wrapper<RagDocument>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ragDocumentMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<RagDocument> wrapper = (LambdaUpdateWrapper<RagDocument>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs()).containsValue("processing");
        verify(ragProcessExecutor).execute(any(Runnable.class));
    }

    @Test
    void retryRagDocument_置processing并提交异步处理() throws Exception {
        Path retryFile = uploadDir.resolve("retry.txt");
        Files.writeString(retryFile, "content");
        RagDocument doc = new RagDocument();
        ReflectionTestUtils.setField(doc, "id", 1L);
        doc.setFilePath(retryFile.toString());
        doc.setFileType("txt");
        when(ragDocumentMapper.selectById(1L)).thenReturn(doc);

        service.retryRagDocument(1L);

        ArgumentCaptor<Wrapper<RagDocument>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(ragDocumentMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<RagDocument> wrapper = (LambdaUpdateWrapper<RagDocument>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs()).containsValue("processing");
        verify(ragProcessExecutor).execute(any(Runnable.class));
    }

    @Test
    void resumePendingRagDocuments_已达重试上限的文档跳过() throws Exception {
        Path file = uploadDir.resolve("over-limit.txt");
        Files.writeString(file, "content");
        RagDocument over = new RagDocument();
        ReflectionTestUtils.setField(over, "id", 1L);
        over.setFilePath(file.toString());
        over.setStatus("failed");
        over.setRetryCount(3); // 已达上限（maxRetry=3），启动不再自动重投，防止死循环
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(over));

        service.resumePendingRagDocuments();

        verify(ragProcessExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void 已在处理中的文档不重复提交() throws Exception {
        Path inFlight = uploadDir.resolve("in-flight.txt");
        Files.writeString(inFlight, "x");
        RagDocument doc = new RagDocument();
        ReflectionTestUtils.setField(doc, "id", 9L);
        doc.setFilePath(inFlight.toString());
        doc.setFileType("txt");
        when(ragDocumentMapper.selectById(9L)).thenReturn(doc);

        // 第一次重试：任务入队但未执行（模拟仍在处理中）
        service.retryRagDocument(9L);
        // 第二次重试同一文档：in-flight 防重，不再投递
        service.retryRagDocument(9L);

        verify(ragProcessExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void uploadRagDocument_入库失败清理已落盘文件() {
        // 文件已 transferTo 成功，但 insert 抛错 → 补清理磁盘文件，避免孤儿文件残留
        when(ragDocumentMapper.insert(any(RagDocument.class))).thenThrow(new RuntimeException("db down"));
        MultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.uploadRagDocument(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文档上传失败");

        try (Stream<Path> stream = Files.list(uploadDir)) {
            assertThat(stream.filter(Files::isRegularFile)).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        verify(ragProcessExecutor, never()).execute(any(Runnable.class));
    }

    // ==================== P1-3 安全加固接线 ====================

    @Test
    void saveEmbeddingConfig_写入perProvider映射并置活动标记() {
        AiKnowledgeConfigSaveRequest request = new AiKnowledgeConfigSaveRequest();
        request.setProvider("openai");
        request.setApiKey("sk-real");
        request.setApiBaseUrl("https://api.openai.com/v1");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>());
        when(configStore.defaultConfig("openai")).thenReturn(null);

        service.saveEmbeddingConfig(request);

        // 合法公网 apiBaseUrl 会经过校验器
        verify(apiBaseUrlValidator).validate("https://api.openai.com/v1");
        // 写入 per-provider 映射：保存真实 key 且标记为活动提供商
        ArgumentCaptor<Map<String, AiKnowledgeConfigVO>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configStore).writeEmbeddingConfigs(captor.capture());
        AiKnowledgeConfigVO saved = captor.getValue().get("openai");
        assertThat(saved.getApiKey()).isEqualTo("sk-real");
        assertThat(saved.getDefaultProvider()).isTrue();
    }

    @Test
    void saveEmbeddingConfig_掩码key保留已存明文() {
        AiKnowledgeConfigSaveRequest request = new AiKnowledgeConfigSaveRequest();
        request.setProvider("openai");
        request.setApiKey("sk-****abcd");
        request.setApiBaseUrl("https://api.openai.com/v1");
        AiKnowledgeConfigVO existing = new AiKnowledgeConfigVO();
        existing.setApiKey("sk-REAL-SECRET");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("openai", existing)));

        service.saveEmbeddingConfig(request);

        ArgumentCaptor<Map<String, AiKnowledgeConfigVO>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configStore).writeEmbeddingConfigs(captor.capture());
        assertThat(captor.getValue().get("openai").getApiKey()).isEqualTo("sk-REAL-SECRET");
    }

    @Test
    void saveEmbeddingConfig_清除场景删除该提供商记录() {
        AiKnowledgeConfigSaveRequest request = new AiKnowledgeConfigSaveRequest();
        request.setProvider("openai");
        request.setApiKey("");
        request.setApiBaseUrl("");
        request.setModel("");
        request.setEmbeddingModel("");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("openai", new AiKnowledgeConfigVO())));

        service.saveEmbeddingConfig(request);

        ArgumentCaptor<Map<String, AiKnowledgeConfigVO>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configStore).writeEmbeddingConfigs(captor.capture());
        assertThat(captor.getValue()).doesNotContainKey("openai");
        // 清除场景不触发 SSRF 校验（空地址直接放行）
        verify(apiBaseUrlValidator, never()).validate(anyString());
    }

    @Test
    void saveEmbeddingConfig_非法apiBaseUrl被拒() {
        AiKnowledgeConfigSaveRequest request = new AiKnowledgeConfigSaveRequest();
        request.setProvider("openai");
        request.setApiKey("sk-real");
        request.setApiBaseUrl("http://192.168.1.100/v1");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>());
        doThrow(new BusinessException(ResultCode.BAD_REQUEST.getCode(), "apiBaseUrl 禁止指向内网"))
                .when(apiBaseUrlValidator).validate("http://192.168.1.100/v1");

        assertThatThrownBy(() -> service.saveEmbeddingConfig(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内网");

        verify(configStore, never()).writeEmbeddingConfigs(any());
    }

    @Test
    void saveConfig_校验apiBaseUrl() {
        AiKnowledgeConfigSaveRequest request = new AiKnowledgeConfigSaveRequest();
        request.setProvider("qwen");
        request.setApiKey("sk-real");
        request.setApiBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        when(configStore.readAllConfigs()).thenReturn(new HashMap<>());
        when(configStore.defaultConfig("qwen")).thenReturn(null);

        service.saveConfig(request);

        verify(apiBaseUrlValidator).validate("https://dashscope.aliyuncs.com/compatible-mode/v1");
        verify(configStore).writeAllConfigs(any());
    }

    @Test
    void getEmbeddingConfig_未指定提供商返回活动配置() {
        AiKnowledgeConfigVO active = new AiKnowledgeConfigVO();
        active.setProvider("qwen");
        active.setDefaultProvider(true);
        active.setApiKey("sk-qwen");
        AiKnowledgeConfigVO openai = new AiKnowledgeConfigVO();
        openai.setProvider("openai");
        openai.setApiKey("sk-openai");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("qwen", active, "openai", openai)));
        when(configStore.maskConfig(active)).thenReturn(active);

        AiKnowledgeConfigVO result = service.getEmbeddingConfig(null);

        assertThat(result.getProvider()).isEqualTo("qwen");
    }

    @Test
    void getEmbeddingConfig_按提供商返回对应配置() {
        AiKnowledgeConfigVO openai = new AiKnowledgeConfigVO();
        openai.setProvider("openai");
        openai.setApiKey("sk-openai");
        AiKnowledgeConfigVO deepseek = new AiKnowledgeConfigVO();
        deepseek.setProvider("deepseek");
        deepseek.setApiKey("sk-deepseek");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("openai", openai, "deepseek", deepseek)));
        when(configStore.maskConfig(deepseek)).thenReturn(deepseek);

        AiKnowledgeConfigVO result = service.getEmbeddingConfig("deepseek");

        assertThat(result.getProvider()).isEqualTo("deepseek");
    }

    @Test
    void getEmbeddingConfig_无已保存配置返回null() {
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>());

        assertThat(service.getEmbeddingConfig(null)).isNull();
        assertThat(service.getEmbeddingConfig("openai")).isNull();
    }

    // ==================== searchRag ====================

    @Test
    void searchRag_正常返回相关文档片段() throws Exception {
        AiKnowledgeConfigVO embedCfg = new AiKnowledgeConfigVO();
        embedCfg.setProvider("openai");
        embedCfg.setApiKey("sk-x");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("openai", embedCfg)));
        when(embeddingService.embedQuery(eq("什么是番茄钟"), eq(embedCfg)))
                .thenReturn(new float[]{0.1f, 0.2f});
        when(pgVectorStore.similaritySearch(any(float[].class), eq(5), eq(0.55)))
                .thenReturn(List.of(PgVectorStore.SearchResult.builder()
                        .chunkId(10L).docId(6L).content("番茄钟是一种时间管理法").score(0.68).build()));
        RagDocument doc = new RagDocument();
        doc.setId(6L);
        doc.setFileName("pomodoro.md");
        when(ragDocumentMapper.selectById(6L)).thenReturn(doc);

        AiKnowledgeRagSearchRequest req = new AiKnowledgeRagSearchRequest();
        req.setQuery("什么是番茄钟");
        req.setTopK(5);

        AiKnowledgeRagSearchResultVO result = service.searchRag(req);

        assertThat(result.getSources()).hasSize(1);
        assertThat(result.getSources().get(0).getFileName()).isEqualTo("pomodoro.md");
        assertThat(result.getSources().get(0).getContent()).isEqualTo("番茄钟是一种时间管理法");
        assertThat(result.getSources().get(0).getScore()).isEqualTo(0.68);
    }

    @Test
    void searchRag_嵌入失败抛业务异常() throws Exception {
        AiKnowledgeConfigVO embedCfg = new AiKnowledgeConfigVO();
        embedCfg.setProvider("openai");
        embedCfg.setApiKey("sk-x");
        when(configStore.readEmbeddingConfigs()).thenReturn(new HashMap<>(Map.of("openai", embedCfg)));
        when(embeddingService.embedQuery(eq("什么是番茄钟"), eq(embedCfg)))
                .thenThrow(new RuntimeException("embedding 服务不可用"));

        AiKnowledgeRagSearchRequest req = new AiKnowledgeRagSearchRequest();
        req.setQuery("什么是番茄钟");

        // P1-1：检索失败不再吞异常，改为抛业务异常让调用方可见
        assertThatThrownBy(() -> service.searchRag(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RAG 检索失败");
    }

    @Test
    void chatStream_useRag时检索知识库并注入上下文() throws Exception {
        // spy 拦截同实例的 searchRag（同一实例方法无法用 @Mock 注入）
        AiKnowledgeServiceImpl spied = spy(service);
        AiKnowledgeChatResponse.RagSourceItem src = new AiKnowledgeChatResponse.RagSourceItem();
        src.setDocumentId(6L);
        src.setFileName("电商平台第一期优化.md");
        src.setScore(0.6);
        src.setContent("电商平台第一期优化方案主要从业务功能、代码组织、性能、安全性、可维护性五个维度优化。");
        AiKnowledgeRagSearchResultVO ragResult = new AiKnowledgeRagSearchResultVO();
        ragResult.setSources(List.of(src));
        doReturn(ragResult).when(spied).searchRag(any());

        // chat 配置（deepseek）
        AiKnowledgeConfigVO cfg = new AiKnowledgeConfigVO();
        cfg.setProvider("deepseek");
        cfg.setApiKey("sk-test");
        when(configStore.readAllConfigs()).thenReturn(Map.of("deepseek", cfg));

        LlmProviderStrategy strategy = mock(LlmProviderStrategy.class);
        when(strategyFactory.getStrategy("deepseek")).thenReturn(strategy);
        when(strategy.chatStream(any(), anyList(), any())).thenReturn(Flux.just("基于知识库的答案"));

        AiKnowledgeChatRequest req = new AiKnowledgeChatRequest();
        req.setQuestion("电商平台第一期优化了哪些方面");
        req.setProvider("deepseek");
        req.setUseRag(true);
        SseEmitter emitter = mock(SseEmitter.class);

        spied.chatStream(req, emitter);

        // 修复点：流式聊天必须走 RAG 消息构建并注入知识库上下文（旧实现直接 buildMessages，忽略 useRag）
        verify(promptBuilder).buildRagMessages(eq(req), contains("电商平台第一期优化方案"));
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void rag_雪花ID序列化为字符串避免前端精度丢失() throws Exception {
        // 真实 19 位雪花 ID（如 2085269452813733889）超出 JS Number.MAX_SAFE_INTEGER（≈9.0e15），
        // 若以 number 下发，前端解析会丢失精度、把删除/重试 id 传错（曾表现为"移除成功但文档仍在"）。
        long snowflakeId = 2085269452813733889L;
        ObjectMapper mapper = new ObjectMapper();

        AiKnowledgeRagUploadResultVO upload = AiKnowledgeRagUploadResultVO.builder()
                .documentId(snowflakeId)
                .fileName("a.md")
                .status("processing")
                .build();
        assertThat(mapper.writeValueAsString(upload))
                .contains("\"documentId\":\"2085269452813733889\"");

        AiKnowledgeRagDocumentVO vo = new AiKnowledgeRagDocumentVO();
        vo.setId(snowflakeId);
        vo.setFileName("a.md");
        assertThat(mapper.writeValueAsString(vo))
                .contains("\"id\":\"2085269452813733889\"");

        AiKnowledgeChatResponse.RagSourceItem source = new AiKnowledgeChatResponse.RagSourceItem();
        source.setDocumentId(snowflakeId);
        assertThat(mapper.writeValueAsString(source))
                .contains("\"documentId\":\"2085269452813733889\"");
    }

    /** 构造最小存在的文档记录（4.5 守卫复核用） */
    private RagDocument docRecord(Long id) {
        RagDocument doc = new RagDocument();
        ReflectionTestUtils.setField(doc, "id", id);
        return doc;
    }
}
