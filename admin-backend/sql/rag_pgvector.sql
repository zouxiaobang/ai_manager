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

-- IVFFlat 索引（加速余弦相似度搜索）
-- lists = 100 适合约 1 万条数据，数据量大时增大
CREATE INDEX IF NOT EXISTS idx_rag_vectors_embedding
    ON rag_vectors USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 常规索引
CREATE INDEX IF NOT EXISTS idx_vectors_chunk_id ON rag_vectors (chunk_id);
CREATE INDEX IF NOT EXISTS idx_vectors_doc_id ON rag_vectors (doc_id);
