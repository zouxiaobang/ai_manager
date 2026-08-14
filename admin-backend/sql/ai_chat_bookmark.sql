-- AI 知识库对话标记（跨设备同步）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ai_chat_bookmark (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT       NOT NULL COMMENT '所属对话 ID',
    name            VARCHAR(100) NOT NULL COMMENT '标记名称',
    msg_id          VARCHAR(64)  NULL COMMENT '定位锚点消息 id（记录时视口附近消息，可空）',
    msg_offset_top  INT          NOT NULL DEFAULT 0 COMMENT '锚点消息记录时相对容器内容顶部的偏移',
    scroll_top      INT          NOT NULL DEFAULT 0 COMMENT '记录时的容器滚动位置',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库对话标记';
