-- =============================================================================
-- 07 迁移结果校验（只读）
-- =============================================================================
USE ai_manager_admin;

SELECT 'ec_platform' AS tbl, COUNT(*) AS cnt FROM ec_platform
UNION ALL SELECT 'ec_shop', COUNT(*) FROM ec_shop
UNION ALL SELECT 'ec_factory', COUNT(*) FROM ec_factory
UNION ALL SELECT 'ec_carton', COUNT(*) FROM ec_carton
UNION ALL SELECT 'ec_product', COUNT(*) FROM ec_product
UNION ALL SELECT 'ec_sku', COUNT(*) FROM ec_sku
UNION ALL SELECT 'ec_inventory', COUNT(*) FROM ec_inventory
UNION ALL SELECT 'ec_listing_link', COUNT(*) FROM ec_listing_link
UNION ALL SELECT 'ec_listing_link_sku', COUNT(*) FROM ec_listing_link_sku
UNION ALL SELECT 'ec_listing_link_product', COUNT(*) FROM ec_listing_link_product
UNION ALL SELECT 'ec_sales_order', COUNT(*) FROM ec_sales_order
UNION ALL SELECT 'ec_sales_order_line', COUNT(*) FROM ec_sales_order_line
UNION ALL SELECT 'ec_inbound_order', COUNT(*) FROM ec_inbound_order
UNION ALL SELECT 'ec_inbound_order_line', COUNT(*) FROM ec_inbound_order_line
UNION ALL SELECT 'ec_settlement_order_decision', COUNT(*) FROM ec_settlement_order_decision
UNION ALL SELECT 'ec_settlement_snapshot', COUNT(*) FROM ec_settlement_snapshot;

-- 订单明细匹配上架 SKU 比例
SELECT
    SUM(CASE WHEN listing_link_sku_id IS NOT NULL THEN 1 ELSE 0 END) AS matched_lines,
    COUNT(*) AS total_lines
FROM ec_sales_order_line;

-- 抽样
SELECT id, order_no, platform_order_no, shop_id, received_amount, status
FROM ec_sales_order
ORDER BY id
LIMIT 5;
