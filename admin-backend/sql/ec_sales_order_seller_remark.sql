-- 订单头卖家备注（若库中尚无该列则执行）
USE ai_manager_admin;

ALTER TABLE ec_sales_order
    ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(512) DEFAULT NULL COMMENT '买家留言' AFTER tracking_number;

ALTER TABLE ec_sales_order
    ADD COLUMN IF NOT EXISTS seller_remark VARCHAR(512) DEFAULT NULL COMMENT '卖家备注' AFTER buyer_remark;
