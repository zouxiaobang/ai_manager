-- 电商库存表 / 库存操作记录（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_inventory (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存主键',
    sku_code        VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    quantity        INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
    ignore_alert    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否忽略预警 1是 0否',
    alert_threshold INT          NOT NULL DEFAULT 0 COMMENT '预警数量(库存<=该值且未忽略预警时报警)',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_inventory_sku_code (sku_code),
    KEY idx_ec_inventory_quantity (quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商库存';

CREATE TABLE IF NOT EXISTS ec_inventory_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录主键',
    inventory_id BIGINT      NOT NULL COMMENT '库存表 ID',
    change_type  VARCHAR(16) NOT NULL COMMENT '改动方式 DEDUCT扣除 RECLAIM回收',
    change_qty   INT         NOT NULL COMMENT '改动数量(正数)',
    deleted      TINYINT     NOT NULL DEFAULT 0,
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_inventory_log_inventory (inventory_id),
    KEY idx_ec_inventory_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商库存操作记录';

-- ========== 演示数据 ==========

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'MUG-W-350', 120, 0, 20
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'MUG-W-350');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'MUG-B-350', 8, 0, 10
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'MUG-B-350');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'BOX-S-3L', 200, 0, 30
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'BOX-S-3L');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'BOX-L-8L', 45, 1, 15
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'BOX-L-8L');

INSERT INTO ec_inventory_log (inventory_id, change_type, change_qty)
SELECT i.id, 'RECLAIM', 50
FROM ec_inventory i
WHERE i.sku_code = 'MUG-W-350'
  AND NOT EXISTS (
      SELECT 1 FROM ec_inventory_log l
      WHERE l.inventory_id = i.id AND l.change_type = 'RECLAIM' AND l.change_qty = 50
  )
LIMIT 1;

INSERT INTO ec_inventory_log (inventory_id, change_type, change_qty)
SELECT i.id, 'DEDUCT', 12
FROM ec_inventory i
WHERE i.sku_code = 'MUG-W-350'
  AND NOT EXISTS (
      SELECT 1 FROM ec_inventory_log l
      WHERE l.inventory_id = i.id AND l.change_type = 'DEDUCT' AND l.change_qty = 12
  )
LIMIT 1;
