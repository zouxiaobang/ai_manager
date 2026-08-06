package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG 知识库 - 文档实体
 */
@Data
@TableName("rag_document")
public class RagDocument {

    /** 雪花 ID 超出 JS Number 安全整数范围，必须序列化为字符串，否则前端会丢失精度把删除/重试 id 传错 */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型 (pdf/txt/md/html/docx) */
    private String fileType;

    /** 文件大小（bytes） */
    private Long fileSize;

    /** 服务器存储路径 */
    private String filePath;

    /** 分块数 */
    private Integer chunkCount;

    /** 状态: pending / processing / ready / failed */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 处理失败重试次数（达到上限后启动不再自动重投，防止死循环） */
    private Integer retryCount;

    /** 索引完成时间 */
    private LocalDateTime indexedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
