-- 笔记正文外置存储（百度网盘 / 本地文件）迁移脚本
-- 在 ai_manager_admin 库执行；可重复执行（部分语句若已执行会报错可忽略）

USE ai_manager_admin;

ALTER TABLE nb_note
    ADD COLUMN storage_type VARCHAR(16) NOT NULL DEFAULT 'BAIDU_PAN' COMMENT 'BAIDU_PAN/LOCAL' AFTER title;

ALTER TABLE nb_note
    ADD COLUMN storage_path VARCHAR(512) NOT NULL DEFAULT '' COMMENT '网盘或本地相对路径' AFTER storage_type;

ALTER TABLE nb_note
    ADD COLUMN storage_fs_id BIGINT DEFAULT NULL COMMENT '百度 fs_id' AFTER storage_path;

ALTER TABLE nb_note
    ADD COLUMN content_hash CHAR(64) DEFAULT NULL COMMENT 'SHA-256' AFTER storage_fs_id;

ALTER TABLE nb_note
    ADD COLUMN content_size BIGINT NOT NULL DEFAULT 0 COMMENT '正文字节数' AFTER content_hash;

ALTER TABLE nb_note
    ADD COLUMN content_version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本' AFTER content_size;

ALTER TABLE nb_note
    ADD COLUMN content_excerpt VARCHAR(512) DEFAULT NULL COMMENT '纯文本摘要' AFTER content_version;

ALTER TABLE nb_note
    ADD COLUMN sync_status VARCHAR(16) NOT NULL DEFAULT 'SYNCED' COMMENT 'SYNCING/SYNCED/FAILED' AFTER content_excerpt;

ALTER TABLE nb_note
    ADD COLUMN sync_error VARCHAR(512) DEFAULT NULL COMMENT '同步失败原因' AFTER sync_status;

CREATE TABLE IF NOT EXISTS nb_baidu_pan_auth (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '用户 ID，单用户默认 1',
    access_token  VARCHAR(512) NOT NULL COMMENT '访问令牌',
    refresh_token VARCHAR(512) NOT NULL COMMENT '刷新令牌',
    expires_at    DATETIME     NOT NULL COMMENT 'access_token 过期时间',
    baidu_uid     BIGINT       DEFAULT NULL COMMENT '百度用户 ID',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='百度网盘 OAuth 授权';

-- 历史 content 列保留，应用启动时会自动迁移到外置存储后可手动执行：
-- ALTER TABLE nb_note DROP COLUMN content;
