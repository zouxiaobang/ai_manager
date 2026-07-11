USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS pixel_dog_state (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    level               INT          NOT NULL DEFAULT 1 COMMENT '等级',
    xp                  INT          NOT NULL DEFAULT 0 COMMENT '当前经验值',
    xp_next             INT          NOT NULL DEFAULT 100 COMMENT '升级所需经验值',
    bond                INT          NOT NULL DEFAULT 0 COMMENT '陪伴值(0-100)',
    emotion             TINYINT      NOT NULL DEFAULT 0 COMMENT '情绪值(-100~100)',
    last_interact_ts    BIGINT       NOT NULL DEFAULT 0 COMMENT '最后互动时间戳(秒)',
    last_greet_ts       BIGINT       NOT NULL DEFAULT 0 COMMENT '最后问好时间戳(秒)',
    status              TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0=idle,7=focus',
    unlocked_items      INT          NOT NULL DEFAULT 1 COMMENT '已解锁物品位掩码',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='像素狗状态';

INSERT INTO pixel_dog_state (level, xp, xp_next, bond, emotion, last_interact_ts, last_greet_ts, status, unlocked_items)
SELECT 1, 0, 100, 0, 0, UNIX_TIMESTAMP(), 0, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM pixel_dog_state WHERE deleted = 0);