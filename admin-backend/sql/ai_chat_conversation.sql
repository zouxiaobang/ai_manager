-- AI 知识库对话管理（跨设备同步）
-- 在 ai_manager_admin 库执行
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ai_chat_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库对话分类';

CREATE TABLE IF NOT EXISTS ai_chat_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT       NOT NULL COMMENT '所属分类 ID',
    title       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '对话标题',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库对话';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT       NOT NULL COMMENT '所属对话 ID',
    msg_id          VARCHAR(64)  NOT NULL COMMENT '客户端消息 ID',
    role            VARCHAR(16)  NOT NULL COMMENT '消息角色：user/assistant/system',
    content         MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库对话消息';
