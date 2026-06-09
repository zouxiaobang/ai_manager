-- 淘宝导入模板：补充发货/完成时间与平台状态列映射
UPDATE sys_import_profile
SET column_mapping = JSON_MERGE_PATCH(
        COALESCE(column_mapping, JSON_OBJECT()),
        JSON_OBJECT(
            'ship_time', '发货时间',
            'complete_time', '确认收货时间',
            'platform_status', '订单状态'
        )
    ),
    update_time = NOW()
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 2
  AND name = '淘宝excel模版';
