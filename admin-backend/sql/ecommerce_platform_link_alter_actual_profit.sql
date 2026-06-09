-- 上架链接 SKU：新增真实设置金额、利润（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @has_actual_set_amount := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'actual_set_amount'
);
SET @sql_add_actual := IF(
    @has_actual_set_amount = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN actual_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''真实设置金额(元，可手动填写)'' AFTER cost_price',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_actual;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_profit := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'profit'
);
SET @sql_add_profit := IF(
    @has_profit = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN profit DECIMAL(12, 2) DEFAULT NULL COMMENT ''利润(元)=((真实设置金额-优惠券)×折扣)-成本'' AFTER actual_set_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_profit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
