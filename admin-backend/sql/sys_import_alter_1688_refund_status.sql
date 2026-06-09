-- 1688 导入：补充退款状态列映射与状态文案映射（支持订单部分退款聚合为 PARTIAL_REFUND）
-- 执行前请备份 sys_import_profile

UPDATE sys_import_profile
SET column_mapping = JSON_SET(
        column_mapping,
        '$.platform_line_status', '退款状态',
        '$.platform_status', '订单状态'
    ),
    value_mapping = JSON_MERGE_PATCH(
        COALESCE(value_mapping, JSON_OBJECT()),
        JSON_OBJECT(
            '退款成功', 'REFUNDED',
            '部分退款', 'PARTIAL_REFUND',
            '退款中', 'REFUNDED'
        )
    )
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 1
  AND name = '1688excel模版';
