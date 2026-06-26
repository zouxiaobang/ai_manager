-- 待办重复：周几 / 每月几号 / 每年哪些日期（MM-DD，逗号分隔）
ALTER TABLE nb_todo_item
    ADD COLUMN repeat_days VARCHAR(255) DEFAULT NULL COMMENT 'WEEKLY:1,3,5 MONTHLY:1,15 YEARLY:01-15,06-01' AFTER repeat_until;
