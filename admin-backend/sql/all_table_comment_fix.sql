-- 全库表/字段 COMMENT 修复（由 deploy-all.sql 自动生成）
-- 生成：node admin-backend/sql/tools/generate_comment_fix.mjs
-- 执行：mysql -h 192.168.0.118 -u ai_manager -p123456 --default-character-set=utf8mb4 ai_manager_admin < all_table_comment_fix.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_manager_admin;

-- ========== ec_carton ==========
ALTER TABLE `ec_carton` COMMENT = '电商纸箱';

ALTER TABLE `ec_carton`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '纸箱主键',
    MODIFY COLUMN `factory_id` BIGINT        DEFAULT NULL COMMENT '所属工厂',
    MODIFY COLUMN `name` VARCHAR(128)  NOT NULL COMMENT '纸箱名称',
    MODIFY COLUMN `length_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '长(cm)',
    MODIFY COLUMN `width_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '宽(cm)',
    MODIFY COLUMN `height_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '高(cm)',
    MODIFY COLUMN `unit_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '单价',
    MODIFY COLUMN `remark` VARCHAR(512)  DEFAULT NULL COMMENT '备注';

-- ========== ec_express_notice ==========
ALTER TABLE `ec_express_notice` COMMENT = '电商快递须知';

ALTER TABLE `ec_express_notice`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '须知主键',
    MODIFY COLUMN `station_id` BIGINT       NOT NULL COMMENT '站点ID',
    MODIFY COLUMN `content` TEXT         NOT NULL COMMENT '须知内容',
    MODIFY COLUMN `highlight_red` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否标红 1是 0否',
    MODIFY COLUMN `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前';

-- ========== ec_express_price ==========
ALTER TABLE `ec_express_price` COMMENT = '电商快递价格';

ALTER TABLE `ec_express_price`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '价格主键',
    MODIFY COLUMN `station_id` BIGINT        NOT NULL COMMENT '站点ID',
    MODIFY COLUMN `province_name` VARCHAR(64)   NOT NULL COMMENT '省份名称',
    MODIFY COLUMN `price_w03_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '0.3kg价格',
    MODIFY COLUMN `price_w05_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '0.5kg价格',
    MODIFY COLUMN `price_w1_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '1kg价格',
    MODIFY COLUMN `price_w15_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '1.5kg价格',
    MODIFY COLUMN `price_w2_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '2kg价格',
    MODIFY COLUMN `price_w25_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '2.5kg价格',
    MODIFY COLUMN `price_w3_kg` DECIMAL(12, 2) DEFAULT NULL COMMENT '3kg价格',
    MODIFY COLUMN `over3_first_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg首重价格',
    MODIFY COLUMN `over3_additional_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg续重价格';

-- ========== ec_express_station ==========
ALTER TABLE `ec_express_station` COMMENT = '电商快递站点';

ALTER TABLE `ec_express_station`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '站点主键',
    MODIFY COLUMN `name` VARCHAR(128) NOT NULL COMMENT '快递名称',
    MODIFY COLUMN `contact` VARCHAR(256) DEFAULT NULL COMMENT '联系方式',
    MODIFY COLUMN `address` VARCHAR(512) DEFAULT NULL COMMENT '地址',
    MODIFY COLUMN `is_default` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
    MODIFY COLUMN `label_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '面单价格(元/单)';

-- ========== ec_factory ==========
ALTER TABLE `ec_factory` COMMENT = '电商工厂';

ALTER TABLE `ec_factory`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工厂主键',
    MODIFY COLUMN `name` VARCHAR(128) NOT NULL COMMENT '工厂名称',
    MODIFY COLUMN `contact_name` VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    MODIFY COLUMN `contact_phone` VARCHAR(64)  DEFAULT NULL COMMENT '联系方式',
    MODIFY COLUMN `address` VARCHAR(512) DEFAULT NULL COMMENT '地址',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== ec_inbound_order ==========
ALTER TABLE `ec_inbound_order` COMMENT = '电商进货单';

ALTER TABLE `ec_inbound_order`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '进货单主键',
    MODIFY COLUMN `order_no` VARCHAR(32)  NOT NULL COMMENT '进货单号',
    MODIFY COLUMN `factory_id` BIGINT       DEFAULT NULL COMMENT '所属工厂',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `order_time` DATETIME DEFAULT NULL COMMENT '下单时间',
    MODIFY COLUMN `expected_delivery_time` DATETIME DEFAULT NULL COMMENT '预收货时间',
    MODIFY COLUMN `actual_receipt_time` DATETIME DEFAULT NULL COMMENT '实际收货时间';

-- ========== ec_inbound_order_line ==========
ALTER TABLE `ec_inbound_order_line` COMMENT = '电商进货单明细';

ALTER TABLE `ec_inbound_order_line`
    MODIFY COLUMN `id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    MODIFY COLUMN `order_id` BIGINT      NOT NULL COMMENT '进货单 ID',
    MODIFY COLUMN `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    MODIFY COLUMN `quantity` INT         NOT NULL COMMENT '下单数量',
    MODIFY COLUMN `received_quantity` INT DEFAULT NULL COMMENT '实际收货数量';

-- ========== ec_inventory ==========
ALTER TABLE `ec_inventory` COMMENT = '电商库存';

ALTER TABLE `ec_inventory`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存主键',
    MODIFY COLUMN `sku_code` VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    MODIFY COLUMN `quantity` INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
    MODIFY COLUMN `ignore_alert` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否忽略预警 1是 0否',
    MODIFY COLUMN `alert_threshold` INT          NOT NULL DEFAULT 0 COMMENT '预警数量(库存<=该值且未忽略预警时报警)';

-- ========== ec_inventory_log ==========
ALTER TABLE `ec_inventory_log` COMMENT = '电商库存操作记录';

ALTER TABLE `ec_inventory_log`
    MODIFY COLUMN `id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录主键',
    MODIFY COLUMN `inventory_id` BIGINT      NOT NULL COMMENT '库存表 ID',
    MODIFY COLUMN `change_type` VARCHAR(16) NOT NULL COMMENT '改动方式 DEDUCT扣除 RECLAIM回收',
    MODIFY COLUMN `change_qty` INT         NOT NULL COMMENT '改动数量(正数)',
    MODIFY COLUMN `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型 INBOUND_ORDER 等',
    MODIFY COLUMN `ref_id` BIGINT DEFAULT NULL COMMENT '关联业务 ID',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注';

-- ========== ec_listing_link ==========
ALTER TABLE `ec_listing_link` COMMENT = '电商上架链接';

ALTER TABLE `ec_listing_link`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '链接主键',
    MODIFY COLUMN `name` VARCHAR(256) NOT NULL COMMENT '链接名称',
    MODIFY COLUMN `shop_id` BIGINT       NOT NULL COMMENT '所属店铺 ID',
    MODIFY COLUMN `platform_url` VARCHAR(1024) DEFAULT NULL COMMENT '平台商品链接URL',
    MODIFY COLUMN `listing_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== ec_listing_link_product ==========
ALTER TABLE `ec_listing_link_product` COMMENT = '上架链接关联商品';

ALTER TABLE `ec_listing_link_product`
    MODIFY COLUMN `id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `link_id` BIGINT   NOT NULL COMMENT '上架链接 ID',
    MODIFY COLUMN `product_id` BIGINT   NOT NULL COMMENT '商品 SPU ID',
    MODIFY COLUMN `sort_order` INT      NOT NULL DEFAULT 0 COMMENT '排序';

-- ========== ec_listing_link_sku ==========
ALTER TABLE `ec_listing_link_sku` COMMENT = '上架链接 SKU 信息';

ALTER TABLE `ec_listing_link_sku`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `link_id` BIGINT        NOT NULL COMMENT '上架链接 ID',
    MODIFY COLUMN `sku_name` VARCHAR(256)  NOT NULL COMMENT '链接 SKU 展示名称',
    MODIFY COLUMN `sku_codes` VARCHAR(1024) NOT NULL COMMENT '对应 SKU 货号，多个英文逗号分隔',
    MODIFY COLUMN `discount_pct` DECIMAL(5, 2) NOT NULL DEFAULT 100.00 COMMENT '折扣折数(90=9折)',
    MODIFY COLUMN `coupon_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券金额(元)',
    MODIFY COLUMN `min_set_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '最低设置金额(元)',
    MODIFY COLUMN `cost_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '成本价格(元，含平台费)',
    MODIFY COLUMN `base_cost_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '基础成本=SKU+纸箱+快递',
    MODIFY COLUMN `platform_fee_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费(盈亏平衡口径)',
    MODIFY COLUMN `actual_set_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '真实设置金额(元，可手动填写)',
    MODIFY COLUMN `profit` DECIMAL(12, 2) DEFAULT NULL COMMENT '利润(元)',
    MODIFY COLUMN `sort_order` INT           NOT NULL DEFAULT 0 COMMENT '排序';

-- ========== ec_order_import_row ==========
ALTER TABLE `ec_order_import_row` COMMENT = '销售订单导入原始行';

ALTER TABLE `ec_order_import_row`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `batch_id` BIGINT       NOT NULL COMMENT '批次 ID',
    MODIFY COLUMN `row_no` INT          NOT NULL COMMENT 'Excel 行号',
    MODIFY COLUMN `parse_status` VARCHAR(16)  NOT NULL DEFAULT 'OK' COMMENT 'OK/ERROR/SKIP',
    MODIFY COLUMN `platform_order_no` VARCHAR(64)  DEFAULT NULL COMMENT '平台订单号',
    MODIFY COLUMN `link_name` VARCHAR(256) DEFAULT NULL COMMENT 'Excel 链接名称',
    MODIFY COLUMN `sku_spec_name` VARCHAR(256) DEFAULT NULL COMMENT 'Excel SKU规格/展示名称',
    MODIFY COLUMN `match_status` VARCHAR(16)  DEFAULT NULL COMMENT 'MATCHED/UNMATCHED',
    MODIFY COLUMN `listing_link_sku_id` BIGINT       DEFAULT NULL COMMENT '匹配到的 ec_listing_link_sku.id',
    MODIFY COLUMN `manual_cost_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '手动成本(元/套，未匹配链接时使用)',
    MODIFY COLUMN `platform_line_status` VARCHAR(64) DEFAULT NULL COMMENT 'Excel 平台子订单/退款状态原文',
    MODIFY COLUMN `line_status` VARCHAR(16) DEFAULT NULL COMMENT '解析或人工指定的系统行状态',
    MODIFY COLUMN `status_match_status` VARCHAR(16) DEFAULT NULL COMMENT '状态映射 MATCHED/UNMATCHED',
    MODIFY COLUMN `sales_order_id` BIGINT       DEFAULT NULL COMMENT '入库订单 ID',
    MODIFY COLUMN `sales_order_line_id` BIGINT       DEFAULT NULL COMMENT '入库明细 ID',
    MODIFY COLUMN `error_message` VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    MODIFY COLUMN `raw_json` JSON         DEFAULT NULL COMMENT '原始行 JSON';

-- ========== ec_platform ==========
ALTER TABLE `ec_platform` COMMENT = '电商平台';

ALTER TABLE `ec_platform`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '平台主键',
    MODIFY COLUMN `name` VARCHAR(128) NOT NULL COMMENT '平台名称',
    MODIFY COLUMN `name_en` VARCHAR(128) DEFAULT NULL COMMENT '平台英文名称',
    MODIFY COLUMN `platform_code` INT          NOT NULL COMMENT '平台标识(枚举 int)',
    MODIFY COLUMN `channel_type` VARCHAR(16)  NOT NULL DEFAULT 'ONLINE' COMMENT '渠道模式 ONLINE/OFFLINE',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== ec_product ==========
ALTER TABLE `ec_product` COMMENT = '电商商品 SPU';

ALTER TABLE `ec_product`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SPU 主键',
    MODIFY COLUMN `factory_id` BIGINT        DEFAULT NULL COMMENT '所属工厂',
    MODIFY COLUMN `name` VARCHAR(256)  NOT NULL COMMENT '商品名称(SPU)',
    MODIFY COLUMN `description` TEXT          DEFAULT NULL COMMENT '商品描述',
    MODIFY COLUMN `rebate_pct` DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，如 5.50 表示 5.5%)',
    MODIFY COLUMN `image_name` VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== ec_sales_order ==========
ALTER TABLE `ec_sales_order` COMMENT = '电商销售订单';

ALTER TABLE `ec_sales_order`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '销售订单主键',
    MODIFY COLUMN `order_no` VARCHAR(32)   NOT NULL COMMENT '系统订单号 SOyyyyMMddxxxx',
    MODIFY COLUMN `shop_id` BIGINT        NOT NULL COMMENT '所属店铺 ID',
    MODIFY COLUMN `platform_order_no` VARCHAR(64)   DEFAULT NULL COMMENT '平台订单号（导入去重键）',
    MODIFY COLUMN `source` VARCHAR(16)   NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL/IMPORT/API',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PAID/PARTIAL_SHIPPED/SHIPPED/PARTIAL_REFUND/COMPLETED/CANCELLED/REFUNDED',
    MODIFY COLUMN `platform_status` VARCHAR(64)   DEFAULT NULL COMMENT '平台原始状态文案',
    MODIFY COLUMN `express_station_id` BIGINT        DEFAULT NULL COMMENT '快递站点 ID，关联 ec_express_station.id，用于计算真实运费',
    MODIFY COLUMN `order_time` DATETIME      NOT NULL COMMENT '下单时间',
    MODIFY COLUMN `pay_time` DATETIME      DEFAULT NULL COMMENT '支付时间',
    MODIFY COLUMN `ship_time` DATETIME      DEFAULT NULL COMMENT '首条明细发货时间',
    MODIFY COLUMN `complete_time` DATETIME      DEFAULT NULL COMMENT '订单完成时间',
    MODIFY COLUMN `buyer_name` VARCHAR(128)  DEFAULT NULL COMMENT '买家昵称/姓名',
    MODIFY COLUMN `buyer_phone` VARCHAR(32)   DEFAULT NULL COMMENT '买家电话',
    MODIFY COLUMN `receive_province` VARCHAR(64)   DEFAULT NULL COMMENT '收货省(由 receive_address 自动解析)',
    MODIFY COLUMN `receive_city` VARCHAR(64)   DEFAULT NULL COMMENT '收货市',
    MODIFY COLUMN `receive_district` VARCHAR(64)   DEFAULT NULL COMMENT '收货区',
    MODIFY COLUMN `receive_address` VARCHAR(512)  DEFAULT NULL COMMENT '详细地址',
    MODIFY COLUMN `tracking_number` VARCHAR(64)   DEFAULT NULL COMMENT '快递单号',
    MODIFY COLUMN `buyer_remark` VARCHAR(512) DEFAULT NULL COMMENT '买家留言',
    MODIFY COLUMN `seller_remark` VARCHAR(512) DEFAULT NULL COMMENT '卖家备注',
    MODIFY COLUMN `received_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '订单实收金额(元)',
    MODIFY COLUMN `total_cost_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '订单总成本(元，明细汇总快照)',
    MODIFY COLUMN `freight_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '买家付运费(元)',
    MODIFY COLUMN `estimated_freight_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '试算运费(元，按站点+省+重量)',
    MODIFY COLUMN `actual_freight_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '真实运费(元，月结快递账单按运单号回填)',
    MODIFY COLUMN `order_coupon_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '订单级优惠券(元)',
    MODIFY COLUMN `platform_fee_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费合计快照(元)',
    MODIFY COLUMN `profit_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '利润合计快照(元，含退款亏损后)',
    MODIFY COLUMN `total_loss_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '退款亏损合计(元)',
    MODIFY COLUMN `has_shortage` TINYINT       NOT NULL DEFAULT 0 COMMENT '是否存在欠货 1是 0否',
    MODIFY COLUMN `import_batch_id` BIGINT        DEFAULT NULL COMMENT '关联导入批次 ID',
    MODIFY COLUMN `platform_raw_json` JSON          DEFAULT NULL COMMENT '平台/导入原始 JSON 快照';

-- ========== ec_sales_order_inventory_deduct ==========
ALTER TABLE `ec_sales_order_inventory_deduct` COMMENT = '销售订单库存扣减记录';

ALTER TABLE `ec_sales_order_inventory_deduct`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `order_id` BIGINT       NOT NULL COMMENT '销售订单 ID',
    MODIFY COLUMN `order_line_id` BIGINT       NOT NULL COMMENT '订单明细 ID',
    MODIFY COLUMN `shortage_id` BIGINT       DEFAULT NULL COMMENT '关联欠货记录 ID',
    MODIFY COLUMN `sku_code` VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    MODIFY COLUMN `inventory_id` BIGINT       DEFAULT NULL COMMENT 'ec_inventory.id',
    MODIFY COLUMN `inventory_log_id` BIGINT       DEFAULT NULL COMMENT 'ec_inventory_log.id',
    MODIFY COLUMN `deduct_qty` INT          NOT NULL COMMENT '本次扣除数量',
    MODIFY COLUMN `before_qty` INT          DEFAULT NULL COMMENT '扣前库存',
    MODIFY COLUMN `after_qty` INT          DEFAULT NULL COMMENT '扣后库存';

-- ========== ec_sales_order_line ==========
ALTER TABLE `ec_sales_order_line` COMMENT = '电商销售订单明细';

ALTER TABLE `ec_sales_order_line`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    MODIFY COLUMN `order_id` BIGINT        NOT NULL COMMENT '销售订单 ID',
    MODIFY COLUMN `sort_order` INT           NOT NULL DEFAULT 0 COMMENT '排序',
    MODIFY COLUMN `listing_link_sku_id` BIGINT        DEFAULT NULL COMMENT '上架链接 SKU ID（匹配 ec_listing_link_sku.id）',
    MODIFY COLUMN `link_name` VARCHAR(256)  DEFAULT NULL COMMENT '链接名称快照 ec_listing_link.name',
    MODIFY COLUMN `sku_spec_name` VARCHAR(256)  DEFAULT NULL COMMENT 'SKU展示名称快照 ec_listing_link_sku.sku_name',
    MODIFY COLUMN `sku_codes` VARCHAR(1024) DEFAULT NULL COMMENT '货号快照，逗号分隔',
    MODIFY COLUMN `sku_quantity` INT           NOT NULL DEFAULT 1 COMMENT 'SKU数量(链接SKU套数)',
    MODIFY COLUMN `shipped_quantity` INT           NOT NULL DEFAULT 0 COMMENT '已发货套数',
    MODIFY COLUMN `short_quantity` INT           NOT NULL DEFAULT 0 COMMENT '欠货套数',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'PAID' COMMENT 'PAID/SHIPPED/COMPLETED/CANCELLED/PARTIAL_REFUND/REFUNDED/RETURNED',
    MODIFY COLUMN `platform_line_status` VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单原始状态',
    MODIFY COLUMN `refund_type` VARCHAR(16)   DEFAULT NULL COMMENT '退款类型 NONE/REFUND_ONLY已发货退款/RETURN_REFUND退货退款',
    MODIFY COLUMN `refund_time` DATETIME      DEFAULT NULL COMMENT '退款/退货时间',
    MODIFY COLUMN `refund_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '退款金额(元)',
    MODIFY COLUMN `loss_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '亏损金额(元)，已发货退款/退货不退库存，按成本记亏',
    MODIFY COLUMN `unit_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '成交单价(元/套)',
    MODIFY COLUMN `discount_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣折数快照(90=9折)',
    MODIFY COLUMN `line_coupon_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '行级优惠券(元)',
    MODIFY COLUMN `line_received_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '行实收金额(元)',
    MODIFY COLUMN `sku_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT 'SKU售价合计快照',
    MODIFY COLUMN `carton_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '纸箱成本快照',
    MODIFY COLUMN `express_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '试算快递成本快照',
    MODIFY COLUMN `base_cost_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '基础成本快照',
    MODIFY COLUMN `platform_fee_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费快照',
    MODIFY COLUMN `cost_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '行总成本快照(含平台费盈亏平衡口径)',
    MODIFY COLUMN `min_set_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '最低设置金额快照',
    MODIFY COLUMN `profit` DECIMAL(12, 2) DEFAULT NULL COMMENT '行利润快照',
    MODIFY COLUMN `pricing_risk` VARCHAR(16)   DEFAULT NULL COMMENT 'OK/BELOW_MIN/NEGATIVE_PROFIT',
    MODIFY COLUMN `platform_line_no` VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单号',
    MODIFY COLUMN `platform_item_name` VARCHAR(512)  DEFAULT NULL COMMENT '平台商品标题快照';

-- ========== ec_sales_order_shortage ==========
ALTER TABLE `ec_sales_order_shortage` COMMENT = '销售订单发货欠货';

ALTER TABLE `ec_sales_order_shortage`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '欠货主键',
    MODIFY COLUMN `order_id` BIGINT       NOT NULL COMMENT '销售订单 ID',
    MODIFY COLUMN `order_line_id` BIGINT       NOT NULL COMMENT '订单明细 ID',
    MODIFY COLUMN `sku_code` VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    MODIFY COLUMN `need_qty` INT          NOT NULL COMMENT '应扣数量',
    MODIFY COLUMN `deducted_qty` INT          NOT NULL DEFAULT 0 COMMENT '实扣数量',
    MODIFY COLUMN `short_qty` INT          NOT NULL DEFAULT 0 COMMENT '欠货数量',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLEARED',
    MODIFY COLUMN `cleared_qty` INT          NOT NULL DEFAULT 0 COMMENT '已核销数量',
    MODIFY COLUMN `cleared_ref_type` VARCHAR(32)  DEFAULT NULL COMMENT '核销来源',
    MODIFY COLUMN `cleared_ref_id` BIGINT       DEFAULT NULL COMMENT '核销业务 ID',
    MODIFY COLUMN `cleared_time` DATETIME     DEFAULT NULL COMMENT '核销时间',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注';

-- ========== ec_settlement_buyer_exclude ==========
ALTER TABLE `ec_settlement_buyer_exclude` COMMENT = '月结统计-买家排除';

ALTER TABLE `ec_settlement_buyer_exclude`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `shop_id` BIGINT       DEFAULT NULL COMMENT '店铺 ID，空=全部店铺',
    MODIFY COLUMN `buyer_name` VARCHAR(128) NOT NULL COMMENT '买家昵称（精确匹配 trim）',
    MODIFY COLUMN `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `enabled` TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用';

-- ========== ec_settlement_express_bill ==========
ALTER TABLE `ec_settlement_express_bill` COMMENT = '月结快递账单导入批次';

ALTER TABLE `ec_settlement_express_bill`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `bill_month` CHAR(7)       NOT NULL COMMENT '账单月份 YYYY-MM',
    MODIFY COLUMN `file_name` VARCHAR(256)  DEFAULT NULL COMMENT '上传文件名',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED/FAILED',
    MODIFY COLUMN `express_station_id` BIGINT       DEFAULT NULL COMMENT '快递站点 ID',
    MODIFY COLUMN `column_mapping` TEXT           DEFAULT NULL COMMENT '列映射 JSON 快照',
    MODIFY COLUMN `header_row` INT            NOT NULL DEFAULT 1 COMMENT '表头行号(1-based)',
    MODIFY COLUMN `data_start_row` INT            NOT NULL DEFAULT 2 COMMENT '数据起始行(1-based)',
    MODIFY COLUMN `import_mode` VARCHAR(16)    NOT NULL DEFAULT 'FILE' COMMENT 'FILE/MANUAL/MIXED',
    MODIFY COLUMN `gap_order_rows` INT            NOT NULL DEFAULT 0 COMMENT '未匹配发货/完成订单数',
    MODIFY COLUMN `other_express` TINYINT NOT NULL DEFAULT 0 COMMENT '1=其他快递公司（未匹配系统站点）',
    MODIFY COLUMN `include_label_price` TINYINT NOT NULL DEFAULT 0 COMMENT '1=运费叠加面单价格';

-- ========== ec_settlement_express_bill_line ==========
ALTER TABLE `ec_settlement_express_bill_line` COMMENT = '月结快递账单明细';

ALTER TABLE `ec_settlement_express_bill_line`
    MODIFY COLUMN `id` BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `bill_id` BIGINT         NOT NULL COMMENT '批次 ID',
    MODIFY COLUMN `source` VARCHAR(16)    NOT NULL COMMENT 'FILE/GAP_ORDER/MANUAL',
    MODIFY COLUMN `order_id` BIGINT         DEFAULT NULL COMMENT '匹配订单 ID',
    MODIFY COLUMN `platform_order_no` VARCHAR(64)    DEFAULT NULL COMMENT '平台订单号',
    MODIFY COLUMN `order_no` VARCHAR(64)    DEFAULT NULL COMMENT '系统订单号',
    MODIFY COLUMN `tracking_number` VARCHAR(128)   DEFAULT NULL COMMENT '运单号',
    MODIFY COLUMN `freight_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '运费',
    MODIFY COLUMN `match_status` VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT 'MATCHED/UNMATCHED/PENDING/APPLIED',
    MODIFY COLUMN `remark` VARCHAR(256)   DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `settlement_destination` VARCHAR(128) DEFAULT NULL COMMENT '结算目的地',
    MODIFY COLUMN `weight` DECIMAL(10, 3) DEFAULT NULL COMMENT '重量(kg)',
    MODIFY COLUMN `ship_time` DATETIME       DEFAULT NULL COMMENT '发货时间';

-- ========== ec_settlement_order_decision ==========
ALTER TABLE `ec_settlement_order_decision` COMMENT = '月结统计-订单纳入决策';

ALTER TABLE `ec_settlement_order_decision`
    MODIFY COLUMN `id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `shop_id` BIGINT      NOT NULL COMMENT '店铺 ID',
    MODIFY COLUMN `order_id` BIGINT      NOT NULL COMMENT '销售订单 ID',
    MODIFY COLUMN `settlement_month` CHAR(7)     NOT NULL COMMENT '统计月份 YYYY-MM',
    MODIFY COLUMN `included` TINYINT     NOT NULL DEFAULT 0 COMMENT '1纳入 0不纳入';

-- ========== ec_shop ==========
ALTER TABLE `ec_shop` COMMENT = '电商店铺';

ALTER TABLE `ec_shop`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '店铺主键',
    MODIFY COLUMN `name` VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    MODIFY COLUMN `name_en` VARCHAR(128)  DEFAULT NULL COMMENT '店铺英文名称',
    MODIFY COLUMN `platform_id` BIGINT        NOT NULL COMMENT '所属平台 ID',
    MODIFY COLUMN `remark` VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `category_commission_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '类目/交易佣金%',
    MODIFY COLUMN `tech_service_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '基础技术服务费%',
    MODIFY COLUMN `payment_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '支付手续费%',
    MODIFY COLUMN `promotion_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '推广/广告默认扣点%',
    MODIFY COLUMN `fulfillment_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '履约/代发服务费%',
    MODIFY COLUMN `return_service_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '退货/逆向物流服务费率%',
    MODIFY COLUMN `installment_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '分期/花呗手续费%',
    MODIFY COLUMN `activity_service_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '活动/大促技术服务费%',
    MODIFY COLUMN `annual_platform_fee` DECIMAL(12, 2) DEFAULT NULL COMMENT '平台年费/软件服务费(元/年)',
    MODIFY COLUMN `deposit_amount` DECIMAL(12, 2) DEFAULT NULL COMMENT '保证金(元)',
    MODIFY COLUMN `shipping_insurance_fee` DECIMAL(10, 2) DEFAULT NULL COMMENT '默认单笔运费险(元)',
    MODIFY COLUMN `other_fee_pct` DECIMAL(5, 2) DEFAULT NULL COMMENT '其他综合扣点%',
    MODIFY COLUMN `other_fee_remark` VARCHAR(256)  DEFAULT NULL COMMENT '其他费用说明',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== ec_sku ==========
ALTER TABLE `ec_sku` COMMENT = '电商商品 SKU';

ALTER TABLE `ec_sku`
    MODIFY COLUMN `id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU 主键',
    MODIFY COLUMN `product_id` BIGINT        NOT NULL COMMENT '所属 SPU',
    MODIFY COLUMN `sku_code` VARCHAR(64)   NOT NULL COMMENT '货号',
    MODIFY COLUMN `spec_name` VARCHAR(128)  DEFAULT NULL COMMENT '规格名称',
    MODIFY COLUMN `rebate_pct` DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，计算以 SKU 为准)',
    MODIFY COLUMN `image_name` VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    MODIFY COLUMN `carton_id` BIGINT        DEFAULT NULL COMMENT '匹配纸箱',
    MODIFY COLUMN `sale_price` DECIMAL(12, 2) DEFAULT NULL COMMENT '销售价',
    MODIFY COLUMN `product_length_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '单品长(cm)',
    MODIFY COLUMN `product_width_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '单品宽(cm)',
    MODIFY COLUMN `product_height_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '单品高(cm)',
    MODIFY COLUMN `carton_length_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱长(cm)',
    MODIFY COLUMN `carton_width_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱宽(cm)',
    MODIFY COLUMN `carton_height_cm` DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱高(cm)',
    MODIFY COLUMN `carton_gross_weight_kg` DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱毛重(kg)',
    MODIFY COLUMN `carton_net_weight_kg` DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱净重(kg)',
    MODIFY COLUMN `units_per_carton` INT           NOT NULL DEFAULT 1 COMMENT '外箱装产品数量',
    MODIFY COLUMN `status` VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/OFF_SALE/DRAFT';

-- ========== ec_stocktake_order ==========
ALTER TABLE `ec_stocktake_order` COMMENT = '电商盘点单';

ALTER TABLE `ec_stocktake_order`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '盘点单主键',
    MODIFY COLUMN `order_no` VARCHAR(32)  NOT NULL COMMENT '盘点单号',
    MODIFY COLUMN `factory_id` BIGINT       DEFAULT NULL COMMENT '所属工厂',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `stocktake_time` DATETIME     DEFAULT NULL COMMENT '盘点时间';

-- ========== ec_stocktake_order_line ==========
ALTER TABLE `ec_stocktake_order_line` COMMENT = '电商盘点单明细';

ALTER TABLE `ec_stocktake_order_line`
    MODIFY COLUMN `id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    MODIFY COLUMN `order_id` BIGINT      NOT NULL COMMENT '盘点单 ID',
    MODIFY COLUMN `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    MODIFY COLUMN `book_quantity` INT         NOT NULL COMMENT '账面数量(保存时快照)',
    MODIFY COLUMN `actual_quantity` INT         DEFAULT NULL COMMENT '实盘数量';

-- ========== nb_baidu_pan_auth ==========
ALTER TABLE `nb_baidu_pan_auth` COMMENT = '百度网盘 OAuth 授权';

ALTER TABLE `nb_baidu_pan_auth`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `user_id` BIGINT       NOT NULL DEFAULT 1 COMMENT '用户 ID，单用户默认 1',
    MODIFY COLUMN `access_token` VARCHAR(512) NOT NULL COMMENT '访问令牌',
    MODIFY COLUMN `refresh_token` VARCHAR(512) NOT NULL COMMENT '刷新令牌',
    MODIFY COLUMN `expires_at` DATETIME     NOT NULL COMMENT 'access_token 过期时间',
    MODIFY COLUMN `baidu_uid` BIGINT       DEFAULT NULL COMMENT '百度用户 ID';

-- ========== nb_note ==========
ALTER TABLE `nb_note` COMMENT = '笔记';

ALTER TABLE `nb_note`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `notebook_id` BIGINT       DEFAULT NULL COMMENT '所属文件夹',
    MODIFY COLUMN `title` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '标题',
    MODIFY COLUMN `storage_type` VARCHAR(16) NOT NULL DEFAULT 'BAIDU_PAN' COMMENT 'BAIDU_PAN/LOCAL',
    MODIFY COLUMN `storage_path` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '网盘或本地相对路径',
    MODIFY COLUMN `storage_fs_id` BIGINT DEFAULT NULL COMMENT '百度 fs_id',
    MODIFY COLUMN `content_hash` CHAR(64) DEFAULT NULL COMMENT 'SHA-256',
    MODIFY COLUMN `content_size` BIGINT NOT NULL DEFAULT 0 COMMENT '正文字节数',
    MODIFY COLUMN `content_version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    MODIFY COLUMN `content_excerpt` VARCHAR(512) DEFAULT NULL COMMENT '纯文本摘要',
    MODIFY COLUMN `sync_status` VARCHAR(16) NOT NULL DEFAULT 'SYNCED' COMMENT 'SYNCING/SYNCED/FAILED',
    MODIFY COLUMN `sync_error` VARCHAR(512) DEFAULT NULL COMMENT '同步失败原因',
    MODIFY COLUMN `note_type` VARCHAR(16)  NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/TODO/MEMO',
    MODIFY COLUMN `is_pinned` TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶',
    MODIFY COLUMN `is_favorite` TINYINT      NOT NULL DEFAULT 0 COMMENT '收藏',
    MODIFY COLUMN `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED';

-- ========== nb_note_tag ==========
ALTER TABLE `nb_note_tag` COMMENT = '笔记标签';

ALTER TABLE `nb_note_tag`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `name` VARCHAR(64)  NOT NULL COMMENT '标签名',
    MODIFY COLUMN `color` VARCHAR(16)  DEFAULT NULL COMMENT '颜色';

-- ========== nb_note_tag_rel ==========
ALTER TABLE `nb_note_tag_rel` COMMENT = '笔记-标签关联';

ALTER TABLE `nb_note_tag_rel`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `note_id` BIGINT NOT NULL COMMENT '笔记 ID',
    MODIFY COLUMN `tag_id` BIGINT NOT NULL COMMENT '标签 ID';

-- ========== nb_notebook ==========
ALTER TABLE `nb_notebook` COMMENT = '笔记本文件夹';

ALTER TABLE `nb_notebook`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `parent_id` BIGINT       DEFAULT NULL COMMENT '父文件夹 ID，NULL 为根级',
    MODIFY COLUMN `name` VARCHAR(128) NOT NULL COMMENT '文件夹名称',
    MODIFY COLUMN `icon` VARCHAR(32)  DEFAULT NULL COMMENT '图标标识',
    MODIFY COLUMN `color` VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    MODIFY COLUMN `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序';

-- ========== nb_todo_item ==========
ALTER TABLE `nb_todo_item` COMMENT = '笔记本待办';

ALTER TABLE `nb_todo_item`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `content` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '待办内容',
    MODIFY COLUMN `completed` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成 0/1',
    MODIFY COLUMN `due_time` DATETIME     DEFAULT NULL COMMENT '截止时间',
    MODIFY COLUMN `remind_time` DATETIME DEFAULT NULL COMMENT '提醒时间',
    MODIFY COLUMN `repeat_type` VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
    MODIFY COLUMN `repeat_interval` INT NOT NULL DEFAULT 1 COMMENT '重复间隔',
    MODIFY COLUMN `repeat_until` DATETIME DEFAULT NULL COMMENT '重复截止日期',
    MODIFY COLUMN `remind_notified` TINYINT NOT NULL DEFAULT 0 COMMENT '提醒是否已推送 0/1',
    MODIFY COLUMN `series_id` BIGINT       DEFAULT NULL COMMENT '重复系列 ID',
    MODIFY COLUMN `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
    MODIFY COLUMN `pinned` TINYINT      NOT NULL DEFAULT 0 COMMENT '特别提醒 0/1',
    MODIFY COLUMN `repeat_days` VARCHAR(255) DEFAULT NULL COMMENT 'WEEKLY:1,3,5 MONTHLY:1,15 YEARLY:01-15,06-01';

-- ========== pomodoro_plan ==========
ALTER TABLE `pomodoro_plan` COMMENT = '番茄钟计划';

ALTER TABLE `pomodoro_plan`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `title` VARCHAR(128) NOT NULL COMMENT '计划名称',
    MODIFY COLUMN `work_duration_min` INT          NOT NULL DEFAULT 25 COMMENT '专注时长(分钟)',
    MODIFY COLUMN `short_break_min` INT          NOT NULL DEFAULT 5 COMMENT '短休息(分钟)',
    MODIFY COLUMN `long_break_min` INT          NOT NULL DEFAULT 15 COMMENT '长休息(分钟)',
    MODIFY COLUMN `rounds_before_long_break` INT          NOT NULL DEFAULT 4 COMMENT '几轮后长休息',
    MODIFY COLUMN `daily_goal_rounds` INT          NOT NULL DEFAULT 8 COMMENT '每日目标轮次',
    MODIFY COLUMN `daily_goal_minutes` INT          NOT NULL DEFAULT 200 COMMENT '每日目标专注分钟',
    MODIFY COLUMN `is_default` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认计划',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED';

-- ========== pomodoro_record ==========
ALTER TABLE `pomodoro_record` COMMENT = '番茄钟完成记录';

ALTER TABLE `pomodoro_record`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `plan_id` BIGINT       DEFAULT NULL COMMENT '关联计划',
    MODIFY COLUMN `record_type` VARCHAR(16)  NOT NULL COMMENT 'WORK/SHORT_BREAK/LONG_BREAK',
    MODIFY COLUMN `duration_sec` INT          NOT NULL COMMENT '实际时长(秒)',
    MODIFY COLUMN `round_index` INT          NOT NULL DEFAULT 0 COMMENT '当日第几轮专注(仅WORK有效)',
    MODIFY COLUMN `stat_date` DATE         NOT NULL COMMENT '统计日期',
    MODIFY COLUMN `source` VARCHAR(16)  NOT NULL DEFAULT 'ADMIN' COMMENT '来源 ADMIN/DEVICE';

-- ========== sys_import_batch ==========
ALTER TABLE `sys_import_batch` COMMENT = '通用文档导入批次';

ALTER TABLE `sys_import_batch`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次主键',
    MODIFY COLUMN `batch_no` VARCHAR(32)  NOT NULL COMMENT '批次号',
    MODIFY COLUMN `profile_id` BIGINT       DEFAULT NULL COMMENT '使用的导入配置',
    MODIFY COLUMN `biz_type` VARCHAR(32)  NOT NULL COMMENT '业务类型',
    MODIFY COLUMN `biz_context` JSON         DEFAULT NULL COMMENT '业务上下文，如 {"shopId":1}',
    MODIFY COLUMN `file_name` VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    MODIFY COLUMN `file_path` VARCHAR(512) DEFAULT NULL COMMENT '存储路径',
    MODIFY COLUMN `detected_columns` JSON         DEFAULT NULL COMMENT '上传时检测到的列名',
    MODIFY COLUMN `source` VARCHAR(16)  NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/SCHEDULED',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREVIEWED/COMMITTED/FAILED',
    MODIFY COLUMN `unmatched_rows` INT          NOT NULL DEFAULT 0 COMMENT '业务层未匹配行数';

-- ========== sys_import_profile ==========
ALTER TABLE `sys_import_profile` COMMENT = '通用文档导入列映射配置';

ALTER TABLE `sys_import_profile`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键',
    MODIFY COLUMN `name` VARCHAR(128) NOT NULL COMMENT '配置名称',
    MODIFY COLUMN `biz_type` VARCHAR(32)  NOT NULL COMMENT '业务类型 SALES_ORDER/...',
    MODIFY COLUMN `platform_id` BIGINT       DEFAULT NULL COMMENT '绑定平台 ec_platform.id（订单导入按平台）',
    MODIFY COLUMN `scope_key` VARCHAR(64)  DEFAULT NULL COMMENT '作用域键，如 platform:2',
    MODIFY COLUMN `shop_id` BIGINT       DEFAULT NULL COMMENT '绑定店铺（可选，一般不用）',
    MODIFY COLUMN `file_type` VARCHAR(16)  NOT NULL DEFAULT 'XLSX' COMMENT 'XLSX/XLS/CSV',
    MODIFY COLUMN `header_row` INT          NOT NULL DEFAULT 1 COMMENT '表头行号（1-based）',
    MODIFY COLUMN `data_start_row` INT          NOT NULL DEFAULT 2 COMMENT '数据起始行号（1-based）',
    MODIFY COLUMN `sheet_name` VARCHAR(64)  DEFAULT NULL COMMENT '工作表名，空则首个 sheet',
    MODIFY COLUMN `column_mapping` JSON         NOT NULL COMMENT '后端字段 -> 文档列名',
    MODIFY COLUMN `value_mapping` JSON         DEFAULT NULL COMMENT '值映射，如平台状态 -> 系统状态',
    MODIFY COLUMN `extra_config` JSON         DEFAULT NULL COMMENT '扩展配置 JSON',
    MODIFY COLUMN `enabled` TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    MODIFY COLUMN `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注';

-- ========== sys_user ==========
ALTER TABLE `sys_user` COMMENT = '系统用户';

ALTER TABLE `sys_user`
    MODIFY COLUMN `id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN `username` VARCHAR(64)  NOT NULL COMMENT '登录名',
    MODIFY COLUMN `nickname` VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    MODIFY COLUMN `status` VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    MODIFY COLUMN `deleted` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除';
