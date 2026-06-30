-- 出货单关联客户工厂（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_outbound_order' AND COLUMN_NAME = 'customer_factory_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_outbound_order ADD COLUMN customer_factory_id BIGINT DEFAULT NULL COMMENT ''客户工厂 ID'' AFTER factory_id, ADD KEY idx_ec_outbound_order_customer (customer_factory_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
