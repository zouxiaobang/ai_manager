-- 移除销售订单导入配置中不再提供的列映射项
-- （手动成本、行实收、支付详情、买家应付货款 — 改由系统逻辑或入库界面处理）
-- 执行前请备份 sys_import_profile

UPDATE sys_import_profile
SET column_mapping = JSON_REMOVE(
        JSON_REMOVE(
                JSON_REMOVE(
                        JSON_REMOVE(column_mapping, '$.line_received_amount'),
                        '$.buyer_goods_amount'),
                '$.pay_detail'),
        '$.manual_cost_price')
WHERE biz_type = 'SALES_ORDER'
  AND (
        JSON_CONTAINS_PATH(column_mapping, 'one', '$.line_received_amount')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.buyer_goods_amount')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.pay_detail')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.manual_cost_price')
    );
