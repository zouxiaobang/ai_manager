package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 知识库 - RAG 文档预览内容 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeRagDocumentContentVO {

    /** 雪花 ID 超出 JS Number 安全整数范围，序列化为字符串避免前端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 文件名 */
    private String fileName;

    /** 文件类型 (pdf/txt/md/html/docx) */
    private String fileType;

    /** 文档内容：md/txt 返回原始文本（md 供前端 marked 渲染），pdf/docx/html 返回解析后的纯文本 */
    private String content;
}
