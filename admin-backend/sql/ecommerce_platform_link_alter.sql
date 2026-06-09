-- 上架链接 SKU：展示金额改名为最低设置金额，并新增成本价格（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- 已有 display_price 列时重命名为 min_set_amount
SET @has_display_price := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'display_price'
);
SET @sql_rename := IF(
    @has_display_price > 0,
    'ALTER TABLE ec_listing_link_sku CHANGE COLUMN display_price min_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''最低设置金额(元)''',
    'SELECT 1'
);
PREPARE stmt FROM @sql_rename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新建表场景直接确保列存在
SET @has_min_set_amount := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'min_set_amount'
);
SET @sql_add_min := IF(
    @has_min_set_amount = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN min_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''最低设置金额(元)'' AFTER coupon_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_min;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_cost_price := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'cost_price'
);
SET @sql_add_cost := IF(
    @has_cost_price = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN cost_price DECIMAL(12, 2) DEFAULT NULL COMMENT ''成本价格(元)=SKU售价+纸箱+快递'' AFTER min_set_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_cost;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 若已执行过上一版 alter，请继续执行：
-- 1. ecommerce_platform_link_data_backfill.sql（回填 cost_price / min_set_amount）
-- 2. ecommerce_platform_link_alter_actual_profit.sql（新增真实设置金额、利润）
