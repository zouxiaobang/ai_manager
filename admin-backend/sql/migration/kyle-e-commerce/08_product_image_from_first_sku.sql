-- =============================================================================
-- 08 SPU 头像：取每个商品下第一个 SKU 的 image_name
-- =============================================================================
USE ai_manager_admin;
SET NAMES utf8mb4;

UPDATE ec_product p
INNER JOIN (
    SELECT s.product_id, s.image_name
    FROM ec_sku s
    INNER JOIN (
        SELECT product_id, MIN(id) AS min_id
        FROM ec_sku
        WHERE deleted = 0
        GROUP BY product_id
    ) first_sku ON s.id = first_sku.min_id
    WHERE s.deleted = 0
      AND s.image_name IS NOT NULL
      AND TRIM(s.image_name) != ''
) src ON p.id = src.product_id
SET p.image_name = src.image_name,
    p.update_time = NOW()
WHERE p.deleted = 0;
