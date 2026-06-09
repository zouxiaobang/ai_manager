-- 上架链接 SKU：按新公式回填 cost_price / min_set_amount（在 ai_manager_admin 库执行）
-- 前提：已执行 ecommerce_platform_link_alter.sql
-- 说明：
--   1. 仅处理 sku_codes 为单个货号（不含英文逗号）的行；组合货号请在管理端重新保存
--   2. 成本 = SKU 售价 + 纸箱单价 + 快递费（默认站点 + 广东省）
--   3. 最低设置金额 = 成本 ÷ (折扣/100) + 优惠券
--   4. 计费重量 = 外箱毛重 ÷ 每箱装数（装数>1 时），否则用外箱毛重，缺省 0.3kg
USE ai_manager_admin;

UPDATE ec_listing_link_sku s
INNER JOIN ec_sku sk
        ON sk.deleted = 0
       AND sk.sku_code = TRIM(s.sku_codes)
       AND s.sku_codes NOT LIKE '%,%'
LEFT JOIN ec_carton c
       ON c.deleted = 0
      AND c.id = sk.carton_id
INNER JOIN ec_express_station st
        ON st.deleted = 0
       AND st.is_default = 1
INNER JOIN ec_express_price ep
        ON ep.deleted = 0
       AND ep.station_id = st.id
       AND ep.province_name = '广东省'
SET
    s.cost_price = ROUND(
        sk.sale_price
        + COALESCE(c.unit_price, 0)
        + (
            CASE
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 0.3 THEN ep.price_w03_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 0.5 THEN ep.price_w05_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 1.0 THEN ep.price_w1_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 1.5 THEN ep.price_w15_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 2.0 THEN ep.price_w2_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 2.5 THEN ep.price_w25_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 3.0 THEN ep.price_w3_kg
                ELSE ep.over3_first_price
                     + ep.over3_additional_price * GREATEST(
                         CEILING(
                             GREATEST(
                                 COALESCE(
                                     CASE
                                         WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                             THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                         ELSE sk.carton_gross_weight_kg
                                     END,
                                     0.3
                                 ),
                                 0.001
                             )
                         ) - 3,
                         1
                     )
            END
        ),
        2
    ),
    s.min_set_amount = ROUND(
        (
            sk.sale_price
            + COALESCE(c.unit_price, 0)
            + (
                CASE
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 0.3 THEN ep.price_w03_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 0.5 THEN ep.price_w05_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 1.0 THEN ep.price_w1_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 1.5 THEN ep.price_w15_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 2.0 THEN ep.price_w2_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 2.5 THEN ep.price_w25_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 3.0 THEN ep.price_w3_kg
                    ELSE ep.over3_first_price
                         + ep.over3_additional_price * GREATEST(
                             CEILING(
                                 GREATEST(
                                     COALESCE(
                                         CASE
                                             WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                                 THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                             ELSE sk.carton_gross_weight_kg
                                         END,
                                         0.3
                                     ),
                                     0.001
                                 )
                             ) - 3,
                             1
                         )
                END
            )
        ) / (s.discount_pct / 100) + s.coupon_amount,
        2
    )
WHERE s.deleted = 0
  AND sk.sale_price IS NOT NULL;

-- 回填后抽查（马克杯演示链接预期：白色 cost=47.40 min=54.67，黑色 cost=49.40 min=56.89）
-- SELECT id, sku_name, sku_codes, discount_pct, coupon_amount, min_set_amount, cost_price
-- FROM ec_listing_link_sku
-- WHERE deleted = 0
-- ORDER BY link_id, sort_order;
