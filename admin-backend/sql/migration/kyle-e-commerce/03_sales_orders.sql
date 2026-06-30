-- =============================================================================
-- 03 销售订单（头表 + 明细，由 e_real_order 扁平数据拆分）
-- order_no 规则：SOM + 旧表 id；platform_order_no 保留原平台单号
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

INSERT INTO ec_sales_order (
    id, order_no, shop_id, platform_order_no, source, status, platform_status,
    express_station_id, order_time, pay_time, ship_time, complete_time,
    tracking_number, received_amount, total_cost_amount,
    estimated_freight_amount, actual_freight_amount,
    has_shortage, deleted, create_time, update_time
)
SELECT
    ro.id,
    CONCAT('SOM', ro.id),
    ro.shop_id,
    ro.order_no,
    'IMPORT',
    CASE ro.order_status
        WHEN 0 THEN 'DRAFT'
        WHEN 1 THEN 'REFUNDED'
        WHEN 2 THEN 'COMPLETED'
        WHEN 3 THEN 'PARTIAL_REFUND'
        WHEN 4 THEN 'CANCELLED'
        WHEN 5 THEN 'REFUNDED'
        ELSE 'DRAFT'
    END,
    CASE ro.order_status
        WHEN 0 THEN '空白'
        WHEN 1 THEN '已签收，退款成功'
        WHEN 2 THEN '已签收'
        WHEN 3 THEN '已发货，退款成功'
        WHEN 4 THEN '已取消'
        WHEN 5 THEN '退款成功'
        ELSE NULL
    END,
    ro.express_site_id,
    IFNULL(ro.order_time, ro.gmt_created),
    IFNULL(ro.order_time, ro.gmt_created),
    CASE WHEN ro.order_status IN (2, 3) THEN IFNULL(ro.order_time, ro.gmt_created) ELSE NULL END,
    CASE WHEN ro.order_status IN (1, 2, 5) THEN IFNULL(ro.order_time, ro.gmt_created) ELSE NULL END,
    NULLIF(TRIM(ro.express_no), ''),
    ro.order_amount,
    NULLIF(ro.cost, 0),
    IFNULL(ro.express_cost, 0),
    IFNULL(ro.express_cost, 0),
    0,
    ro.is_deleted,
    ro.gmt_created,
    ro.gmt_modified
FROM `kyle-e-commerce`.e_real_order ro
INNER JOIN (
    SELECT shop_id, order_no, MIN(id) AS keep_id
    FROM `kyle-e-commerce`.e_real_order
    WHERE is_deleted = 0
    GROUP BY shop_id, order_no
) dedup ON ro.id = dedup.keep_id
WHERE ro.is_deleted = 0;

-- 订单明细（1:1）；匹配链接 SKU：同店铺 + sku_name
INSERT INTO ec_sales_order_line (
    order_id, sort_order, listing_link_sku_id,
    link_name, sku_spec_name, sku_codes, sku_quantity,
    shipped_quantity, short_quantity, status,
    unit_price, discount_pct, line_coupon_amount, line_received_amount,
    min_set_amount, platform_item_name,
    deleted, create_time, update_time
)
SELECT
    ro.id,
    0,
    (
        SELECT ss.id
        FROM `kyle-e-commerce`.e_shelf_sku ss
        INNER JOIN `kyle-e-commerce`.e_shelf_plan sp ON sp.id = ss.parent_id
        WHERE sp.shop_id = ro.shop_id
          AND ss.name = ro.sku_name
          AND ss.is_deleted = 0
          AND sp.is_deleted = 0
        ORDER BY ss.id
        LIMIT 1
    ),
    ro.order_title,
    ro.sku_name,
    (
        SELECT ss.product_nos
        FROM `kyle-e-commerce`.e_shelf_sku ss
        INNER JOIN `kyle-e-commerce`.e_shelf_plan sp ON sp.id = ss.parent_id
        WHERE sp.shop_id = ro.shop_id
          AND ss.name = ro.sku_name
          AND ss.is_deleted = 0
          AND sp.is_deleted = 0
        ORDER BY ss.id
        LIMIT 1
    ),
    GREATEST(IFNULL(ro.sku_number, 1), 1),
    CASE WHEN ro.order_status IN (2, 3) THEN GREATEST(IFNULL(ro.sku_number, 1), 1) ELSE 0 END,
    0,
    CASE ro.order_status
        WHEN 0 THEN 'PAID'
        WHEN 1 THEN 'REFUNDED'
        WHEN 2 THEN 'COMPLETED'
        WHEN 3 THEN 'PARTIAL_REFUND'
        WHEN 4 THEN 'CANCELLED'
        WHEN 5 THEN 'REFUNDED'
        ELSE 'PAID'
    END,
    CASE WHEN GREATEST(IFNULL(ro.sku_number, 1), 1) > 0
        THEN ROUND(ro.order_amount / GREATEST(IFNULL(ro.sku_number, 1), 1), 2)
        ELSE ro.order_amount
    END,
    (
        SELECT ss.discount
        FROM `kyle-e-commerce`.e_shelf_sku ss
        INNER JOIN `kyle-e-commerce`.e_shelf_plan sp ON sp.id = ss.parent_id
        WHERE sp.shop_id = ro.shop_id
          AND ss.name = ro.sku_name
          AND ss.is_deleted = 0
          AND sp.is_deleted = 0
        ORDER BY ss.id
        LIMIT 1
    ),
    (
        SELECT ss.coupon
        FROM `kyle-e-commerce`.e_shelf_sku ss
        INNER JOIN `kyle-e-commerce`.e_shelf_plan sp ON sp.id = ss.parent_id
        WHERE sp.shop_id = ro.shop_id
          AND ss.name = ro.sku_name
          AND ss.is_deleted = 0
          AND sp.is_deleted = 0
        ORDER BY ss.id
        LIMIT 1
    ),
    ro.order_amount,
    (
        SELECT ss.display_price
        FROM `kyle-e-commerce`.e_shelf_sku ss
        INNER JOIN `kyle-e-commerce`.e_shelf_plan sp ON sp.id = ss.parent_id
        WHERE sp.shop_id = ro.shop_id
          AND ss.name = ro.sku_name
          AND ss.is_deleted = 0
          AND sp.is_deleted = 0
        ORDER BY ss.id
        LIMIT 1
    ),
    ro.order_title,
    ro.is_deleted,
    ro.gmt_created,
    ro.gmt_modified
FROM `kyle-e-commerce`.e_real_order ro
INNER JOIN (
    SELECT shop_id, order_no, MIN(id) AS keep_id
    FROM `kyle-e-commerce`.e_real_order
    WHERE is_deleted = 0
    GROUP BY shop_id, order_no
) dedup ON ro.id = dedup.keep_id
WHERE ro.is_deleted = 0;

-- 成本补录回填（e_order_calculate_supplement）
UPDATE ec_sales_order so
INNER JOIN `kyle-e-commerce`.e_order_calculate_supplement sup ON sup.order_id = so.id
SET
    so.total_cost_amount = COALESCE(sup.product_cost, 0) + COALESCE(sup.package_cost, 0),
    so.estimated_freight_amount = COALESCE(sup.budget_express_cost, so.estimated_freight_amount),
    so.actual_freight_amount = COALESCE(sup.real_express_cost, so.actual_freight_amount),
    so.update_time = GREATEST(so.update_time, sup.gmt_modified);
