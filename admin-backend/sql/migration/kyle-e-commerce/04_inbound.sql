-- =============================================================================
-- 04 入库单（每条采购记录生成一张入库单 + 一行明细）
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

INSERT INTO ec_inbound_order (
    id, order_no, factory_id, status, remark, order_time,
    expected_delivery_time, actual_receipt_time,
    deleted, create_time, update_time
)
SELECT
    pr.id,
    CONCAT('IB', LPAD(pr.id, 8, '0')),
    (
        SELECT p.product_company_id
        FROM `kyle-e-commerce`.e_product_item pi
        INNER JOIN `kyle-e-commerce`.e_product_info p ON p.id = pi.product_id AND p.is_deleted = 0
        WHERE pi.product_no = pr.sku_no
          AND pi.is_deleted = 0
        ORDER BY pi.id
        LIMIT 1
    ),
    CASE WHEN pr.is_finished = 1 THEN 'CONFIRMED' ELSE 'DRAFT' END,
    TRIM(CONCAT(
        IFNULL(pr.remark, ''),
        CASE WHEN pr.other_amount > 0 THEN CONCAT(' [其他费用:', pr.other_amount, ']') ELSE '' END
    )),
    pr.record_time,
    pr.record_time,
    CASE WHEN pr.is_finished = 1 THEN pr.record_time ELSE NULL END,
    pr.is_deleted,
    pr.gmt_created,
    pr.gmt_modified
FROM `kyle-e-commerce`.e_purchase_records pr
WHERE pr.is_deleted = 0;

INSERT INTO ec_inbound_order_line (
    order_id, sku_code, quantity, received_quantity,
    deleted, create_time, update_time
)
SELECT
    pr.id,
    pr.sku_no,
    pr.number,
    CASE WHEN pr.is_finished = 1 THEN pr.number ELSE NULL END,
    pr.is_deleted,
    pr.gmt_created,
    pr.gmt_modified
FROM `kyle-e-commerce`.e_purchase_records pr
WHERE pr.is_deleted = 0;
