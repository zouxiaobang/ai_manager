-- 笔记本独立待办表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

DROP TABLE IF EXISTS nb_note_todo_item;

CREATE TABLE IF NOT EXISTS nb_todo_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    content         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '待办内容',
    completed       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成 0/1',
    due_time        DATETIME     DEFAULT NULL COMMENT '截止时间',
    remind_time     DATETIME     DEFAULT NULL COMMENT '提醒时间',
    repeat_type     VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
    repeat_interval INT          NOT NULL DEFAULT 1 COMMENT '重复间隔',
    repeat_until    DATETIME     DEFAULT NULL COMMENT '重复截止日期',
    remind_notified TINYINT      NOT NULL DEFAULT 0 COMMENT '提醒是否已推送 0/1',
    series_id       BIGINT       DEFAULT NULL COMMENT '重复系列 ID',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_completed (completed),
    KEY idx_due_time (due_time),
    KEY idx_remind_time (remind_time),
    KEY idx_series_id (series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记本待办';
