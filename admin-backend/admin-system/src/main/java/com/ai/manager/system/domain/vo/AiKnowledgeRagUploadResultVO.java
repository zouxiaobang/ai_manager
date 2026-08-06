package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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

    /** 雪花 ID 超出 JS Number 安全整数范围，序列化为字符串避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long documentId;

    private String fileName;

    private String status;

    private String message;
}
