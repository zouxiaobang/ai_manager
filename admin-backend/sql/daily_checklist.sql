-- 24小时重启系统每日检查清单表
USE ai_manager_admin;

DROP TABLE IF EXISTS daily_checklist;

CREATE TABLE IF NOT EXISTS daily_checklist (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    checklist_date  DATE         NOT NULL COMMENT '日期',
    item_key        VARCHAR(64)  NOT NULL COMMENT '检查项key',
    completed       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成 0/1',
    content         VARCHAR(512) DEFAULT NULL COMMENT '填写内容（复盘等）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_key (checklist_date, item_key),
    KEY idx_date (checklist_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='24小时重启系统每日检查清单';
