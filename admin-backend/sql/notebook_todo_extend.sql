-- 待办扩展：提醒时间、重复规则（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN remind_time DATETIME DEFAULT NULL COMMENT '提醒时间' AFTER due_time,
    ADD COLUMN repeat_type VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY' AFTER remind_time,
    ADD COLUMN repeat_interval INT NOT NULL DEFAULT 1 COMMENT '重复间隔' AFTER repeat_type,
    ADD COLUMN repeat_until DATETIME DEFAULT NULL COMMENT '重复截止日期' AFTER repeat_interval,
    ADD COLUMN series_id BIGINT DEFAULT NULL COMMENT '重复系列 ID' AFTER repeat_until,
    ADD INDEX idx_remind_time (remind_time),
    ADD INDEX idx_series_id (series_id);
