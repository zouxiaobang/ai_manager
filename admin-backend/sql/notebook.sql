-- 笔记本模块表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS nb_notebook (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父文件夹 ID，NULL 为根级',
    name        VARCHAR(128) NOT NULL COMMENT '文件夹名称',
    icon        VARCHAR(32)  DEFAULT NULL COMMENT '图标标识',
    color       VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记本文件夹';

CREATE TABLE IF NOT EXISTS nb_note (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    notebook_id     BIGINT       DEFAULT NULL COMMENT '所属文件夹',
    title           VARCHAR(256) NOT NULL DEFAULT '' COMMENT '标题',
    storage_type    VARCHAR(16)  NOT NULL DEFAULT 'BAIDU_PAN' COMMENT 'BAIDU_PAN/LOCAL',
    storage_path    VARCHAR(512) NOT NULL DEFAULT '' COMMENT '网盘或本地路径',
    storage_fs_id   BIGINT       DEFAULT NULL COMMENT '百度 fs_id',
    content_hash    CHAR(64)     DEFAULT NULL COMMENT 'SHA-256',
    content_size    BIGINT       NOT NULL DEFAULT 0 COMMENT '正文字节数',
    content_version INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    content_excerpt VARCHAR(512) DEFAULT NULL COMMENT '纯文本摘要',
    sync_status     VARCHAR(16)  NOT NULL DEFAULT 'SYNCED' COMMENT 'SYNCING/SYNCED/FAILED',
    sync_error      VARCHAR(512) DEFAULT NULL COMMENT '同步失败原因',
    note_type       VARCHAR(16)  NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/TODO/MEMO',
    is_pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶',
    is_favorite     TINYINT      NOT NULL DEFAULT 0 COMMENT '收藏',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notebook_id (notebook_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记';

CREATE TABLE IF NOT EXISTS nb_note_tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '标签名',
    color       VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记标签';

CREATE TABLE IF NOT EXISTS nb_note_tag_rel (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    note_id BIGINT NOT NULL COMMENT '笔记 ID',
    tag_id  BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_tag (note_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记-标签关联';

CREATE TABLE IF NOT EXISTS nb_baidu_pan_auth (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL DEFAULT 1 COMMENT '用户 ID',
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

-- 示例数据
INSERT INTO nb_notebook (id, parent_id, name, sort_order)
SELECT 1, NULL, '工作', 1
WHERE NOT EXISTS (SELECT 1 FROM nb_notebook WHERE id = 1);

INSERT INTO nb_notebook (id, parent_id, name, sort_order)
SELECT 2, 1, 'java', 1
WHERE NOT EXISTS (SELECT 1 FROM nb_notebook WHERE id = 2);

INSERT INTO nb_notebook (id, parent_id, name, sort_order)
SELECT 3, 2, '基础', 1
WHERE NOT EXISTS (SELECT 1 FROM nb_notebook WHERE id = 3);

INSERT INTO nb_notebook (id, parent_id, name, sort_order)
SELECT 4, 2, '进阶', 2
WHERE NOT EXISTS (SELECT 1 FROM nb_notebook WHERE id = 4);

INSERT INTO nb_note (notebook_id, title, content_excerpt, sort_order)
SELECT 3, 'Java程序运行原理', 'JVM 加载、验证、准备、解析、初始化…', 1
WHERE NOT EXISTS (SELECT 1 FROM nb_note WHERE title = 'Java程序运行原理' AND deleted = 0);

INSERT INTO nb_note (notebook_id, title, content_excerpt, sort_order)
SELECT 4, 'CPU缓存和内存屏障', 'volatile、synchronized 与 happens-before…', 1
WHERE NOT EXISTS (SELECT 1 FROM nb_note WHERE title = 'CPU缓存和内存屏障' AND deleted = 0);

INSERT INTO nb_note_tag (name, color)
SELECT 'Java', '#409eff'
WHERE NOT EXISTS (SELECT 1 FROM nb_note_tag WHERE name = 'Java' AND deleted = 0);
