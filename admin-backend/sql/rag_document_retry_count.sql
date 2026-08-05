-- ============================================================
-- RAG 文档：处理重试次数（P0-2 异步化配套）
-- 在 ai_manager_admin 库执行；用于旧库升级（新库已内置该列）
-- 幂等：仅当列不存在时添加
-- ============================================================
USE ai_manager_admin;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rag_document' AND COLUMN_NAME = 'retry_count'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE rag_document ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT ''处理重试次数（达上限后启动不再自动重投，防止死循环）'' AFTER error_message',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
