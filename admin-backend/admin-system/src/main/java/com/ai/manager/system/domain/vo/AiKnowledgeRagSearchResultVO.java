package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 知识库 - RAG 搜索结果 VO
 */
@Data
public class AiKnowledgeRagSearchResultVO {

    private List<AiKnowledgeChatResponse.RagSourceItem> sources;
}
