-- ============================================================
-- 从 ec_order_import_* 迁移到 sys_import_*（已执行旧版 sales_order.sql 时使用）
-- ============================================================

USE ai_manager_admin;

-- 1. 若旧 profile 表存在且新表不存在，重命名并补列
SET @has_old_profile := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_order_import_profile'
);
SET @has_new_profile := (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_import_profile'
);

-- 先确保新表结构存在：请先执行 admin-backend/sql/sys_import.sql

-- 迁移 profile 数据
INSERT INTO sys_import_profile (
    id, name, biz_type, platform_id, scope_key, shop_id, file_type, header_row, data_start_row, sheet_name,
    column_mapping, value_mapping, extra_config, enabled, remark, deleted, create_time, update_time
)
SELECT
    p.id,
    p.name,
    'SALES_ORDER',
    ep.id,
    CONCAT('platform:', ep.id),
    NULL,
    p.file_type, p.header_row, p.data_start_row, p.sheet_name,
    p.column_mapping,
    p.line_status_mapping,
    JSON_OBJECT('defaultLineStatus', IFNULL(p.default_line_status, 'PAID'), 'platformCode', p.platform_code),
    p.enabled, p.remark, p.deleted, p.create_time, p.update_time
FROM ec_order_import_profile p
LEFT JOIN ec_platform ep ON ep.platform_code = p.platform_code
WHERE @has_old_profile > 0
  AND NOT EXISTS (SELECT 1 FROM sys_import_profile sp WHERE sp.id = p.id);

-- 2. 迁移 batch：shop_id -> biz_context
INSERT INTO sys_import_batch (
    id, batch_no, profile_id, biz_type, biz_context, file_name, file_path,
    source, status, total_rows, success_rows, failed_rows, unmatched_rows,
    error_summary, operator, committed_time, deleted, create_time, update_time
)
SELECT
    b.id, b.batch_no, b.profile_id, 'SALES_ORDER',
    JSON_OBJECT('shopId', b.shop_id),
    b.file_name, b.file_path, b.source, b.status,
    b.total_rows, b.success_rows, b.failed_rows, b.unmatched_rows,
    b.error_summary, b.operator, b.committed_time, b.deleted, b.create_time, b.update_time
FROM ec_order_import_batch b
WHERE EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_order_import_batch'
)
AND NOT EXISTS (SELECT 1 FROM sys_import_batch sb WHERE sb.id = b.id);

-- 3. ec_order_import_row.batch_id 仍指向同一 id，无需改行表

-- 4. 可选：确认迁移后删除旧表（手动执行）
-- DROP TABLE IF EXISTS ec_order_import_batch;
-- DROP TABLE IF EXISTS ec_order_import_profile;
