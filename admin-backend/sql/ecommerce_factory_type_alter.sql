-- 工厂类型：PRODUCTION 生产 / CUSTOMER 客户（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_factory' AND COLUMN_NAME = 'factory_type'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_factory ADD COLUMN factory_type VARCHAR(16) NOT NULL DEFAULT ''PRODUCTION'' COMMENT ''PRODUCTION/CUSTOMER'' AFTER name, ADD KEY idx_ec_factory_type (factory_type)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
