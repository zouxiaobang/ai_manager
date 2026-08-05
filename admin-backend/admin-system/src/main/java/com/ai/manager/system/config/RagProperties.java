package com.ai.manager.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 知识库配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-manager.rag")
public class RagProperties {

    /** 文档上传存储路径 */
    private String uploadPath = "uploads/rag-docs";

    /** 处理失败最大重试次数（达到上限后启动不再自动重投，防止死循环） */
    private int maxRetry = 3;

    /** 启动期 PG 数据源探针开关（无 PG 环境可设 false 关闭，默认开启） */
    private boolean datasourceProbeEnabled = true;

    /** 分块配置 */
    private ChunkConfig chunk = new ChunkConfig();

    /** 搜索配置 */
    private SearchConfig search = new SearchConfig();

    /**
     * 嵌入向量维度（必须与 rag_vectors.embedding 列维度一致，默认 1024）。
     *
     * <p>qwen text-embedding-v3 原生 1024 维；OpenAI text-embedding-3-small 默认 1536，
     * 通过请求中的 dimensions 参数对齐到本值。改此值需同步迁移表结构。</p>
     */
    private int embeddingDimensions = 1024;

    @Data
    public static class ChunkConfig {
        /** 目标 chunk 大小（字符数） */
        private int maxSize = 500;
        /** 相邻 chunk 重叠字符数 */
        private int overlap = 50;
        /** 分块策略 */
        private String strategy = "recursive";
    }

    @Data
    public static class SearchConfig {
        /** 默认返回 topK 条结果 */
        private int topK = 5;
        /** 相似度阈值（联调实测自然问句约 0.55~0.69，0.72 偏高会漏召回，取 0.65） */
        private double similarityThreshold = 0.65;
    }
}
