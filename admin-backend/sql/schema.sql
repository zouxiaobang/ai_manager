CREATE DATABASE IF NOT EXISTS ai_manager_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

INSERT INTO sys_user (username, nickname, status) VALUES
('admin', '管理员', 'ENABLED'),
('demo', '演示用户', 'ENABLED');
