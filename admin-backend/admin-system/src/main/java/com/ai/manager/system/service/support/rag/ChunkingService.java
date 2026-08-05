package com.ai.manager.system.service.support.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本分块服务
 *
 * <p>支持固定大小、递归、按 Markdown 标题三种分块策略。</p>
 */
@Component
public class ChunkingService {

    /** Markdown 标题正则 */
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 将文本按配置分块
     *
     * @param text   原始文本
     * @param config 分块配置
     * @return 分块列表
     */
    public List<Chunk> chunk(String text, ChunkingConfig config) {
        return switch (config.getStrategy()) {
            case FIXED -> fixedSizeChunk(text, config);
            case RECURSIVE -> recursiveChunk(text, config);
            case HEADER_BASED -> headerBasedChunk(text, config);
        };
    }

    /**
     * 固定大小分块
     */
    private List<Chunk> fixedSizeChunk(String text, ChunkingConfig config) {
        List<Chunk> chunks = new ArrayList<>();
        int size = config.getMaxChunkSize();
        int overlap = config.getChunkOverlap();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            // 尽量在句号或换行处断开
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, end, size / 2);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                chunks.add(Chunk.builder()
                        .chunkIndex(index++)
                        .content(content)
                        .tokenCount(estimateTokens(content))
                        .build());
            }

            start = end - overlap;
            if (start >= text.length() || start >= end) break;
        }

        return chunks;
    }

    /**
     * 递归分块（按段落→句子逐级切分）
     */
    private List<Chunk> recursiveChunk(String text, ChunkingConfig config) {
        int maxSize = config.getMaxChunkSize();
        int overlap = config.getChunkOverlap();

        // 先按段落切分
        String[] paragraphs = text.split("\n\\s*\n");
        List<String> combined = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() > maxSize && current.length() > 0) {
                combined.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (para.length() > maxSize) {
                // 超长段落按句子切分
                List<String> sentences = splitSentences(para);
                for (String sentence : sentences) {
                    if (current.length() + sentence.length() > maxSize && current.length() > 0) {
                        combined.add(current.toString().trim());
                        // overlap: 保留最后一段
                        String currentStr = current.toString();
                        int overlapStart = Math.max(0, currentStr.length() - overlap);
                        current = new StringBuilder(currentStr.substring(overlapStart));
                    }
                    current.append(sentence);
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            }
        }
        if (current.length() > 0) {
            combined.add(current.toString().trim());
        }

        // 转换为 Chunk 对象
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < combined.size(); i++) {
            String c = combined.get(i);
            if (!c.isEmpty()) {
                chunks.add(Chunk.builder()
                        .chunkIndex(i)
                        .content(c)
                        .tokenCount(estimateTokens(c))
                        .build());
            }
        }
        return chunks;
    }

    /**
     * 按 Markdown 标题分块
     */
    private List<Chunk> headerBasedChunk(String text, ChunkingConfig config) {
        List<Chunk> chunks = new ArrayList<>();
        Matcher matcher = HEADER_PATTERN.matcher(text);
        int lastEnd = 0;
        String lastHeader = "";
        int index = 0;

        while (matcher.find()) {
            if (lastEnd > 0) {
                String section = text.substring(lastEnd, matcher.start()).trim();
                if (!section.isEmpty()) {
                    String content = lastHeader + "\n" + section;
                    // 如果过长则递归切分
                    if (content.length() > config.getMaxChunkSize()) {
                        chunks.addAll(fixedSizeChunk(content, config));
                    } else {
                        chunks.add(Chunk.builder()
                                .chunkIndex(index++)
                                .content(content)
                                .tokenCount(estimateTokens(content))
                                .build());
                    }
                }
            }
            lastHeader = matcher.group().trim();
            lastEnd = matcher.end();
        }

        // 剩余内容
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                String content = lastHeader.isEmpty() ? remaining : lastHeader + "\n" + remaining;
                chunks.add(Chunk.builder()
                        .chunkIndex(index++)
                        .content(content)
                        .tokenCount(estimateTokens(content))
                        .build());
            }
        }

        // 如果没有匹配到标题，退回递归分块
        if (chunks.isEmpty()) {
            return recursiveChunk(text, config);
        }

        return chunks;
    }

    /**
     * 找到合适的断点（句号或换行附近）
     */
    private int findBreakPoint(String text, int around, int range) {
        int start = Math.max(0, around - range);
        int end = Math.min(text.length(), around + range);

        // 从 around 向前找句号或换行
        for (int i = around; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '\n' || c == '.' || c == '！' || c == '？') {
                return i + 1;
            }
        }
        // 找不到则向后找
        for (int i = around; i < end; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '\n' || c == '.' || c == '！' || c == '？') {
                return i + 1;
            }
        }
        return around;
    }

    /**
     * 将文本切分为句子（线性扫描）
     *
     * <p>原实现用正则 {@code ([^。！？\n.!?]+[。！？\n.!?])} 切分，遇到大段无句末标点的文本
     * （代码块、无标点长句等）会灾难性回溯（O(n²)），长文档直接把异步线程池卡死。
     * 改为逐字符扫描、遇终止符即切分，O(n)。孤立/连续的终止符按旧语义丢弃。</p>
     */
    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean hasContent = false; // 自上次切分后是否出现过非终止符
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (isSentenceTerminator(c)) {
                if (hasContent) {
                    sentences.add(current.toString().trim());
                    current.setLength(0);
                    hasContent = false;
                } else {
                    // 孤立/连续终止符（如 "。。" 第二个）不属于任何句子，丢弃
                    current.setLength(0);
                }
            } else {
                hasContent = true;
            }
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty() && hasContent) {
            sentences.add(remaining);
        }
        return sentences;
    }

    private static boolean isSentenceTerminator(char c) {
        return c == '。' || c == '！' || c == '？' || c == '\n' || c == '.' || c == '!' || c == '?';
    }

    /**
     * 估算 Token 数（按 1 token ≈ 1.5 char 中文估算）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 1.5);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk {
        private int chunkIndex;
        private String content;
        private int tokenCount;
    }
}
