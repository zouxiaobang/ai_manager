-- 上架链接模块增强（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- 店铺默认收货省（快递试算）
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_shop' AND COLUMN_NAME = 'default_receive_province'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_shop ADD COLUMN default_receive_province VARCHAR(64) DEFAULT ''广东省'' COMMENT ''默认收货省份(快递试算)'' AFTER other_fee_remark',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ec_shop SET default_receive_province = '广东省'
WHERE default_receive_province IS NULL OR default_receive_province = '';

-- 链接：平台 URL、关联 SPU
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND COLUMN_NAME = 'platform_url'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link ADD COLUMN platform_url VARCHAR(1024) DEFAULT NULL COMMENT ''平台商品链接URL'' AFTER shop_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND COLUMN_NAME = 'product_id'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link ADD COLUMN product_id BIGINT DEFAULT NULL COMMENT ''关联商品SPU ID'' AFTER platform_url',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND INDEX_NAME = 'idx_ec_listing_link_product'
);
SET @sql := IF(@idx = 0,
    'ALTER TABLE ec_listing_link ADD KEY idx_ec_listing_link_product (product_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- SKU 行：基础成本、平台费（盈亏平衡口径）
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link_sku' AND COLUMN_NAME = 'base_cost_amount'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN base_cost_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''基础成本=SKU+纸箱+快递'' AFTER cost_price',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link_sku' AND COLUMN_NAME = 'platform_fee_amount'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN platform_fee_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''平台费(盈亏平衡口径)'' AFTER base_cost_amount',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 演示：马克杯链接关联 SPU id=1
UPDATE ec_listing_link SET product_id = 1 WHERE id = 1 AND product_id IS NULL;

-- 已有 cost_price 需按新公式重算：部署后调用 POST /api/ecommerce/listing-links/recalculate-all
-- 或等待定时任务 EcListingLinkPricingRecalcJob 执行
