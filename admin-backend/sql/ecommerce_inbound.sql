-- 进货单 + 库存日志扩展（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_inbound_order (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '进货单主键',
    order_no    VARCHAR(32)  NOT NULL COMMENT '进货单号',
    factory_id  BIGINT       DEFAULT NULL COMMENT '所属工厂',
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    remark      VARCHAR(512) DEFAULT NULL COMMENT '备注',
    order_time  DATETIME     DEFAULT NULL COMMENT '下单时间',
    expected_delivery_time DATETIME DEFAULT NULL COMMENT '预收货时间',
    actual_receipt_time DATETIME DEFAULT NULL COMMENT '实际收货时间',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_inbound_order_no (order_no),
    KEY idx_ec_inbound_order_status (status),
    KEY idx_ec_inbound_order_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商进货单';

CREATE TABLE IF NOT EXISTS ec_inbound_order_line (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id    BIGINT      NOT NULL COMMENT '进货单 ID',
    sku_code    VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    quantity    INT         NOT NULL COMMENT '下单数量',
    received_quantity INT   DEFAULT NULL COMMENT '实际收货数量',
    deleted     TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_inbound_order_line_order (order_id),
    KEY idx_ec_inbound_order_line_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商进货单明细';

-- 库存操作记录扩展（关联进货单等，重复执行若列已存在会报错可忽略）
ALTER TABLE ec_inventory_log
    ADD COLUMN ref_type VARCHAR(32) DEFAULT NULL COMMENT '关联类型 INBOUND_ORDER 等' AFTER change_qty,
    ADD COLUMN ref_id BIGINT DEFAULT NULL COMMENT '关联业务 ID' AFTER ref_type,
    ADD COLUMN remark VARCHAR(512) DEFAULT NULL COMMENT '备注' AFTER ref_id;
