-- 导入行：未匹配链接时可填手动成本（元/套）
USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_order_import_row'
      AND COLUMN_NAME = 'manual_cost_price'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ec_order_import_row
        ADD COLUMN manual_cost_price DECIMAL(12, 2) DEFAULT NULL
            COMMENT ''手动成本(元/套，未匹配链接时使用)'' AFTER listing_link_sku_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
