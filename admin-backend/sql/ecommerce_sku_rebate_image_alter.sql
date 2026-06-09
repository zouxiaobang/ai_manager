-- 已有 ec_product / ec_sku 表时追加 SKU 退点、图片字段，SPU 图片字段（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- ec_product: main_image -> image_name，或新增 image_name
SET @main_image_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'main_image'
);
SET @image_name_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'image_name'
);
SET @sql = IF(@main_image_exists = 1 AND @image_name_exists = 0,
    'ALTER TABLE ec_product CHANGE COLUMN main_image image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名''',
    IF(@image_name_exists = 0,
        'ALTER TABLE ec_product ADD COLUMN image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名'' AFTER rebate_pct',
        'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'rebate_pct'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN rebate_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT ''退点(百分比，计算以 SKU 为准)'' AFTER spec_name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'image_name'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名'' AFTER rebate_pct',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有 SKU 退点默认继承所属 SPU
UPDATE ec_sku s
INNER JOIN ec_product p ON s.product_id = p.id
SET s.rebate_pct = p.rebate_pct
WHERE s.rebate_pct = 0 OR s.rebate_pct IS NULL;
