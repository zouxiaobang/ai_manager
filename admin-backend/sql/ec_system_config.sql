-- 电商系统参数（键值 JSON，在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_system_config (
    config_key   VARCHAR(64)  NOT NULL COMMENT '配置键 inventory/order_import/express/delivery_note/company',
    config_json  TEXT         NOT NULL COMMENT '配置 JSON',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商系统参数';

INSERT INTO ec_system_config (config_key, config_json) VALUES
('inventory', '{"defaultAlertThreshold":10,"slowMovingDays":45,"slowMovingFallbackDays":90}'),
('order_import', '{"headerRow":1,"dataStartRow":2,"dateFormat":"yyyy-MM-dd HH:mm:ss"}'),
('order_import_status', '{"defaultLineStatus":"PAID","statusMapping":{"交易成功":"COMPLETED","交易关闭":"CANCELLED","确认收货":"COMPLETED","卖家已发货，等待买家确认":"SHIPPED","等待买家确认收货":"SHIPPED","卖家已发货":"SHIPPED","等待买家确认":"SHIPPED","买家已付款，等待卖家发货":"PAID","买家已付款":"PAID","等待卖家发货":"PAID","待发货":"PAID","已关闭":"CANCELLED","已发货":"SHIPPED","已完成":"COMPLETED","已退款":"REFUNDED","退款成功":"REFUNDED","部分退款":"PARTIAL_REFUND","退款中":"REFUNDED","退货退款":"RETURNED","已取消":"CANCELLED"}}'),
('express', '{"headerRow":1,"dataStartRow":2,"includeLabelPriceDefault":false}'),
('delivery_note', '{"title":"唯十嘉送货单","address":"","tel":"","preparedBy":"","shipFromName":"","shipFromPhone":"","shipFromAddress":"","requirementItems":[],"noteItems":[]}'),
('outbound_order', '{"title":"唯十嘉出库单","address":"","tel":"","preparedBy":"","approvedBy":"","warehouseKeeper":"","requirementItems":[],"noteItems":[]}'),
('settlement', '{"profitDisplayMode":"ACTUAL_PREFERRED","costIncludesFreight":true}'),
('rebate', '{"defaultRebatePct":0}'),
('notification', '{"inventoryAlertEnabled":true,"zeroStockAlertEnabled":true,"settlementRemindEnabled":true,"settlementRemindDayOfMonth":25}'),
('data_retention', '{"importHistoryRetentionDays":365,"inventoryLogRetentionDays":180,"autoCleanupEnabled":false}'),
('company', '{"companyName":"","address":"","tel":"","contactName":"","contactPhone":""}')
ON DUPLICATE KEY UPDATE config_key = config_key;
