-- 电商演示数据（可重复执行，已存在则跳过）
USE ai_manager_admin;

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 1, '东莞精品日用制造厂', '张经理', '13800138001', '广东省东莞市虎门镇工业路 88 号', '主打陶瓷杯、厨房小件，交期稳定', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 1);

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 2, '义乌源头小商品工厂', '李总', '13900139002', '浙江省义乌市北苑街道春晗路 126 号', '收纳类、塑料家居，支持贴牌', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 2);

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 3, '宁波竹木家居用品厂', '王姐', '13700137003', '浙江省宁波市慈溪市逍林镇竹艺园区 5 栋', '竹纤维、餐垫砧板，出口品质', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 3);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 1, 1, '简约陶瓷马克杯套装', '高温陶瓷，可进洗碗机，礼盒装', 5.50, 'mug-set-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 1);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 2, 2, '多层厨房收纳盒', 'PP 材质，可叠加，厨房/衣柜通用', 8.00, 'storage-box-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 2);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 3, 3, '竹纤维餐垫四件套', '天然竹纤维，防滑耐热，四色一组', 6.00, 'bamboo-mat-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 3);

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-W-350', '白色 350ml', 5.50, 'mug-white-350.jpg', 29.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.500, 7.800, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-W-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-B-350', '黑色 350ml', 6.00, 'mug-black-350.jpg', 31.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.600, 7.900, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-B-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-S-3L', '小号 3L', 8.00, 'box-small-3l.jpg', 18.50, 20.00, 15.00, 12.00, 62.00, 42.00, 38.00, 6.200, 5.600, 20, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-S-3L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-L-8L', '大号 8L', 8.00, 'box-large-8l.jpg', 32.00, 32.00, 22.00, 18.00, 68.00, 48.00, 42.00, 9.800, 8.900, 12, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-L-8L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-GR', '四件套 灰色', 6.00, 'mat-4pc-gray.jpg', 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-GR');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-BE', '四件套 米色', 6.00, 'mat-4pc-beige.jpg', 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-BE');
