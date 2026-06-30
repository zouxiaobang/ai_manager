-- =============================================================================
-- 05 月结：订单纳入决策 + 月度快照（简化 JSON，供历史参考）
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

-- 月结订单纳入决策
INSERT INTO ec_settlement_order_decision (
    shop_id, order_id, settlement_month, included, deleted, create_time, update_time
)
SELECT
    ro.shop_id,
    oct.order_id,
    DATE_FORMAT(IFNULL(ro.order_time, ro.gmt_created), '%Y-%m'),
    oct.can_calculate,
    0,
    oct.gmt_created,
    oct.gmt_modified
FROM `kyle-e-commerce`.e_order_classification_temp oct
INNER JOIN `kyle-e-commerce`.e_real_order ro ON ro.id = oct.order_id
WHERE ro.is_deleted = 0;

-- 月度快照（按月份聚合各店铺，JSON 存档）
INSERT INTO ec_settlement_snapshot (
    settlement_month, express_bill_imported, snapshot_json, calculated_at,
    deleted, create_time, update_time
)
SELECT
    mo.month AS settlement_month,
    0,
    JSON_OBJECT(
        'source', 'kyle-e-commerce',
        'migration', TRUE,
        'shops', JSON_ARRAYAGG(
            JSON_OBJECT(
                'shopId', mo.shop_id,
                'profitCalculated', mo.profit_calculated,
                'turnoverCalculated', mo.turnover_calculated,
                'costCalculated', mo.cost_calculated,
                'profitManually', mo.profit_manually,
                'turnoverManually', mo.turnover_manually,
                'costManually', mo.cost_manually,
                'profitActual', mo.profit_actual,
                'turnoverActual', mo.turnover_actual,
                'maxOrderNo', mo.max_order_no,
                'maxProfit', mo.max_profit,
                'maxTurnover', mo.max_turnover
            )
        )
    ),
    MAX(mo.gmt_modified),
    0,
    MIN(mo.gmt_created),
    MAX(mo.gmt_modified)
FROM `kyle-e-commerce`.e_month_order mo
WHERE mo.is_deleted = 0
GROUP BY mo.month;
