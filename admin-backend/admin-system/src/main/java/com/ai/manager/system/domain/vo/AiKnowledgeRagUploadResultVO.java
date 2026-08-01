package com.ai.manager.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 知识库 - RAG 文档上传结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeRagUploadResultVO {

    private Long documentId;

    private String fileName;

    private String status;

    private String message;
}
