-- 快递账单：明细快递公司、其他快递公司标记
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN other_express TINYINT NOT NULL DEFAULT 0 COMMENT '1=其他快递公司（未匹配系统站点）' AFTER express_station_id;

ALTER TABLE ec_settlement_express_bill_line
    ADD COLUMN express_station_id BIGINT DEFAULT NULL COMMENT '快递公司 ID' AFTER bill_id,
    ADD KEY idx_express_bill_line_station (express_station_id);
