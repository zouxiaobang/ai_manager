-- ============================================================
-- RAG 知识库 - 本地 dev 独立向量库（与生产 ai_manager_rag 隔离）
--
-- 背景：本地 dev 与生产 prod 此前共用 118 上同一个 PG 向量库
-- （application.yml 默认 PGVECTOR_DATABASE=ai_manager_rag）。本库将
-- 本地向量数据隔离到 ai_manager_rag_dev，避免本地上传/重建索引
-- 污染生产检索。生产库 ai_manager_rag 不受影响。
--
-- 执行：需 PG 超级用户（ai_manager 无 CREATEDB）。在 118 上用 psql：
--   sudo -u postgres psql -f rag_pgvector_dev.sql
-- ============================================================

CREATE DATABASE ai_manager_rag_dev;

\connect ai_manager_rag_dev

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 向量索引表（与生产 ai_manager_rag.rag_vectors 同构，维度 1024）
CREATE TABLE IF NOT EXISTS rag_vectors (
    id          BIGSERIAL PRIMARY KEY,
    chunk_id    BIGINT       NOT NULL,
    doc_id      BIGINT       NOT NULL,
    embedding   VECTOR(1024) NOT NULL,
    content     TEXT         NOT NULL
);

-- HNSW 索引（与生产一致的构建参数）
DROP INDEX IF EXISTS idx_rag_vectors_embedding;
CREATE INDEX idx_rag_vectors_embedding
    ON rag_vectors USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_vectors_chunk_id ON rag_vectors (chunk_id);
CREATE INDEX IF NOT EXISTS idx_vectors_doc_id ON rag_vectors (doc_id);

-- 授权应用账号 ai_manager：表 owner 含序列权限，保证 BIGSERIAL 自增可用
ALTER TABLE rag_vectors OWNER TO ai_manager;
GRANT USAGE ON SCHEMA public TO ai_manager;
