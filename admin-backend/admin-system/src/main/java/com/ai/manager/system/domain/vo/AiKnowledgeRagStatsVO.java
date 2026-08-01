package com.ai.manager.system.domain.vo;

import lombok.Data;

/**
 * AI 知识库 - RAG 统计 VO
 */
@Data
public class AiKnowledgeRagStatsVO {

    /** 总文档数 */
    private Long totalDocs;

    /** 已就绪数 */
    private Long readyCount;

    /** 处理中数 */
    private Long processingCount;

    /** 失败数 */
    private Long failedCount;

    /** 总分块数 */
    private Long totalChunks;
}
