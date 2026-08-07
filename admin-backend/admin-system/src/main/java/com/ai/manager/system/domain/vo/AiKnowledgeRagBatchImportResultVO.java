package com.ai.manager.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 知识库 - RAG 批量导入笔记结果 VO
 *
 * <p>imported 为成功导入数；failed 为单篇失败明细，便于前端逐条提示（单篇失败不中断整体批量导入）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeRagBatchImportResultVO {

    /** 成功导入的笔记数 */
    private int imported;

    /** 失败明细（含失败笔记 ID 与原因） */
    private List<FailedItem> failed;

    /**
     * 单篇失败项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {

        /** 失败的笔记 ID */
        private Long noteId;

        /** 失败原因 */
        private String message;
    }
}
