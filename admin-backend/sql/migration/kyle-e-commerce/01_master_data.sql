-- =============================================================================
-- 01 主数据：平台 / 店铺 / 工厂 / 快递
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

-- 平台
INSERT INTO ec_platform (
    id, name, name_en, avatar_url, platform_code, channel_type, remark, status,
    deleted, create_time, update_time
)
SELECT
    id,
    name,
    name_en,
    NULL,
    platform_type,
    'ONLINE',
    remark,
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_platform_info
WHERE is_deleted = 0;

-- 店铺
INSERT INTO ec_shop (
    id, name, name_en, avatar_url, platform_id, remark,
    default_receive_province, status, deleted, create_time, update_time
)
SELECT
    id,
    name,
    name_en,
    NULL,
    platform,
    remark,
    '广东省',
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_shop
WHERE is_deleted = 0;

-- 生产工厂
INSERT INTO ec_factory (
    id, name, factory_type, contact_name, contact_phone, address, remark, status,
    deleted, create_time, update_time
)
SELECT
    id,
    name,
    'PRODUCTION',
    contact,
    mobile,
    address,
    remark,
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_product_company
WHERE is_deleted = 0;

-- 纸箱供应商（ID 偏移 10000，避免与生产工厂冲突）
INSERT INTO ec_factory (
    id, name, factory_type, contact_name, contact_phone, address, remark, status,
    deleted, create_time, update_time
)
SELECT
    id + 10000,
    name,
    'CUSTOMER',
    NULL,
    mobile,
    address,
    TRIM(CONCAT(
        IFNULL(remark, ''),
        CASE
            WHEN threshold IS NOT NULL OR discount IS NOT NULL THEN
                CONCAT(' [threshold=', IFNULL(CAST(threshold AS CHAR), ''), ', discount=', IFNULL(CAST(discount AS CHAR), ''), ']')
            ELSE ''
        END
    )),
    'ENABLED',
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_box_company
WHERE is_deleted = 0;

-- 快递站点
INSERT INTO ec_express_station (
    id, name, avatar_url, contact, address, label_price, is_default,
    deleted, create_time, update_time
)
SELECT
    id,
    name,
    NULL,
    mobile,
    address,
    page_price,
    is_default,
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_express_site
WHERE is_deleted = 0;

-- 快递价目
INSERT INTO ec_express_price (
    id, station_id, province_name,
    price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg,
    over3_first_price, over3_additional_price,
    deleted, create_time, update_time
)
SELECT
    id,
    parent_id,
    provinces,
    kg03, kg05, kg10, kg15, kg20, kg25, kg30,
    fisrt_3kg, next_3kg,
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_express_item
WHERE is_deleted = 0;

-- 快递须知
INSERT INTO ec_express_notice (
    id, station_id, content, highlight_red, sort_order,
    deleted, create_time, update_time
)
SELECT
    id,
    parent_id,
    remark,
    red,
    `order`,
    is_deleted,
    gmt_created,
    gmt_modified
FROM `kyle-e-commerce`.e_express_remark
WHERE is_deleted = 0;
