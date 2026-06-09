-- 上架链接关联多个商品 SPU（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_listing_link_product (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    link_id     BIGINT   NOT NULL COMMENT '上架链接 ID',
    product_id  BIGINT   NOT NULL COMMENT '商品 SPU ID',
    sort_order  INT      NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_listing_link_product (link_id, product_id),
    KEY idx_ec_listing_link_product_link (link_id),
    KEY idx_ec_listing_link_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架链接关联商品';

-- 从 ec_listing_link.product_id 迁移（若列存在）
SET @has_product_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND COLUMN_NAME = 'product_id'
);
SET @sql_migrate := IF(
    @has_product_id > 0,
    'INSERT INTO ec_listing_link_product (link_id, product_id, sort_order)
     SELECT l.id, l.product_id, 0
     FROM ec_listing_link l
     WHERE l.product_id IS NOT NULL AND l.deleted = 0
       AND NOT EXISTS (
         SELECT 1 FROM ec_listing_link_product lp
         WHERE lp.link_id = l.id AND lp.product_id = l.product_id AND lp.deleted = 0
       )',
    'SELECT 1'
);
PREPARE stmt FROM @sql_migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 可选：移除旧单列（新环境可跳过）
SET @sql_drop := IF(
    @has_product_id > 0,
    'ALTER TABLE ec_listing_link DROP COLUMN product_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND INDEX_NAME = 'idx_ec_listing_link_product'
);
SET @sql_drop_idx := IF(
    @idx > 0,
    'ALTER TABLE ec_listing_link DROP INDEX idx_ec_listing_link_product',
    'SELECT 1'
);
PREPARE stmt FROM @sql_drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO ec_listing_link_product (link_id, product_id, sort_order)
SELECT 1, 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM ec_listing_link WHERE id = 1)
  AND NOT EXISTS (
    SELECT 1 FROM ec_listing_link_product WHERE link_id = 1 AND product_id = 1
  );
