-- 电商纸箱表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_carton (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '纸箱主键',
    factory_id  BIGINT        DEFAULT NULL COMMENT '所属工厂',
    name        VARCHAR(128)  NOT NULL COMMENT '纸箱名称',
    length_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '长(cm)',
    width_cm    DECIMAL(10, 2) DEFAULT NULL COMMENT '宽(cm)',
    height_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '高(cm)',
    unit_price  DECIMAL(12, 2) DEFAULT NULL COMMENT '单价',
    remark      VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    deleted     TINYINT       NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_carton_factory (factory_id),
    KEY idx_ec_carton_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商纸箱';

-- ========== 演示数据（重复执行前请先清空或跳过） ==========

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 1, 1, '五层瓦楞外箱-中号', 42.00, 32.00, 28.00, 3.50, '适用于马克杯 24 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 1);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 2, 2, 'PP收纳专用箱', 62.00, 42.00, 38.00, 4.20, '小号收纳盒 20 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 2);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 3, 3, '竹纤维餐垫彩箱', 48.00, 34.00, 26.00, 2.80, '四件套餐垫 30 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 3);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 4, 2, '加厚物流箱-大号', 68.00, 48.00, 42.00, 5.60, '大号收纳盒 12 装，加强耐压'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 4);
