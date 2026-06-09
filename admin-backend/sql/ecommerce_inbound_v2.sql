-- 进货单时间字段 + 明细实收数量（在 ai_manager_admin 库执行，列已存在可忽略报错）
USE ai_manager_admin;

ALTER TABLE ec_inbound_order
    ADD COLUMN order_time DATETIME DEFAULT NULL COMMENT '下单时间' AFTER remark,
    ADD COLUMN expected_delivery_time DATETIME DEFAULT NULL COMMENT '预收货时间' AFTER order_time,
    ADD COLUMN actual_receipt_time DATETIME DEFAULT NULL COMMENT '实际收货时间' AFTER expected_delivery_time;

ALTER TABLE ec_inbound_order_line
    ADD COLUMN received_quantity INT DEFAULT NULL COMMENT '实际收货数量' AFTER quantity;
