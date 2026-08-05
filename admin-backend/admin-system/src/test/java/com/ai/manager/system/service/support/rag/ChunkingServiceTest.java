package com.ai.manager.system.service.support.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChunkingService 单测
 *
 * <p>重点：递归分块必须线性处理。旧版 splitSentences 用正则
 * {@code ([^。！？\n.!?]+[。！？\n.!?])} 切句，大段无句末标点的文本会灾难性回溯
 * （O(n²)），长文档直接卡死异步线程池。回归用例用超时兜底。</p>
 */
class ChunkingServiceTest {

    private final ChunkingService service = new ChunkingService();
    private final ChunkingConfig config = ChunkingConfig.builder()
            .maxChunkSize(500)
            .chunkOverlap(50)
            .strategy(ChunkingConfig.ChunkingStrategy.RECURSIVE)
            .build();

    @Test
    @Timeout(5)
    void recursive_大段无标点文本_线性切分不卡死() {
        // 无任何句末标点/换行的长文本（约 3 万字符）：旧正则在此灾难性回溯，新实现 O(n)
        // @Timeout(5) 为回归兜底——旧实现会超时失败，新实现立即返回
        String text = "这是一段没有任何句末标点换行的长文本内容".repeat(1000);

        List<ChunkingService.Chunk> chunks = service.chunk(text, config);

        // 无句界时整段作为一块（已知限制：超长无标点块后续可能触发嵌入输入超长，另作优化）
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getContent()).contains("长文本内容");
    }

    @Test
    @Timeout(5)
    void recursive_按句子切分超长段落() {
        // 单段超过 maxSize（500），触发按句子切分
        String text = "第一句话用于测试分块。".repeat(60);
        assertThat(text.length()).isGreaterThan(500);

        List<ChunkingService.Chunk> chunks = service.chunk(text, config);

        // 应被切分为多块，而不是整段一坨
        assertThat(chunks).hasSizeGreaterThan(1);
        for (ChunkingService.Chunk c : chunks) {
            assertThat(c.getContent()).contains("第一句话用于测试分块");
            assertThat(c.getContent().length()).isLessThanOrEqualTo(600);
        }
    }

    @Test
    @Timeout(5)
    void recursive_孤立连续标点不产生空句子() {
        // 连续标点、以及以标点开头的文本，不应产生空 chunk
        String text = "。。！！？正常句子内容。";

        List<ChunkingService.Chunk> chunks = service.chunk(text, config);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(c -> !c.getContent().isBlank());
    }
}
