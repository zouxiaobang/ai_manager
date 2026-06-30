-- =============================================================================
-- kyle-e-commerce → ai_manager_admin 一键迁移（本地执行）
-- 用法：mysql -u root -p < run_all.sql
-- 依赖：本机同时存在库 `kyle-e-commerce` 与 `ai_manager_admin`
-- =============================================================================

SOURCE 00_cleanup.sql;
SOURCE 01_master_data.sql;
SOURCE 02_products_listing.sql;
SOURCE 03_sales_orders.sql;
SOURCE 04_inbound.sql;
SOURCE 05_settlement.sql;
SOURCE 06_reset_auto_increment.sql;
SOURCE 08_product_image_from_first_sku.sql;
SOURCE 07_verify.sql;
