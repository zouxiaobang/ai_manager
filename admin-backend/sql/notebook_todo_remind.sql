-- 待办提醒已通知标记（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN remind_notified TINYINT NOT NULL DEFAULT 0 COMMENT '提醒是否已推送 0/1' AFTER repeat_until;
