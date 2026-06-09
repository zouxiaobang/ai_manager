-- ============================================================
-- sys_import_profile 增加 platform_id（已有 sys_import 表时执行）
-- ============================================================

USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_import_profile'
      AND COLUMN_NAME = 'platform_id'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_import_profile
        ADD COLUMN platform_id BIGINT DEFAULT NULL COMMENT ''绑定平台 ec_platform.id'' AFTER biz_type,
        ADD KEY idx_sys_import_profile_platform (platform_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 从 extra_config.platformCode 回填 platform_id
UPDATE sys_import_profile sp
INNER JOIN ec_platform ep ON ep.platform_code = CAST(JSON_UNQUOTE(JSON_EXTRACT(sp.extra_config, '$.platformCode')) AS UNSIGNED)
SET sp.platform_id = ep.id,
    sp.scope_key = CONCAT('platform:', ep.id)
WHERE sp.platform_id IS NULL
  AND JSON_EXTRACT(sp.extra_config, '$.platformCode') IS NOT NULL;

-- 删除旧的全局模板，插入按平台模板（若不存在）
DELETE FROM sys_import_profile
WHERE biz_type = 'SALES_ORDER' AND platform_id IS NULL AND name = '系统标准模板';

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
           'line_received_amount', '单品金额'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '1688 平台订单导出默认列映射'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 1 AND name = '1688excel模版');

INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '淘宝excel模版', 'SALES_ORDER', 2, 'platform:2', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单编号',
           'order_time', '买家下单时间',
           'pay_time', '买家付款时间',
           'express_station_name', '物流公司',
           'received_amount', '买家实付金额',
           'tracking_number', '运单号',
           'buyer_name', '买家会员名',
           'buyer_phone', '联系手机',
           'receive_address', '收货地址',
           'link_name', '宝贝标题',
           'sku_spec_name', '宝贝规格',
           'sku_quantity', '宝贝总数量',
           'buyer_goods_amount', '买家应付货款',
           'line_received_amount', '买家应付货款',
           'received_amount', '买家实付金额',
           'platform_line_status', '订单状态'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '淘宝平台订单导出默认列映射'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 2 AND name = '淘宝excel模版');
