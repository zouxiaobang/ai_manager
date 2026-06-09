-- 电商快递站点 / 价格 / 须知（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_express_station (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '站点主键',
    name        VARCHAR(128) NOT NULL COMMENT '快递名称',
    contact     VARCHAR(256) DEFAULT NULL COMMENT '联系方式',
    address     VARCHAR(512) DEFAULT NULL COMMENT '地址',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_express_station_name (name),
    KEY idx_ec_express_station_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递站点';

CREATE TABLE IF NOT EXISTS ec_express_price (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '价格主键',
    station_id            BIGINT        NOT NULL COMMENT '站点ID',
    province_name         VARCHAR(64)   NOT NULL COMMENT '省份名称',
    price_w03_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '0.3kg价格',
    price_w05_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '0.5kg价格',
    price_w1_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '1kg价格',
    price_w15_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '1.5kg价格',
    price_w2_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '2kg价格',
    price_w25_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '2.5kg价格',
    price_w3_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '3kg价格',
    over3_first_price     DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg首重价格',
    over3_additional_price DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg续重价格',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_express_price_station_province (station_id, province_name),
    KEY idx_ec_express_price_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递价格';

CREATE TABLE IF NOT EXISTS ec_express_notice (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '须知主键',
    station_id    BIGINT       NOT NULL COMMENT '站点ID',
    content       TEXT         NOT NULL COMMENT '须知内容',
    highlight_red TINYINT      NOT NULL DEFAULT 0 COMMENT '是否标红 1是 0否',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_express_notice_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递须知';

-- ========== 演示数据 ==========

INSERT INTO ec_express_station (id, name, contact, address, is_default)
SELECT 1, '顺丰标快', '95338', '广东省深圳市南山区顺丰总部营业点', 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_station WHERE id = 1);

INSERT INTO ec_express_station (id, name, contact, address, is_default)
SELECT 2, '中通快递', '95311', '浙江省杭州市余杭区中通转运中心', 0
WHERE NOT EXISTS (SELECT 1 FROM ec_express_station WHERE id = 2);

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 1, '广东省', 12.00, 14.00, 18.00, 22.00, 26.00, 30.00, 34.00, 34.00, 6.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 1 AND province_name = '广东省');

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 1, '北京市', 15.00, 17.00, 22.00, 26.00, 30.00, 34.00, 38.00, 38.00, 8.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 1 AND province_name = '北京市');

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 2, '广东省', 8.00, 9.00, 11.00, 13.00, 15.00, 17.00, 19.00, 19.00, 4.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 2 AND province_name = '广东省');

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 1, '生鲜、易碎品请提前告知客服并加强包装。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 1 AND sort_order = 1);

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 1, '偏远地区可能产生附加费用，以实际揽收为准。', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 1 AND sort_order = 2);

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 2, '中通不支持液体、粉末等违禁品寄递。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 2 AND sort_order = 1);
