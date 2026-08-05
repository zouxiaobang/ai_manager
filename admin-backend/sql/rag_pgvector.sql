-- ============================================================
-- RAG 知识库 - PostgreSQL pgvector 建表语句
-- 在 ai_manager_rag 库执行
-- ============================================================

-- 启用 pgvector 扩展（首次需要，已启用则跳过）
CREATE EXTENSION IF NOT EXISTS vector;

-- 向量索引表
CREATE TABLE IF NOT EXISTS rag_vectors (
    id          BIGSERIAL PRIMARY KEY,
    chunk_id    BIGINT       NOT NULL,
    doc_id      BIGINT       NOT NULL,
    embedding   VECTOR(1536) NOT NULL,
    content     TEXT         NOT NULL
);

-- HNSW 索引（加速余弦相似度搜索；无需训练、无空表限制，小数据集构建与召回优于 IVFFlat）
-- 存量库升级：已存在旧 IVFFlat 索引时，下方 DROP + CREATE 会先删旧索引再建 HNSW（幂等，可重复执行）
DROP INDEX IF EXISTS idx_rag_vectors_embedding;
CREATE INDEX idx_rag_vectors_embedding
    ON rag_vectors USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- 常规索引
CREATE INDEX IF NOT EXISTS idx_vectors_chunk_id ON rag_vectors (chunk_id);
CREATE INDEX IF NOT EXISTS idx_vectors_doc_id ON rag_vectors (doc_id);
