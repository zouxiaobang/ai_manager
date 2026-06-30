-- 月结统计结果快照（点击「统计」后入库，下次进入按月份读取）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_settlement_snapshot (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    settlement_month      CHAR(7)      NOT NULL COMMENT '统计月份 YYYY-MM',
    express_bill_imported TINYINT      NOT NULL DEFAULT 0 COMMENT '统计时是否已导入快递账单',
    snapshot_json         LONGTEXT     NOT NULL COMMENT 'EcMonthlySettlementVO JSON 快照',
    calculated_at         DATETIME     NOT NULL COMMENT '统计完成时间',
    deleted               TINYINT      NOT NULL DEFAULT 0,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_snapshot_month (settlement_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结统计结果快照';
