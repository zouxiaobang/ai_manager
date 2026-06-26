-- =============================================================================
-- 编码规范（admin-backend/sql 目录下所有 .sql 文件均适用）
--   文件保存：UTF-8（无 BOM）
--   库/表：    utf8mb4 + utf8mb4_unicode_ci
--   客户端：   mysql ... --default-character-set=utf8mb4
--   JDBC：     characterEncoding=UTF-8 & connectionCollation=utf8mb4_unicode_ci
-- =============================================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
