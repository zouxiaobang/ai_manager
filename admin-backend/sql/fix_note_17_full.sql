USE ai_manager_admin;

UPDATE nb_note
SET title = 'AI概念',
    content_excerpt = 'Large Languaue Model：大语言模型 -> 大模型',
    content_size = 8000,
    sync_status = 'LOCAL_ONLY',
    sync_error = '百度网盘授权异常，已保留本地副本',
    update_time = CURRENT_TIMESTAMP
WHERE id = 17;

SELECT id, title, content_excerpt, content_size, sync_status, sync_error
FROM nb_note
WHERE id = 17;