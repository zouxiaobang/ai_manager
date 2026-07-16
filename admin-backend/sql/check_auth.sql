USE ai_manager_admin;
SELECT id, user_id, expires_at, baidu_uid FROM nb_baidu_pan_auth WHERE deleted = 0;
