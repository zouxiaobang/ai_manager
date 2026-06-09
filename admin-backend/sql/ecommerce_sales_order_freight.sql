-- 销售订单：试算运费字段（真实运费由月结快递账单按运单号回填 actual_freight_amount）
USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_sales_order'
      AND COLUMN_NAME = 'estimated_freight_amount'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ec_sales_order
        ADD COLUMN estimated_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''试算运费(元，按站点+省+重量)'' AFTER freight_amount,
        MODIFY COLUMN actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''真实运费(元，月结快递账单按运单号回填)''',
    'ALTER TABLE ec_sales_order
        MODIFY COLUMN actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''真实运费(元，月结快递账单按运单号回填)''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
