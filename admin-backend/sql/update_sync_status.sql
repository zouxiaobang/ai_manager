USE ai_manager_admin;

UPDATE nb_note
SET sync_status = 'LOCAL_ONLY',
    sync_error = '百度网盘授权异常(errno=102)，已保留本地副本',
    content_hash = '7a2f1d4e8c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f',
    content_version = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 17;

SELECT id, title, content_excerpt, content_size, sync_status, sync_error, content_version
FROM nb_note
WHERE id = 17;