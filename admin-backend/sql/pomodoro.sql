-- 番茄钟模块表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS pomodoro_plan (
    id                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title                    VARCHAR(128) NOT NULL COMMENT '计划名称',
    work_duration_min        INT          NOT NULL DEFAULT 25 COMMENT '专注时长(分钟)',
    short_break_min          INT          NOT NULL DEFAULT 5 COMMENT '短休息(分钟)',
    long_break_min           INT          NOT NULL DEFAULT 15 COMMENT '长休息(分钟)',
    rounds_before_long_break INT          NOT NULL DEFAULT 4 COMMENT '几轮后长休息',
    daily_goal_rounds        INT          NOT NULL DEFAULT 8 COMMENT '每日目标轮次',
    daily_goal_minutes       INT          NOT NULL DEFAULT 200 COMMENT '每日目标专注分钟',
    is_default               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认计划',
    status                   VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted                  TINYINT      NOT NULL DEFAULT 0,
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='番茄钟计划';

CREATE TABLE IF NOT EXISTS pomodoro_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id       BIGINT       DEFAULT NULL COMMENT '关联计划',
    record_type   VARCHAR(16)  NOT NULL COMMENT 'WORK/SHORT_BREAK/LONG_BREAK',
    duration_sec  INT          NOT NULL COMMENT '实际时长(秒)',
    round_index   INT          NOT NULL DEFAULT 0 COMMENT '当日第几轮专注(仅WORK有效)',
    stat_date     DATE         NOT NULL COMMENT '统计日期',
    source        VARCHAR(16)  NOT NULL DEFAULT 'ADMIN' COMMENT '来源 ADMIN/DEVICE',
    remark        VARCHAR(255) DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stat_date (stat_date),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='番茄钟完成记录';

INSERT INTO pomodoro_plan (title, work_duration_min, short_break_min, long_break_min,
                           rounds_before_long_break, daily_goal_rounds, daily_goal_minutes, is_default, status)
SELECT '默认专注计划', 25, 5, 15, 4, 8, 200, 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM pomodoro_plan WHERE is_default = 1 AND deleted = 0);
