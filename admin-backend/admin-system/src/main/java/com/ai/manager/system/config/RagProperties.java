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

    /** 分块配置 */
    private ChunkConfig chunk = new ChunkConfig();

    /** 搜索配置 */
    private SearchConfig search = new SearchConfig();

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
        /** 相似度阈值 */
        private double similarityThreshold = 0.72;
    }
}
