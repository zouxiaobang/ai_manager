-- 已有电商表时追加 ec_carton（在 ai_manager_admin 库执行）
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

-- 演示数据见 ecommerce_carton.sql 末尾 INSERT 语句
