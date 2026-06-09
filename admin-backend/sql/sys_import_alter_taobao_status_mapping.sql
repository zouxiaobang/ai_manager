-- 更新淘宝导入模板的订单状态 value_mapping（支持「交易成功」「交易关闭」等淘宝导出文案）
UPDATE sys_import_profile
SET value_mapping = JSON_OBJECT(
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
    update_time = NOW()
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 2
  AND name = '淘宝excel模版';
