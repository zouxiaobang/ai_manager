USE ai_manager_admin;

UPDATE nb_note
SET title = 'AI概念',
    content_excerpt = 'AI概念相关笔记内容',
    update_time = CURRENT_TIMESTAMP
WHERE id = 17;

SELECT id, title, storage_type, storage_path, content_excerpt, sync_status
FROM nb_note
WHERE id = 17;