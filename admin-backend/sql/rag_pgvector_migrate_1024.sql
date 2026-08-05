-- ============================================================
-- RAG 向量表维度迁移：VECTOR(1536) → VECTOR(1024)（一次性）
-- 在 ai_manager_rag 库执行
-- 背景：rag_vectors.embedding 原按 OpenAI（text-embedding-3-small，1536 维）建表，
--       但当前嵌入配置为 qwen text-embedding-v3（原生 1024 维），向量存不进去。
--       迁移到 1024 维并统一通过 embeddings 请求的 dimensions=1024 对齐各提供商。
-- ============================================================

-- 1. 清空旧向量（1536 维向量无法转换到 1024 维，且与当前嵌入模型不兼容；
--    迁移后通过应用「重建索引」重新嵌入已有文档）
TRUNCATE TABLE rag_vectors;

-- 2. 改列维度（会失效旧 HNSW 索引）
ALTER TABLE rag_vectors ALTER COLUMN embedding TYPE vector(1024);

-- 3. 重建 HNSW 索引（与原 rag_pgvector.sql 保持一致）
DROP INDEX IF EXISTS idx_rag_vectors_embedding;
CREATE INDEX idx_rag_vectors_embedding
    ON rag_vectors USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
