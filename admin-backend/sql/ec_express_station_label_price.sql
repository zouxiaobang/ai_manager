-- 快递站点：面单价格；账单批次：是否叠加面单价格
USE ai_manager_admin;

ALTER TABLE ec_express_station
    ADD COLUMN label_price DECIMAL(10, 2) DEFAULT NULL COMMENT '面单价格(元/单)' AFTER address;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN include_label_price TINYINT NOT NULL DEFAULT 0 COMMENT '1=运费叠加面单价格' AFTER express_station_id;
