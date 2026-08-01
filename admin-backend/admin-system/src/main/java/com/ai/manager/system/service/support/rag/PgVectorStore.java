package com.ai.manager.system.service.support.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * pgvector 向量存储服务
 *
 * <p>使用 PostgreSQL + pgvector 扩展存储和检索 embedding 向量。</p>
 */
@Slf4j
@Component
public class PgVectorStore {

    private final DataSource pgVectorDataSource;

    public PgVectorStore(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        this.pgVectorDataSource = pgVectorDataSource;
    }

    /**
     * 获取用于向量操作的 JdbcTemplate
     */
    private JdbcTemplate getJdbc() {
        return new JdbcTemplate(pgVectorDataSource);
    }

    /**
     * 批量存储向量
     */
    public void storeBatch(List<VectorRecord> records) {
        if (records.isEmpty()) return;

        String sql = "INSERT INTO rag_vectors (chunk_id, doc_id, embedding, content) VALUES (?, ?, ?::vector, ?)";

        getJdbc().batchUpdate(sql, records, records.size(), (ps, record) -> {
            ps.setLong(1, record.getChunkId());
            ps.setLong(2, record.getDocId());
            ps.setString(3, vectorToString(record.getEmbedding()));
            ps.setString(4, record.getContent());
        });

        log.debug("向量批量存储完成：{} 条", records.size());
    }

    /**
     * 存储单条向量
     */
    public void store(Long chunkId, Long docId, float[] embedding, String content) {
        String sql = "INSERT INTO rag_vectors (chunk_id, doc_id, embedding, content) VALUES (?, ?, ?::vector, ?)";
        getJdbc().update(sql, chunkId, docId, vectorToString(embedding), content);
    }

    /**
     * 按文档 ID 删除所有向量（文档删除/重索引时使用）
     */
    public void deleteByDocId(Long docId) {
        String sql = "DELETE FROM rag_vectors WHERE doc_id = ?";
        int count = getJdbc().update(sql, docId);
        log.debug("删除文档向量：doc_id={}, 影响 {} 行", docId, count);
    }

    /**
     * 按 chunk ID 列表删除向量
     */
    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds.isEmpty()) return;
        String placeholders = chunkIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM rag_vectors WHERE chunk_id IN (" + placeholders + ")";
        int count = getJdbc().update(sql, chunkIds.toArray());
        log.debug("删除 chunk 向量：{} 条", count);
    }

    /**
     * 清空向量表（重建索引时使用）
     */
    public void truncateAll() {
        getJdbc().execute("TRUNCATE TABLE rag_vectors");
        log.info("向量表已清空");
    }

    /**
     * 相似度搜索
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回前 K 条
     * @param threshold      余弦相似度阈值 (0.0-1.0)
     * @return 搜索结果列表（按相似度降序）
     */
    public List<SearchResult> similaritySearch(float[] queryEmbedding, int topK, double threshold) {
        String sql = "SELECT v.chunk_id, v.doc_id, v.content, " +
                "1 - (v.embedding <=> ?::vector) AS score " +
                "FROM rag_vectors v " +
                "WHERE 1 - (v.embedding <=> ?::vector) > ? " +
                "ORDER BY score DESC " +
                "LIMIT ?";

        String queryVec = vectorToString(queryEmbedding);

        return getJdbc().query(sql,
                new Object[]{queryVec, queryVec, threshold, topK},
                (rs, rowNum) -> SearchResult.builder()
                        .chunkId(rs.getLong("chunk_id"))
                        .docId(rs.getLong("doc_id"))
                        .content(rs.getString("content"))
                        .score(rs.getDouble("score"))
                        .build());
    }

    /**
     * 将 float[] 转换为 pgvector 文本格式 '[0.1,0.2,0.3,...]'
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorRecord {
        private Long chunkId;
        private Long docId;
        private float[] embedding;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private Long chunkId;
        private Long docId;
        private String content;
        private Double score;
    }
}
