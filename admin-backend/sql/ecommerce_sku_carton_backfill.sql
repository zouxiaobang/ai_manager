-- 为已有 SKU 按单品尺寸匹配并回填 carton_id（需先执行 ecommerce_sku_carton_alter.sql）
USE ai_manager_admin;

-- 演示 SKU（与 EcCartonMatcher 计算结果一致）
UPDATE ec_sku SET carton_id = 1 WHERE sku_code IN ('MUG-W-350', 'MUG-B-350');
UPDATE ec_sku SET carton_id = 2 WHERE sku_code = 'BOX-S-3L';
UPDATE ec_sku SET carton_id = 2 WHERE sku_code = 'BOX-L-8L';
UPDATE ec_sku SET carton_id = 3 WHERE sku_code IN ('MAT-4PC-GR', 'MAT-4PC-BE');

-- 其余有完整单品尺寸、尚未配置 carton_id 的 SKU：优先同工厂最小体积纸箱（同向尺寸比较）
UPDATE ec_sku s
INNER JOIN ec_product p ON s.product_id = p.id
INNER JOIN (
    SELECT
        s2.id AS sku_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(c.id ORDER BY (c.length_cm * c.width_cm * c.height_cm) ASC, c.id ASC),
            ',', 1
        ) + 0 AS carton_id
    FROM ec_sku s2
    INNER JOIN ec_product p2 ON s2.product_id = p2.id
    INNER JOIN ec_carton c ON c.deleted = 0
        AND c.length_cm >= s2.product_length_cm
        AND c.width_cm >= s2.product_width_cm
        AND c.height_cm >= s2.product_height_cm
        AND (p2.factory_id IS NULL OR c.factory_id = p2.factory_id)
    WHERE s2.deleted = 0
      AND s2.product_length_cm IS NOT NULL
      AND s2.product_width_cm IS NOT NULL
      AND s2.product_height_cm IS NOT NULL
      AND s2.product_length_cm > 0
      AND s2.product_width_cm > 0
      AND s2.product_height_cm > 0
      AND (s2.carton_id IS NULL OR s2.carton_id = 0)
    GROUP BY s2.id
) matched ON matched.sku_id = s.id
SET s.carton_id = matched.carton_id
WHERE s.carton_id IS NULL OR s.carton_id = 0;

-- 同工厂无匹配时，放宽到全部纸箱
UPDATE ec_sku s
INNER JOIN (
    SELECT
        s2.id AS sku_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(c.id ORDER BY (c.length_cm * c.width_cm * c.height_cm) ASC, c.id ASC),
            ',', 1
        ) + 0 AS carton_id
    FROM ec_sku s2
    INNER JOIN ec_carton c ON c.deleted = 0
        AND c.length_cm >= s2.product_length_cm
        AND c.width_cm >= s2.product_width_cm
        AND c.height_cm >= s2.product_height_cm
    WHERE s2.deleted = 0
      AND (s2.carton_id IS NULL OR s2.carton_id = 0)
      AND s2.product_length_cm IS NOT NULL
      AND s2.product_width_cm IS NOT NULL
      AND s2.product_height_cm IS NOT NULL
      AND s2.product_length_cm > 0
      AND s2.product_width_cm > 0
      AND s2.product_height_cm > 0
    GROUP BY s2.id
) matched ON matched.sku_id = s.id
SET s.carton_id = matched.carton_id
WHERE s.carton_id IS NULL OR s.carton_id = 0;
