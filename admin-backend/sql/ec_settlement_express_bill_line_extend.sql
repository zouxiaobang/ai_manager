-- 快递账单明细：结算目的地、重量、发货时间
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill_line
    ADD COLUMN settlement_destination VARCHAR(128) DEFAULT NULL COMMENT '结算目的地' AFTER freight_amount,
    ADD COLUMN weight               DECIMAL(10, 3) DEFAULT NULL COMMENT '重量(kg)' AFTER settlement_destination,
    ADD COLUMN ship_time            DATETIME       DEFAULT NULL COMMENT '发货时间' AFTER weight;
