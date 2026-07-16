USE ai_manager_admin;

UPDATE nb_note
SET content_hash = '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
    content_size = 30,
    content_version = 1,
    sync_status = 'CLOUD_PENDING',
    sync_error = '等待同步至云盘',
    update_time = CURRENT_TIMESTAMP
WHERE id = 17;

SELECT id, title, storage_type, storage_path, content_hash, content_size, content_version, sync_status, sync_error
FROM nb_note
WHERE id = 17;