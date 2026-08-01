package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 知识库 - 文档分块实体
 */
@Data
@TableName("rag_chunk")
public class RagChunk {

    @TableId
    private Long id;

    /** 所属文档 ID */
    private Long documentId;

    /** 块序号（从 0 开始） */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** 预估 Token 数 */
    private Integer tokenCount;

    private LocalDateTime createdAt;
}
