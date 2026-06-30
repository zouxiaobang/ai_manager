-- =============================================================================
-- 06 重置自增 ID（在显式插入 id 后执行）
-- =============================================================================
USE ai_manager_admin;

ALTER TABLE ec_platform AUTO_INCREMENT = 100;
ALTER TABLE ec_shop AUTO_INCREMENT = 100;
ALTER TABLE ec_factory AUTO_INCREMENT = 20000;
ALTER TABLE ec_carton AUTO_INCREMENT = 100;
ALTER TABLE ec_product AUTO_INCREMENT = 500;
ALTER TABLE ec_sku AUTO_INCREMENT = 500;
ALTER TABLE ec_inventory AUTO_INCREMENT = 500;
ALTER TABLE ec_express_station AUTO_INCREMENT = 100;
ALTER TABLE ec_express_price AUTO_INCREMENT = 500;
ALTER TABLE ec_express_notice AUTO_INCREMENT = 100;
ALTER TABLE ec_listing_link AUTO_INCREMENT = 100;
ALTER TABLE ec_listing_link_sku AUTO_INCREMENT = 500;
ALTER TABLE ec_listing_link_product AUTO_INCREMENT = 1000;
ALTER TABLE ec_inbound_order AUTO_INCREMENT = 10000;
ALTER TABLE ec_inbound_order_line AUTO_INCREMENT = 10000;
ALTER TABLE ec_sales_order AUTO_INCREMENT = 100000;
ALTER TABLE ec_sales_order_line AUTO_INCREMENT = 100000;
ALTER TABLE ec_settlement_order_decision AUTO_INCREMENT = 10000;
ALTER TABLE ec_settlement_snapshot AUTO_INCREMENT = 100;
