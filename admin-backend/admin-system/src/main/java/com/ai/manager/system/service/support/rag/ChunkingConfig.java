package com.ai.manager.system.service.support.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkingConfig {

    /** 目标 chunk 大小（字符数） */
    @Builder.Default
    private int maxChunkSize = 500;

    /** 相邻 chunk 重叠字符数 */
    @Builder.Default
    private int chunkOverlap = 50;

    /** 分块策略 */
    @Builder.Default
    private ChunkingStrategy strategy = ChunkingStrategy.RECURSIVE;

    public enum ChunkingStrategy {
        FIXED,       // 固定大小切分
        RECURSIVE,   // 递归按段落→句子切分（默认）
        HEADER_BASED // 按 Markdown 标题切分
    }
}
