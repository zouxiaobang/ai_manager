-- 销售订单 MVP v2（在 ai_manager_admin 库执行）
--
-- 约定：
--   1. Excel 导入时用「链接名称 + SKU展示名称」在内存中匹配，入库后明细只存 listing_link_sku_id
--      匹配规则：trim(excel.链接名称) = ec_listing_link.name
--               trim(excel.SKU规格名称) = ec_listing_link_sku.sku_name（同 shop_id 内）
--   2. 订单状态以明细为准；订单头 status 由明细聚合（支持部分退款/部分发货）
--   3. 发货扣库存方案 B：扣至 0，欠货写入 ec_sales_order_shortage
--   4. 订单头 express_station_id 关联快递站点，用于后期按收货省试算/核对真实运费
--   5. 已发货退款/退货退款：不回收库存，按行记 loss_amount 亏损
USE ai_manager_admin;

-- ========== 1. 销售订单头 ==========
CREATE TABLE IF NOT EXISTS ec_sales_order (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '销售订单主键',
    order_no              VARCHAR(32)   NOT NULL COMMENT '系统订单号 SOyyyyMMddxxxx',
    shop_id               BIGINT        NOT NULL COMMENT '所属店铺 ID',
    platform_order_no     VARCHAR(64)   DEFAULT NULL COMMENT '平台订单号（导入去重键）',
    source                VARCHAR(16)   NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL/IMPORT/API',
    -- 聚合状态（由明细同步，见文末状态说明）
    status                VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PAID/PARTIAL_SHIPPED/SHIPPED/PARTIAL_REFUND/COMPLETED/CANCELLED/REFUNDED',
    platform_status       VARCHAR(64)   DEFAULT NULL COMMENT '平台原始状态文案',
    express_station_id    BIGINT        DEFAULT NULL COMMENT '快递站点 ID，关联 ec_express_station.id，用于计算真实运费',
    order_time            DATETIME      NOT NULL COMMENT '下单时间',
    pay_time              DATETIME      DEFAULT NULL COMMENT '支付时间',
    ship_time             DATETIME      DEFAULT NULL COMMENT '首条明细发货时间',
    complete_time         DATETIME      DEFAULT NULL COMMENT '订单完成时间',
    buyer_name            VARCHAR(128)  DEFAULT NULL COMMENT '买家昵称/姓名',
    buyer_phone           VARCHAR(32)   DEFAULT NULL COMMENT '买家电话',
    receive_province      VARCHAR(64)   DEFAULT NULL COMMENT '收货省(由 receive_address 自动解析)',
    receive_city          VARCHAR(64)   DEFAULT NULL COMMENT '收货市',
    receive_district      VARCHAR(64)   DEFAULT NULL COMMENT '收货区',
    receive_address       VARCHAR(512)  DEFAULT NULL COMMENT '详细地址',
    tracking_number       VARCHAR(64)   DEFAULT NULL COMMENT '快递单号',
    buyer_remark          VARCHAR(512)  DEFAULT NULL COMMENT '买家留言',
    seller_remark         VARCHAR(512)  DEFAULT NULL COMMENT '卖家备注',
    received_amount       DECIMAL(12, 2) DEFAULT NULL COMMENT '订单实收金额(元)',
    total_cost_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '订单总成本(元，明细汇总快照)',
    freight_amount        DECIMAL(12, 2) DEFAULT NULL COMMENT '买家付运费(元)',
    estimated_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '试算运费(元，按站点+省+重量)',
    actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '真实运费(元，月结快递账单按运单号回填)',
    order_coupon_amount   DECIMAL(12, 2) DEFAULT NULL COMMENT '订单级优惠券(元)',
    platform_fee_amount   DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费合计快照(元)',
    profit_amount         DECIMAL(12, 2) DEFAULT NULL COMMENT '利润合计快照(元，含退款亏损后)',
    total_loss_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '退款亏损合计(元)',
    has_shortage          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否存在欠货 1是 0否',
    import_batch_id       BIGINT        DEFAULT NULL COMMENT '关联导入批次 ID',
    platform_raw_json     JSON          DEFAULT NULL COMMENT '平台/导入原始 JSON 快照',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_sales_order_no (order_no),
    UNIQUE KEY uk_ec_sales_order_platform (shop_id, platform_order_no),
    KEY idx_ec_sales_order_shop (shop_id),
    KEY idx_ec_sales_order_status (status),
    KEY idx_ec_sales_order_order_time (order_time),
    KEY idx_ec_sales_order_express_station (express_station_id),
    KEY idx_ec_sales_order_tracking (tracking_number),
    KEY idx_ec_sales_order_import_batch (import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商销售订单';

-- ========== 2. 销售订单明细 ==========
-- 不在明细表存 Excel 链接名/规格名；导入匹配后只填 listing_link_sku_id，并快照展示名称等供展示与计算。
CREATE TABLE IF NOT EXISTS ec_sales_order_line (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id                BIGINT        NOT NULL COMMENT '销售订单 ID',
    sort_order              INT           NOT NULL DEFAULT 0 COMMENT '排序',
    listing_link_sku_id     BIGINT        DEFAULT NULL COMMENT '上架链接 SKU ID（匹配 ec_listing_link_sku.id）',
    link_name               VARCHAR(256)  DEFAULT NULL COMMENT '链接名称快照 ec_listing_link.name',
    sku_spec_name           VARCHAR(256)  DEFAULT NULL COMMENT 'SKU展示名称快照 ec_listing_link_sku.sku_name',
    sku_codes               VARCHAR(1024) DEFAULT NULL COMMENT '货号快照，逗号分隔',
    sku_quantity            INT           NOT NULL DEFAULT 1 COMMENT 'SKU数量(链接SKU套数)',
    shipped_quantity        INT           NOT NULL DEFAULT 0 COMMENT '已发货套数',
    short_quantity          INT           NOT NULL DEFAULT 0 COMMENT '欠货套数',
    -- 明细状态（退款/发货以行为单位）
    status                  VARCHAR(16)   NOT NULL DEFAULT 'PAID' COMMENT 'PAID/SHIPPED/COMPLETED/CANCELLED/PARTIAL_REFUND/REFUNDED/RETURNED',
    platform_line_status    VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单原始状态',
    refund_type             VARCHAR(16)   DEFAULT NULL COMMENT '退款类型 NONE/REFUND_ONLY已发货退款/RETURN_REFUND退货退款',
    refund_time             DATETIME      DEFAULT NULL COMMENT '退款/退货时间',
    refund_amount           DECIMAL(12, 2) DEFAULT NULL COMMENT '退款金额(元)',
    loss_amount             DECIMAL(12, 2) DEFAULT NULL COMMENT '亏损金额(元)，已发货退款/退货不退库存，按成本记亏',
    unit_price              DECIMAL(12, 2) DEFAULT NULL COMMENT '成交单价(元/套)',
    discount_pct            DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣折数快照(90=9折)',
    line_coupon_amount      DECIMAL(12, 2) DEFAULT NULL COMMENT '行级优惠券(元)',
    line_received_amount    DECIMAL(12, 2) DEFAULT NULL COMMENT '行实收金额(元)',
    sku_amount              DECIMAL(12, 2) DEFAULT NULL COMMENT 'SKU售价合计快照',
    carton_amount           DECIMAL(12, 2) DEFAULT NULL COMMENT '纸箱成本快照',
    express_amount          DECIMAL(12, 2) DEFAULT NULL COMMENT '试算快递成本快照',
    base_cost_amount        DECIMAL(12, 2) DEFAULT NULL COMMENT '基础成本快照',
    platform_fee_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费快照',
    cost_price              DECIMAL(12, 2) DEFAULT NULL COMMENT '行总成本快照(含平台费盈亏平衡口径)',
    min_set_amount          DECIMAL(12, 2) DEFAULT NULL COMMENT '最低设置金额快照',
    profit                  DECIMAL(12, 2) DEFAULT NULL COMMENT '行利润快照',
    pricing_risk            VARCHAR(16)   DEFAULT NULL COMMENT 'OK/BELOW_MIN/NEGATIVE_PROFIT',
    platform_line_no        VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单号',
    platform_item_name      VARCHAR(512)  DEFAULT NULL COMMENT '平台商品标题快照',
    deleted                 TINYINT       NOT NULL DEFAULT 0,
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_line_order (order_id),
    KEY idx_ec_sales_order_line_link_sku (listing_link_sku_id),
    KEY idx_ec_sales_order_line_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商销售订单明细';

-- ========== 3. 发货欠货明细（方案 B） ==========
CREATE TABLE IF NOT EXISTS ec_sales_order_shortage (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '欠货主键',
    order_id            BIGINT       NOT NULL COMMENT '销售订单 ID',
    order_line_id       BIGINT       NOT NULL COMMENT '订单明细 ID',
    sku_code            VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    need_qty            INT          NOT NULL COMMENT '应扣数量',
    deducted_qty        INT          NOT NULL DEFAULT 0 COMMENT '实扣数量',
    short_qty           INT          NOT NULL DEFAULT 0 COMMENT '欠货数量',
    status              VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLEARED',
    cleared_qty         INT          NOT NULL DEFAULT 0 COMMENT '已核销数量',
    cleared_ref_type    VARCHAR(32)  DEFAULT NULL COMMENT '核销来源',
    cleared_ref_id      BIGINT       DEFAULT NULL COMMENT '核销业务 ID',
    cleared_time        DATETIME     DEFAULT NULL COMMENT '核销时间',
    remark              VARCHAR(512) DEFAULT NULL COMMENT '备注',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_shortage_order (order_id),
    KEY idx_ec_sales_order_shortage_line (order_line_id),
    KEY idx_ec_sales_order_shortage_sku (sku_code),
    KEY idx_ec_sales_order_shortage_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单发货欠货';

-- ========== 4. 发货扣库存流水 ==========
CREATE TABLE IF NOT EXISTS ec_sales_order_inventory_deduct (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id            BIGINT       NOT NULL COMMENT '销售订单 ID',
    order_line_id       BIGINT       NOT NULL COMMENT '订单明细 ID',
    shortage_id         BIGINT       DEFAULT NULL COMMENT '关联欠货记录 ID',
    sku_code            VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    inventory_id        BIGINT       DEFAULT NULL COMMENT 'ec_inventory.id',
    inventory_log_id    BIGINT       DEFAULT NULL COMMENT 'ec_inventory_log.id',
    deduct_qty          INT          NOT NULL COMMENT '本次扣除数量',
    before_qty          INT          DEFAULT NULL COMMENT '扣前库存',
    after_qty           INT          DEFAULT NULL COMMENT '扣后库存',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_deduct_order (order_id),
    KEY idx_ec_sales_order_deduct_line (order_line_id),
    KEY idx_ec_sales_order_deduct_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单库存扣减记录';

-- ========== 5. 订单导入配置与批次 ==========
-- 通用表见 admin-backend/sql/sys_import.sql（sys_import_profile / sys_import_batch）
-- 请先执行 sys_import.sql；若已有 ec_order_import_* 请再执行 sys_import_migrate_from_ec_order.sql

-- ========== 6. 导入原始行（Excel 链接名/规格名只存这里，用于预览与排错） ==========
CREATE TABLE IF NOT EXISTS ec_order_import_row (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_id            BIGINT       NOT NULL COMMENT '批次 ID',
    row_no              INT          NOT NULL COMMENT 'Excel 行号',
    parse_status        VARCHAR(16)  NOT NULL DEFAULT 'OK' COMMENT 'OK/ERROR/SKIP',
    platform_order_no   VARCHAR(64)  DEFAULT NULL COMMENT '平台订单号',
    link_name           VARCHAR(256) DEFAULT NULL COMMENT 'Excel 链接名称',
    sku_spec_name       VARCHAR(256) DEFAULT NULL COMMENT 'Excel SKU规格/展示名称',
    match_status        VARCHAR(16)  DEFAULT NULL COMMENT 'MATCHED/UNMATCHED',
    listing_link_sku_id BIGINT       DEFAULT NULL COMMENT '匹配到的 ec_listing_link_sku.id',
    manual_cost_price   DECIMAL(12, 2) DEFAULT NULL COMMENT '手动成本(元/套，未匹配链接时使用)',
    platform_line_status VARCHAR(64)  DEFAULT NULL COMMENT 'Excel 平台子订单/退款状态原文',
    line_status         VARCHAR(16)  DEFAULT NULL COMMENT '解析或人工指定的系统行状态',
    status_match_status VARCHAR(16)  DEFAULT NULL COMMENT '状态映射 MATCHED/UNMATCHED',
    sales_order_id      BIGINT       DEFAULT NULL COMMENT '入库订单 ID',
    sales_order_line_id BIGINT       DEFAULT NULL COMMENT '入库明细 ID',
    error_message       VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    raw_json            JSON         DEFAULT NULL COMMENT '原始行 JSON',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_order_import_row_batch (batch_id),
    KEY idx_ec_order_import_row_platform_no (platform_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单导入原始行';

-- ========== 8. 状态与退款规则（实现于 Service） ==========
-- 明细 status: PAID -> SHIPPED -> COMPLETED | CANCELLED | PARTIAL_REFUND | REFUNDED | RETURNED
--
-- 退款规则（不退库存，直接记亏）：
--   REFUND_ONLY  已发货仅退款：status=REFUNDED, refund_type=REFUND_ONLY
--                不 RECLAIM 库存；loss_amount = 行成本(cost_price*sku_quantity) + 分摊真实运费 - 退款后留存收入(0或部分)
--   RETURN_REFUND 退货退款：status=RETURNED, refund_type=RETURN_REFUND
--                同样不 RECLAIM 库存；货品视为损耗，loss_amount 同上口径
--   未发货取消：status=CANCELLED，无 loss_amount，未扣库存则无需处理
--
-- 行 profit 退款后重算：profit = line_received_amount - cost_price*sku_quantity - 分摊费用 - loss_adjustment
-- 或退款完成后 profit 置 0，loss_amount 记录净亏损
--
-- 头 status 聚合：
--   全部 PAID                     -> PAID
--   存在 SHIPPED 且非全部完成     -> PARTIAL_SHIPPED
--   全部 SHIPPED/COMPLETED        -> SHIPPED/COMPLETED
--   存在 REFUNDED/RETURNED 且混合 -> PARTIAL_REFUND
--   全部 REFUNDED/RETURNED/CANCELLED -> REFUNDED/CANCELLED
--
-- 4. express_station_id + 收货省(由地址解析，空则用店铺默认省) + 明细重量 → estimated_freight_amount
--    actual_freight_amount 由月结快递账单上传后按 tracking_number 回填，订单 Excel 不导入

-- ========== 9. 内置导入模板 ==========
-- 见 sys_import.sql 中 SALES_ORDER 默认 profile

-- ========== 10. 链接匹配辅助索引 ==========
SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND INDEX_NAME = 'idx_ec_listing_link_shop_name'
);
SET @sql := IF(
    @idx = 0,
    'ALTER TABLE ec_listing_link ADD KEY idx_ec_listing_link_shop_name (shop_id, name(128))',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx2 := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND INDEX_NAME = 'idx_ec_listing_link_sku_link_name'
);
SET @sql2 := IF(
    @idx2 = 0,
    'ALTER TABLE ec_listing_link_sku ADD KEY idx_ec_listing_link_sku_link_name (link_id, sku_name(128))',
    'SELECT 1'
);
PREPARE stmt FROM @sql2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
