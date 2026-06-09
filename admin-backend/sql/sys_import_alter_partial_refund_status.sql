-- 状态映射：平台「部分退款」映射为 PARTIAL_REFUND（非 REFUNDED）
USE ai_manager_admin;

UPDATE sys_import_profile
SET value_mapping = JSON_SET(
        COALESCE(value_mapping, JSON_OBJECT()),
        '$.部分退款', 'PARTIAL_REFUND'
    )
WHERE biz_type = 'SALES_ORDER'
  AND JSON_UNQUOTE(JSON_EXTRACT(value_mapping, '$.部分退款')) = 'REFUNDED';
