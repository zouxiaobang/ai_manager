-- 月结快递账单：明细入库、快递公司、列映射配置
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN express_station_id BIGINT       DEFAULT NULL COMMENT '快递站点 ID' AFTER bill_month,
    ADD COLUMN column_mapping   TEXT           DEFAULT NULL COMMENT '列映射 JSON 快照',
    ADD COLUMN header_row       INT            NOT NULL DEFAULT 1 COMMENT '表头行号(1-based)',
    ADD COLUMN data_start_row   INT            NOT NULL DEFAULT 2 COMMENT '数据起始行(1-based)',
    ADD COLUMN import_mode      VARCHAR(16)    NOT NULL DEFAULT 'FILE' COMMENT 'FILE/MANUAL/MIXED',
    ADD COLUMN gap_order_rows   INT            NOT NULL DEFAULT 0 COMMENT '未匹配发货/完成订单数',
    ADD COLUMN manual_applied_rows INT         NOT NULL DEFAULT 0 COMMENT '手动补录并应用条数',
    ADD KEY idx_settlement_express_bill_station (express_station_id);

CREATE TABLE IF NOT EXISTS ec_settlement_express_bill_line (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_id            BIGINT         NOT NULL COMMENT '批次 ID',
    source             VARCHAR(16)    NOT NULL COMMENT 'FILE/GAP_ORDER/MANUAL',
    order_id           BIGINT         DEFAULT NULL COMMENT '匹配订单 ID',
    platform_order_no  VARCHAR(64)    DEFAULT NULL COMMENT '平台订单号',
    order_no           VARCHAR(64)    DEFAULT NULL COMMENT '系统订单号',
    tracking_number    VARCHAR(128)   DEFAULT NULL COMMENT '运单号',
    freight_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '运费',
    match_status       VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT 'MATCHED/UNMATCHED/PENDING/APPLIED',
    remark             VARCHAR(256)   DEFAULT NULL COMMENT '备注',
    deleted            TINYINT        NOT NULL DEFAULT 0,
    create_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_express_bill_line_bill (bill_id),
    KEY idx_express_bill_line_order (order_id),
    KEY idx_express_bill_line_tracking (tracking_number(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结快递账单明细';

-- 列映射配置统一使用 sys_import_profile（biz_type=SETTLEMENT_EXPRESS_BILL, scope_key=express_station:{id}）
-- 若曾创建 ec_settlement_express_bill_profile，请执行 ec_settlement_express_bill_profile_to_sys_import.sql
