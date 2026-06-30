-- =============================================================================
-- kyle-e-commerce → ai_manager_admin 电商数据迁移
-- 00 清理目标库现有电商业务数据（保留 ec_system_config / ec_purchase_order_config）
-- 执行前请确认在本地环境；源库 `kyle-e-commerce` 不会被修改
-- =============================================================================
USE ai_manager_admin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM ec_sales_order_inventory_deduct;
DELETE FROM ec_sales_order_shortage;
DELETE FROM ec_order_import_row;
DELETE FROM ec_sales_order_line;
DELETE FROM ec_sales_order;
DELETE FROM ec_settlement_express_bill_line;
DELETE FROM ec_settlement_express_bill;
DELETE FROM ec_settlement_order_decision;
DELETE FROM ec_settlement_snapshot;
DELETE FROM ec_settlement_buyer_exclude;
DELETE FROM ec_outbound_order_line;
DELETE FROM ec_outbound_order;
DELETE FROM ec_inbound_order_line;
DELETE FROM ec_inbound_order;
DELETE FROM ec_stocktake_order_line;
DELETE FROM ec_stocktake_order;
DELETE FROM ec_inventory_log;
DELETE FROM ec_inventory;
DELETE FROM ec_listing_link_product;
DELETE FROM ec_listing_link_sku;
DELETE FROM ec_listing_link;
DELETE FROM ec_sku;
DELETE FROM ec_product;
DELETE FROM ec_carton;
DELETE FROM ec_express_notice;
DELETE FROM ec_express_price;
DELETE FROM ec_express_station;
DELETE FROM ec_factory;
DELETE FROM ec_shop;
DELETE FROM ec_platform;

DELETE FROM sys_import_batch WHERE biz_type = 'SALES_ORDER';

SET FOREIGN_KEY_CHECKS = 1;
