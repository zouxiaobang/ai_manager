-- ============================================================
-- 通用文档导入：配置 + 批次（多业务复用，biz_type 区分）
-- 执行库：ai_manager_admin
-- 订单等业务原始行仍用业务表（如 ec_order_import_row）
-- ============================================================

USE ai_manager_admin;

-- ========== 1. 导入列映射配置 ==========
CREATE TABLE IF NOT EXISTS sys_import_profile (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键',
    name                 VARCHAR(128) NOT NULL COMMENT '配置名称',
    biz_type             VARCHAR(32)  NOT NULL COMMENT '业务类型 SALES_ORDER/...',
    platform_id          BIGINT       DEFAULT NULL COMMENT '绑定平台 ec_platform.id（订单导入按平台）',
    scope_key            VARCHAR(64)  DEFAULT NULL COMMENT '作用域键，如 platform:2',
    shop_id              BIGINT       DEFAULT NULL COMMENT '绑定店铺（可选，一般不用）',
    file_type            VARCHAR(16)  NOT NULL DEFAULT 'XLSX' COMMENT 'XLSX/XLS/CSV',
    header_row           INT          NOT NULL DEFAULT 1 COMMENT '表头行号（1-based）',
    data_start_row       INT          NOT NULL DEFAULT 2 COMMENT '数据起始行号（1-based）',
    sheet_name           VARCHAR(64)  DEFAULT NULL COMMENT '工作表名，空则首个 sheet',
    column_mapping       JSON         NOT NULL COMMENT '后端字段 -> 文档列名',
    value_mapping        JSON         DEFAULT NULL COMMENT '值映射，如平台状态 -> 系统状态',
    extra_config         JSON         DEFAULT NULL COMMENT '扩展配置 JSON',
    enabled              TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    remark               VARCHAR(512) DEFAULT NULL COMMENT '备注',
    deleted              TINYINT      NOT NULL DEFAULT 0,
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_import_profile_biz (biz_type),
    KEY idx_sys_import_profile_platform (platform_id),
    KEY idx_sys_import_profile_shop (shop_id),
    KEY idx_sys_import_profile_scope (biz_type, scope_key),
    UNIQUE KEY uk_sys_import_profile_biz_platform_name (biz_type, platform_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文档导入列映射配置';

-- column_mapping 示例（SALES_ORDER）：
--   link_name -> 链接名称
--   sku_spec_name -> SKU规格名称
--   sku_quantity -> SKU数量
--   express_station_name -> 快递站点
--   platform_order_no, order_time, received_amount, tracking_number ...

-- ========== 2. 导入批次 ==========
CREATE TABLE IF NOT EXISTS sys_import_batch (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次主键',
    batch_no            VARCHAR(32)  NOT NULL COMMENT '批次号',
    profile_id          BIGINT       DEFAULT NULL COMMENT '使用的导入配置',
    biz_type            VARCHAR(32)  NOT NULL COMMENT '业务类型',
    biz_context         JSON         DEFAULT NULL COMMENT '业务上下文，如 {"shopId":1}',
    file_name           VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    file_path           VARCHAR(512) DEFAULT NULL COMMENT '存储路径',
    detected_columns    JSON         DEFAULT NULL COMMENT '上传时检测到的列名',
    source              VARCHAR(16)  NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/SCHEDULED',
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREVIEWED/COMMITTED/FAILED',
    total_rows          INT          NOT NULL DEFAULT 0,
    success_rows        INT          NOT NULL DEFAULT 0,
    failed_rows         INT          NOT NULL DEFAULT 0,
    unmatched_rows      INT          NOT NULL DEFAULT 0 COMMENT '业务层未匹配行数',
    error_summary       VARCHAR(1024) DEFAULT NULL,
    operator            VARCHAR(64)  DEFAULT NULL,
    committed_time      DATETIME     DEFAULT NULL,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_import_batch_no (batch_no),
    KEY idx_sys_import_batch_biz (biz_type),
    KEY idx_sys_import_batch_profile (profile_id),
    KEY idx_sys_import_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文档导入批次';

-- ========== 3. 销售订单导入内置模板（按平台） ==========
INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '1688excel模版', 'SALES_ORDER', 1, 'platform:1', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单号',
           'order_time', '下单时间',
           'express_station_name', '物流公司',
           'received_amount', '实付款',
           'tracking_number', '运单号',
           'buyer_name', '买家',
           'receive_address', '收货地址',
           'link_name', '货品标题',
           'sku_spec_name', '规格',
           'sku_quantity', '数量',
           'platform_line_status', '退款状态',
           'platform_status', '订单状态'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退款成功', 'REFUNDED', '部分退款', 'PARTIAL_REFUND', '退款中', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '1688 平台订单导出默认列映射，可按实际导出列名调整'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 1 AND name = '1688excel模版');

INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '淘宝excel模版', 'SALES_ORDER', 2, 'platform:2', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单编号',
           'order_time', '买家下单时间',
           'pay_time', '买家付款时间',
           'ship_time', '发货时间',
           'complete_time', '确认收货时间',
           'express_station_name', '物流公司',
           'received_amount', '买家实付金额',
           'tracking_number', '运单号',
           'buyer_name', '买家会员名',
           'buyer_phone', '联系手机',
           'receive_address', '收货地址',
           'link_name', '宝贝标题',
           'sku_spec_name', '宝贝规格',
           'sku_quantity', '宝贝总数量',
           'platform_line_status', '订单状态',
           'platform_status', '订单状态'
       ),
       JSON_OBJECT(
           '交易成功', 'COMPLETED',
           '交易关闭', 'CANCELLED',
           '卖家已发货', 'SHIPPED',
           '等待买家确认', 'SHIPPED',
           '买家已付款', 'PAID',
           '等待卖家发货', 'PAID',
           '待发货', 'PAID',
           '已发货', 'SHIPPED',
           '已完成', 'COMPLETED',
           '已退款', 'REFUNDED',
           '退货退款', 'RETURNED',
           '已取消', 'CANCELLED'
       ),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '淘宝平台订单导出默认列映射，可按实际导出列名调整'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 2 AND name = '淘宝excel模版');
