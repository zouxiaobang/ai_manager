package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 知识库 - RAG 批量导入笔记请求 DTO
 *
 * <p>body 形如 {@code { "noteIds": [1, 2, 3] }}；noteIds 为空时服务直接返回全 0 结果。</p>
 */
@Data
public class AiKnowledgeRagBatchImportRequest {

    /** 待导入的笔记 ID 列表 */
    private List<Long> noteIds;
}
