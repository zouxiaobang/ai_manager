-- 已有 ec_product / ec_sku 表时追加工厂表与 factory_id（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_factory (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工厂主键',
    name          VARCHAR(128) NOT NULL COMMENT '工厂名称',
    contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(64)  DEFAULT NULL COMMENT '联系方式',
    address       VARCHAR(512) DEFAULT NULL COMMENT '地址',
    remark        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_factory_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商工厂';

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'factory_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_product ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT ''所属工厂'' AFTER id, ADD KEY idx_ec_product_factory (factory_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 演示数据见 ecommerce_product.sql 末尾 INSERT 语句，或执行：
-- mysql ... < admin-backend/sql/ecommerce_seed.sql
