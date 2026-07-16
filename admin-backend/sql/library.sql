-- 文档库模块表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS doc_library_folder (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档库文件夹';

CREATE TABLE IF NOT EXISTS doc_library_file (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    folder_id       BIGINT       DEFAULT NULL COMMENT '所属文件夹 ID',
    name            VARCHAR(256) NOT NULL COMMENT '文件名',
    original_name   VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    extension       VARCHAR(32)  DEFAULT NULL COMMENT '扩展名',
    mime_type       VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
    file_size       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    storage_type    VARCHAR(32)  DEFAULT NULL COMMENT '存储类型',
    storage_path    VARCHAR(512) DEFAULT NULL COMMENT '存储路径',
    storage_key     VARCHAR(256) DEFAULT NULL COMMENT '存储 Key',
    content_hash    VARCHAR(64)  DEFAULT NULL COMMENT '文件哈希',
    thumbnail_path  VARCHAR(512) DEFAULT NULL COMMENT '缩略图路径',
    is_pinned       TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶',
    description     VARCHAR(512) DEFAULT NULL COMMENT '描述',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    kb_status       VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT '知识库状态 NONE/READY/PROCESSING/FAILED',
    kb_error        VARCHAR(512) DEFAULT NULL COMMENT '知识库处理错误信息',
    kb_processed_at DATETIME     DEFAULT NULL COMMENT '知识库处理时间',
    view_count      INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    download_count  INT          NOT NULL DEFAULT 0 COMMENT '下载次数',
    deleted_at      DATETIME     DEFAULT NULL COMMENT '删除时间（回收站）',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_folder_id (folder_id),
    KEY idx_extension (extension),
    KEY idx_kb_status (kb_status),
    KEY idx_deleted_at (deleted_at),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档库文件';

CREATE TABLE IF NOT EXISTS doc_library_tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '标签名',
    color       VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档库标签';

CREATE TABLE IF NOT EXISTS doc_library_file_tag (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_id BIGINT NOT NULL COMMENT '文件 ID',
    tag_id  BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_tag (file_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档库文件-标签关联';

CREATE TABLE IF NOT EXISTS doc_library_event_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event       VARCHAR(64)  NOT NULL COMMENT '事件类型',
    file_id     BIGINT       DEFAULT NULL COMMENT '文件 ID',
    folder_id   BIGINT       DEFAULT NULL COMMENT '文件夹 ID',
    params_json JSON         DEFAULT NULL COMMENT '事件参数 JSON',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_event (event),
    KEY idx_file_id (file_id),
    KEY idx_folder_id (folder_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档库事件日志';
