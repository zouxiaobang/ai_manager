-- 电商商品模块（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_factory (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工厂主键',
    name          VARCHAR(128) NOT NULL COMMENT '工厂名称',
    contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(64)  DEFAULT NULL COMMENT '联系方式',
    address       VARCHAR(512) DEFAULT NULL COMMENT '地址',
    remark        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_factory_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商工厂';

CREATE TABLE IF NOT EXISTS ec_product (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SPU 主键',
    factory_id   BIGINT        DEFAULT NULL COMMENT '所属工厂',
    name         VARCHAR(256)  NOT NULL COMMENT '商品名称(SPU)',
    description  TEXT          DEFAULT NULL COMMENT '商品描述',
    rebate_pct   DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，如 5.50 表示 5.5%)',
    image_name   VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    status       VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted      TINYINT       NOT NULL DEFAULT 0,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_product_status (status),
    KEY idx_ec_product_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商商品 SPU';

CREATE TABLE IF NOT EXISTS ec_sku (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU 主键',
    product_id            BIGINT        NOT NULL COMMENT '所属 SPU',
    sku_code              VARCHAR(64)   NOT NULL COMMENT '货号',
    spec_name             VARCHAR(128)  DEFAULT NULL COMMENT '规格名称',
    rebate_pct            DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，计算以 SKU 为准)',
    image_name            VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    carton_id             BIGINT        DEFAULT NULL COMMENT '匹配纸箱',
    sale_price            DECIMAL(12, 2) DEFAULT NULL COMMENT '销售价',
    product_length_cm     DECIMAL(10, 2) DEFAULT NULL COMMENT '单品长(cm)',
    product_width_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '单品宽(cm)',
    product_height_cm     DECIMAL(10, 2) DEFAULT NULL COMMENT '单品高(cm)',
    carton_length_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱长(cm)',
    carton_width_cm       DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱宽(cm)',
    carton_height_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱高(cm)',
    carton_gross_weight_kg DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱毛重(kg)',
    carton_net_weight_kg  DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱净重(kg)',
    units_per_carton      INT           NOT NULL DEFAULT 1 COMMENT '外箱装产品数量',
    status                VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/OFF_SALE/DRAFT',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_sku_code (sku_code),
    KEY idx_ec_sku_product (product_id),
    KEY idx_ec_sku_carton (carton_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商商品 SKU';

-- ========== 演示数据（重复执行前请先清空或跳过） ==========

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

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-W-350', '白色 350ml', 5.50, 'mug-white-350.jpg', 1, 29.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.500, 7.800, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-W-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-B-350', '黑色 350ml', 6.00, 'mug-black-350.jpg', 1, 31.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.600, 7.900, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-B-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-S-3L', '小号 3L', 8.00, 'box-small-3l.jpg', 2, 18.50, 20.00, 15.00, 12.00, 62.00, 42.00, 38.00, 6.200, 5.600, 20, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-S-3L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-L-8L', '大号 8L', 8.00, 'box-large-8l.jpg', 2, 32.00, 32.00, 22.00, 18.00, 68.00, 48.00, 42.00, 9.800, 8.900, 12, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-L-8L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-GR', '四件套 灰色', 6.00, 'mat-4pc-gray.jpg', 3, 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-GR');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-BE', '四件套 米色', 6.00, 'mat-4pc-beige.jpg', 3, 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-BE');
