-- ec_sku 追加 carton_id（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'carton_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN carton_id BIGINT DEFAULT NULL COMMENT ''匹配纸箱'' AFTER image_name, ADD KEY idx_ec_sku_carton (carton_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
