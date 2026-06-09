-- 月结统计：买家排除配置、订单纳入决策、快递账单导入记录
USE ai_manager_admin;

-- 统计时排除的买家昵称（可按店铺，shop_id 为空表示全平台生效）
CREATE TABLE IF NOT EXISTS ec_settlement_buyer_exclude (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id     BIGINT       DEFAULT NULL COMMENT '店铺 ID，空=全部店铺',
    buyer_name  VARCHAR(128) NOT NULL COMMENT '买家昵称（精确匹配 trim）',
    remark      VARCHAR(256) DEFAULT NULL COMMENT '备注',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_settlement_buyer_exclude_shop (shop_id),
    KEY idx_settlement_buyer_exclude_name (buyer_name(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结统计-买家排除';

-- 待确认订单的人工纳入决策（按自然月 YYYY-MM）
CREATE TABLE IF NOT EXISTS ec_settlement_order_decision (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id          BIGINT      NOT NULL COMMENT '店铺 ID',
    order_id         BIGINT      NOT NULL COMMENT '销售订单 ID',
    settlement_month CHAR(7)     NOT NULL COMMENT '统计月份 YYYY-MM',
    included         TINYINT     NOT NULL DEFAULT 0 COMMENT '1纳入 0不纳入',
    deleted          TINYINT     NOT NULL DEFAULT 0,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_order_month (order_id, settlement_month),
    KEY idx_settlement_decision_shop_month (shop_id, settlement_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结统计-订单纳入决策';

-- 月结快递账单导入批次（按运单号回填 actual_freight_amount）
CREATE TABLE IF NOT EXISTS ec_settlement_express_bill (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_month    CHAR(7)       NOT NULL COMMENT '账单月份 YYYY-MM',
    file_name     VARCHAR(256)  DEFAULT NULL COMMENT '上传文件名',
    total_rows    INT           NOT NULL DEFAULT 0,
    matched_rows  INT           NOT NULL DEFAULT 0,
    unmatched_rows INT          NOT NULL DEFAULT 0,
    status        VARCHAR(16)   NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED/FAILED',
    error_message VARCHAR(512)  DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_settlement_express_bill_month (bill_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结快递账单导入批次';
