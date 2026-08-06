package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * AI 知识库 - 聊天响应 VO
 */
@Data
public class AiKnowledgeChatResponse {

    /** AI 回答内容 */
    private String answer;

    /** 引用的知识来源 (RAG 模式时) */
    private List<RagSourceItem> sources;

    /** 本次请求消耗的 Token 总数 */
    private Long totalTokens;

    @Data
    public static class RagSourceItem {
        /** 雪花 ID 超出 JS Number 安全整数范围，序列化为字符串避免前端精度丢失 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long documentId;
        private String fileName;
        private Integer chunkIndex;
        private String content;
        private Double score;
    }
}
