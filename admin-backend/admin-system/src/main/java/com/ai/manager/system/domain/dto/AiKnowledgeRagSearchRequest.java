package com.ai.manager.system.domain.dto;

import lombok.Data;

/**
 * AI 知识库 - RAG 搜索请求 DTO
 */
@Data
public class AiKnowledgeRagSearchRequest {

    /** 搜索关键词 */
    private String query;

    /** 返回 topK 条结果 */
    private Integer topK;
}
