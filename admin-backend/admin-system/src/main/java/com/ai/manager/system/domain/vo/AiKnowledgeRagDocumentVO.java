package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库 - RAG 文档 VO
 */
@Data
public class AiKnowledgeRagDocumentVO {

    /** 雪花 ID 超出 JS Number 安全整数范围，序列化为字符串避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小 (bytes) */
    private Long fileSize;

    /** 分块数 */
    private Integer chunkCount;

    /** 状态: pending / processing / ready / failed */
    private String status;

    /** 索引时间 */
    private LocalDateTime indexedAt;

    /** 错误信息 */
    private String errorMessage;
}
