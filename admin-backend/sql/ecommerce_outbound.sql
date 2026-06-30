-- 出货单（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_outbound_order (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出货单主键',
    order_no    VARCHAR(32)  NOT NULL COMMENT '出货单号',
    factory_id  BIGINT       DEFAULT NULL COMMENT '所属工厂',
    customer_factory_id BIGINT DEFAULT NULL COMMENT '客户工厂 ID',
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    remark      VARCHAR(512) DEFAULT NULL COMMENT '备注',
    order_time  DATETIME     DEFAULT NULL COMMENT '创单时间',
    expected_ship_time DATETIME DEFAULT NULL COMMENT '预出货时间',
    actual_ship_time DATETIME DEFAULT NULL COMMENT '实际出货时间',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_outbound_order_no (order_no),
    KEY idx_ec_outbound_order_status (status),
    KEY idx_ec_outbound_order_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商出货单';

CREATE TABLE IF NOT EXISTS ec_outbound_order_line (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id    BIGINT      NOT NULL COMMENT '出货单 ID',
    sku_code    VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    quantity    INT         NOT NULL COMMENT '计划出货数量',
    shipped_quantity INT   DEFAULT NULL COMMENT '实际出货数量',
    deleted     TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_outbound_order_line_order (order_id),
    KEY idx_ec_outbound_order_line_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商出货单明细';
