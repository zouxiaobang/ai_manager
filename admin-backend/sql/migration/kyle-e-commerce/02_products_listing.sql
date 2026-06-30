-- =============================================================================
-- 02 产品 / 纸箱 / SKU / 库存 / 上架链接
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

-- 纸箱（factory_id = 纸箱公司 id + 10000；解析 size 为长宽髙）
INSERT INTO ec_carton (
    id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark,
    illustration_variant, preview_image, deleted, create_time, update_time
)
SELECT
    bi.id,
    bi.parent_id + 10000,
    bi.name,
    CAST(
        CASE
            WHEN bi.size IS NULL OR TRIM(bi.size) IN ('', '0') THEN NULL
            WHEN REPLACE(LOWER(bi.size), 'x', '*') NOT LIKE '%*%' THEN NULL
            ELSE NULLIF(TRIM(SUBSTRING_INDEX(REPLACE(LOWER(bi.size), 'x', '*'), '*', 1)), '')
        END AS DECIMAL(10, 2)
    ),
    CAST(
        CASE
            WHEN bi.size IS NULL OR TRIM(bi.size) IN ('', '0') THEN NULL
            WHEN REPLACE(LOWER(bi.size), 'x', '*') NOT LIKE '%*%' THEN NULL
            ELSE NULLIF(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(REPLACE(LOWER(bi.size), 'x', '*'), '*', 2), '*', -1)), '')
        END AS DECIMAL(10, 2)
    ),
    CAST(
        CASE
            WHEN bi.size IS NULL OR TRIM(bi.size) IN ('', '0') THEN NULL
            WHEN REPLACE(LOWER(bi.size), 'x', '*') NOT LIKE '%*%' THEN NULL
            ELSE NULLIF(TRIM(SUBSTRING_INDEX(REPLACE(LOWER(bi.size), 'x', '*'), '*', -1)), '')
        END AS DECIMAL(10, 2)
    ),
    bi.price,
    bi.remark,
    NULL,
    NULL,
    bi.is_deleted,
    bi.gmt_created,
    bi.gmt_modified
FROM `kyle-e-commerce`.e_box_item bi
WHERE bi.is_deleted = 0;

-- SPU
INSERT INTO ec_product (
    id, factory_id, name, description, rebate_pct, image_name, status,
    deleted, create_time, update_time
)
SELECT
    id,
    product_company_id,
    name,
    remark,
    IFNULL(rebate, 0),
    NULL,
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_product_info
WHERE is_deleted = 0;

-- SKU
INSERT INTO ec_sku (
    id, product_id, sku_code, spec_name, rebate_pct, image_name, carton_id,
    sale_price, product_length_cm, product_width_cm, product_height_cm,
    carton_length_cm, carton_width_cm, carton_height_cm,
    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status,
    deleted, create_time, update_time
)
SELECT
    pi.id,
    pi.product_id,
    pi.product_no,
    pi.name,
    IFNULL(pi.rebate, 0),
    pi.product_img,
    NULL,
    pi.price,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    pi.gross_weight,
    pi.net_weight,
    GREATEST(IFNULL(pi.inner_boxes_number, 1), 1),
    'ON_SALE',
    pi.is_deleted,
    pi.gmt_created,
    pi.gmt_modified
FROM `kyle-e-commerce`.e_product_item pi
WHERE pi.is_deleted = 0;

-- 由上架链接反填 SKU 纸箱 ID（取链接上最常见的 package_no）
UPDATE ec_sku s
INNER JOIN (
    SELECT
        pi.product_no AS sku_code,
        MAX(ss.package_no) AS carton_id
    FROM `kyle-e-commerce`.e_shelf_sku ss
    INNER JOIN `kyle-e-commerce`.e_product_item pi
        ON FIND_IN_SET(pi.product_no COLLATE utf8mb4_unicode_ci, REPLACE(ss.product_nos, ' ', '') COLLATE utf8mb4_unicode_ci) > 0
    WHERE ss.is_deleted = 0
      AND pi.is_deleted = 0
      AND ss.package_no IS NOT NULL
    GROUP BY pi.product_no
) m ON m.sku_code COLLATE utf8mb4_unicode_ci = s.sku_code COLLATE utf8mb4_unicode_ci
SET s.carton_id = m.carton_id;

-- 库存
INSERT INTO ec_inventory (
    id, sku_code, quantity, ignore_alert, alert_threshold,
    deleted, create_time, update_time
)
SELECT
    id,
    sku_no,
    inventory,
    ignore_warning,
    warning_threshold,
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_inventory
WHERE is_deleted = 0;

-- 上架链接
INSERT INTO ec_listing_link (
    id, name, shop_id, platform_url, listing_time, remark, status,
    deleted, create_time, update_time
)
SELECT
    id,
    name,
    shop_id,
    NULL,
    IFNULL(shelf_time, gmt_created),
    NULL,
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_shelf_plan
WHERE is_deleted = 0;

-- 链接 SKU
INSERT INTO ec_listing_link_sku (
    id, link_id, sku_name, sku_codes, discount_pct, coupon_amount,
    min_set_amount, actual_set_amount, sort_order,
    deleted, create_time, update_time
)
SELECT
    id,
    parent_id,
    name,
    product_nos,
    discount,
    coupon,
    display_price,
    actual_price,
    0,
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_shelf_sku
WHERE is_deleted = 0;

-- 链接关联 SPU（由货号反查 product_id 去重）
INSERT INTO ec_listing_link_product (
    link_id, product_id, sort_order, deleted, create_time, update_time
)
SELECT DISTINCT
    ss.parent_id,
    pi.product_id,
    0,
    0,
    NOW(),
    NOW()
FROM `kyle-e-commerce`.e_shelf_sku ss
INNER JOIN `kyle-e-commerce`.e_product_item pi
    ON FIND_IN_SET(pi.product_no COLLATE utf8mb4_unicode_ci, REPLACE(ss.product_nos, ' ', '') COLLATE utf8mb4_unicode_ci) > 0
WHERE ss.is_deleted = 0
  AND pi.is_deleted = 0;
