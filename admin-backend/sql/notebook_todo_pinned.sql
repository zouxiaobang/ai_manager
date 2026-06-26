-- 待办特别提醒标记（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 COMMENT '特别提醒 0/1' AFTER sort_order,
    ADD INDEX idx_pinned (pinned);
