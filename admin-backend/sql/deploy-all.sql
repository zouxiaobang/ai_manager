-- =============================================================================
-- AI Manager 全量数据库初始化（合并脚本）
-- 生成方式：按模块依赖顺序合并 admin-backend/sql/*.sql
-- 适用：全新空库一次性部署（MySQL 8 / MariaDB）
--
-- 未包含（仅旧库升级用）：
--   sys_import_migrate_from_ec_order.sql
--   ecommerce_seed.sql（演示数据已含在 ecommerce_product.sql 等）
--
-- 部署示例：
--   docker exec -i ai-manager-mysql mysql -uroot -p < deploy-all.sql
--   mysql -h 192.168.0.118 -u ai_manager -p < deploy-all.sql
-- =============================================================================


-- ########## FILE: schema.sql ##########

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



-- ########## FILE: pomodoro.sql ##########

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



-- ########## FILE: notebook.sql ##########

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



-- ########## FILE: notebook_storage_migration.sql ##########

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



-- ########## FILE: notebook_todo.sql ##########

-- 笔记本独立待办表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

DROP TABLE IF EXISTS nb_note_todo_item;

CREATE TABLE IF NOT EXISTS nb_todo_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    content         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '待办内容',
    completed       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成 0/1',
    due_time        DATETIME     DEFAULT NULL COMMENT '截止时间',
    remind_time     DATETIME     DEFAULT NULL COMMENT '提醒时间',
    repeat_type     VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
    repeat_interval INT          NOT NULL DEFAULT 1 COMMENT '重复间隔',
    repeat_until    DATETIME     DEFAULT NULL COMMENT '重复截止日期',
    remind_notified TINYINT      NOT NULL DEFAULT 0 COMMENT '提醒是否已推送 0/1',
    series_id       BIGINT       DEFAULT NULL COMMENT '重复系列 ID',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    pinned          TINYINT      NOT NULL DEFAULT 0 COMMENT '特别提醒 0/1',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_completed (completed),
    KEY idx_due_time (due_time),
    KEY idx_remind_time (remind_time),
    KEY idx_series_id (series_id),
    KEY idx_pinned (pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记本待办';



-- ########## FILE: notebook_todo_extend.sql ##########

-- 待办扩展：提醒时间、重复规则（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN remind_time DATETIME DEFAULT NULL COMMENT '提醒时间' AFTER due_time,
    ADD COLUMN repeat_type VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY' AFTER remind_time,
    ADD COLUMN repeat_interval INT NOT NULL DEFAULT 1 COMMENT '重复间隔' AFTER repeat_type,
    ADD COLUMN repeat_until DATETIME DEFAULT NULL COMMENT '重复截止日期' AFTER repeat_interval,
    ADD COLUMN series_id BIGINT DEFAULT NULL COMMENT '重复系列 ID' AFTER repeat_until,
    ADD INDEX idx_remind_time (remind_time),
    ADD INDEX idx_series_id (series_id);



-- ########## FILE: notebook_todo_remind.sql ##########

-- 待办提醒已通知标记（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN remind_notified TINYINT NOT NULL DEFAULT 0 COMMENT '提醒是否已推送 0/1' AFTER repeat_until;



-- ########## FILE: notebook_todo_pinned.sql ##########

-- 待办特别提醒标记（在 ai_manager_admin 库执行，仅需执行一次）
USE ai_manager_admin;

ALTER TABLE nb_todo_item
    ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 COMMENT '特别提醒 0/1' AFTER sort_order,
    ADD INDEX idx_pinned (pinned);



-- ########## FILE: ecommerce_carton.sql ##########

-- 电商纸箱表（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_carton (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '纸箱主键',
    factory_id  BIGINT        DEFAULT NULL COMMENT '所属工厂',
    name        VARCHAR(128)  NOT NULL COMMENT '纸箱名称',
    length_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '长(cm)',
    width_cm    DECIMAL(10, 2) DEFAULT NULL COMMENT '宽(cm)',
    height_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '高(cm)',
    unit_price  DECIMAL(12, 2) DEFAULT NULL COMMENT '单价',
    remark      VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    deleted     TINYINT       NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_carton_factory (factory_id),
    KEY idx_ec_carton_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商纸箱';

-- ========== 演示数据（重复执行前请先清空或跳过） ==========

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 1, 1, '五层瓦楞外箱-中号', 42.00, 32.00, 28.00, 3.50, '适用于马克杯 24 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 1);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 2, 2, 'PP收纳专用箱', 62.00, 42.00, 38.00, 4.20, '小号收纳盒 20 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 2);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 3, 3, '竹纤维餐垫彩箱', 48.00, 34.00, 26.00, 2.80, '四件套餐垫 30 装'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 3);

INSERT INTO ec_carton (id, factory_id, name, length_cm, width_cm, height_cm, unit_price, remark)
SELECT 4, 2, '加厚物流箱-大号', 68.00, 48.00, 42.00, 5.60, '大号收纳盒 12 装，加强耐压'
WHERE NOT EXISTS (SELECT 1 FROM ec_carton WHERE id = 4);



-- ########## FILE: ecommerce_product.sql ##########

-- 电商商品模块（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_factory (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工厂主键',
    name          VARCHAR(128) NOT NULL COMMENT '工厂名称',
    contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(64)  DEFAULT NULL COMMENT '联系方式',
    address       VARCHAR(512) DEFAULT NULL COMMENT '地址',
    remark        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_factory_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商工厂';

CREATE TABLE IF NOT EXISTS ec_product (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SPU 主键',
    factory_id   BIGINT        DEFAULT NULL COMMENT '所属工厂',
    name         VARCHAR(256)  NOT NULL COMMENT '商品名称(SPU)',
    description  TEXT          DEFAULT NULL COMMENT '商品描述',
    rebate_pct   DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，如 5.50 表示 5.5%)',
    image_name   VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    status       VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted      TINYINT       NOT NULL DEFAULT 0,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_product_status (status),
    KEY idx_ec_product_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商商品 SPU';

CREATE TABLE IF NOT EXISTS ec_sku (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU 主键',
    product_id            BIGINT        NOT NULL COMMENT '所属 SPU',
    sku_code              VARCHAR(64)   NOT NULL COMMENT '货号',
    spec_name             VARCHAR(128)  DEFAULT NULL COMMENT '规格名称',
    rebate_pct            DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '退点(百分比，计算以 SKU 为准)',
    image_name            VARCHAR(256)  DEFAULT NULL COMMENT '图片文件名',
    carton_id             BIGINT        DEFAULT NULL COMMENT '匹配纸箱',
    sale_price            DECIMAL(12, 2) DEFAULT NULL COMMENT '销售价',
    product_length_cm     DECIMAL(10, 2) DEFAULT NULL COMMENT '单品长(cm)',
    product_width_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '单品宽(cm)',
    product_height_cm     DECIMAL(10, 2) DEFAULT NULL COMMENT '单品高(cm)',
    carton_length_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱长(cm)',
    carton_width_cm       DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱宽(cm)',
    carton_height_cm      DECIMAL(10, 2) DEFAULT NULL COMMENT '外箱高(cm)',
    carton_gross_weight_kg DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱毛重(kg)',
    carton_net_weight_kg  DECIMAL(10, 3) DEFAULT NULL COMMENT '外箱净重(kg)',
    units_per_carton      INT           NOT NULL DEFAULT 1 COMMENT '外箱装产品数量',
    status                VARCHAR(16)   NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/OFF_SALE/DRAFT',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_sku_code (sku_code),
    KEY idx_ec_sku_product (product_id),
    KEY idx_ec_sku_carton (carton_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商商品 SKU';

-- ========== 演示数据（重复执行前请先清空或跳过） ==========

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 1, '东莞精品日用制造厂', '张经理', '13800138001', '广东省东莞市虎门镇工业路 88 号', '主打陶瓷杯、厨房小件，交期稳定', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 1);

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 2, '义乌源头小商品工厂', '李总', '13900139002', '浙江省义乌市北苑街道春晗路 126 号', '收纳类、塑料家居，支持贴牌', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 2);

INSERT INTO ec_factory (id, name, contact_name, contact_phone, address, remark, status)
SELECT 3, '宁波竹木家居用品厂', '王姐', '13700137003', '浙江省宁波市慈溪市逍林镇竹艺园区 5 栋', '竹纤维、餐垫砧板，出口品质', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_factory WHERE id = 3);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 1, 1, '简约陶瓷马克杯套装', '高温陶瓷，可进洗碗机，礼盒装', 5.50, 'mug-set-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 1);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 2, 2, '多层厨房收纳盒', 'PP 材质，可叠加，厨房/衣柜通用', 8.00, 'storage-box-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 2);

INSERT INTO ec_product (id, factory_id, name, description, rebate_pct, image_name, status)
SELECT 3, 3, '竹纤维餐垫四件套', '天然竹纤维，防滑耐热，四色一组', 6.00, 'bamboo-mat-main.jpg', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_product WHERE id = 3);

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-W-350', '白色 350ml', 5.50, 'mug-white-350.jpg', 1, 29.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.500, 7.800, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-W-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 1, 'MUG-B-350', '黑色 350ml', 6.00, 'mug-black-350.jpg', 1, 31.90, 9.50, 9.50, 12.00, 42.00, 32.00, 28.00, 8.600, 7.900, 24, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MUG-B-350');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-S-3L', '小号 3L', 8.00, 'box-small-3l.jpg', 2, 18.50, 20.00, 15.00, 12.00, 62.00, 42.00, 38.00, 6.200, 5.600, 20, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-S-3L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 2, 'BOX-L-8L', '大号 8L', 8.00, 'box-large-8l.jpg', 2, 32.00, 32.00, 22.00, 18.00, 68.00, 48.00, 42.00, 9.800, 8.900, 12, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'BOX-L-8L');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-GR', '四件套 灰色', 6.00, 'mat-4pc-gray.jpg', 3, 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-GR');

INSERT INTO ec_sku (product_id, sku_code, spec_name, rebate_pct, image_name, carton_id, sale_price,
                    product_length_cm, product_width_cm, product_height_cm,
                    carton_length_cm, carton_width_cm, carton_height_cm,
                    carton_gross_weight_kg, carton_net_weight_kg, units_per_carton, status)
SELECT 3, 'MAT-4PC-BE', '四件套 米色', 6.00, 'mat-4pc-beige.jpg', 3, 45.00, 30.00, 45.00, 0.40, 48.00, 34.00, 26.00, 5.500, 5.000, 30, 'ON_SALE'
WHERE NOT EXISTS (SELECT 1 FROM ec_sku WHERE sku_code = 'MAT-4PC-BE');



-- ########## FILE: ecommerce_factory_alter.sql ##########

-- 已有 ec_product / ec_sku 表时追加工厂表与 factory_id（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_factory (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工厂主键',
    name          VARCHAR(128) NOT NULL COMMENT '工厂名称',
    contact_name  VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(64)  DEFAULT NULL COMMENT '联系方式',
    address       VARCHAR(512) DEFAULT NULL COMMENT '地址',
    remark        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_factory_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商工厂';

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'factory_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_product ADD COLUMN factory_id BIGINT DEFAULT NULL COMMENT ''所属工厂'' AFTER id, ADD KEY idx_ec_product_factory (factory_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 演示数据见 ecommerce_product.sql 末尾 INSERT 语句，或执行：
-- mysql ... < admin-backend/sql/ecommerce_seed.sql



-- ########## FILE: ecommerce_carton_alter.sql ##########

-- 已有电商表时追加 ec_carton（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_carton (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '纸箱主键',
    factory_id  BIGINT        DEFAULT NULL COMMENT '所属工厂',
    name        VARCHAR(128)  NOT NULL COMMENT '纸箱名称',
    length_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '长(cm)',
    width_cm    DECIMAL(10, 2) DEFAULT NULL COMMENT '宽(cm)',
    height_cm   DECIMAL(10, 2) DEFAULT NULL COMMENT '高(cm)',
    unit_price  DECIMAL(12, 2) DEFAULT NULL COMMENT '单价',
    remark      VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    deleted     TINYINT       NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_carton_factory (factory_id),
    KEY idx_ec_carton_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商纸箱';

-- 演示数据见 ecommerce_carton.sql 末尾 INSERT 语句



-- ########## FILE: ecommerce_sku_carton_alter.sql ##########

-- ec_sku 追加 carton_id（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'carton_id'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN carton_id BIGINT DEFAULT NULL COMMENT ''匹配纸箱'' AFTER image_name, ADD KEY idx_ec_sku_carton (carton_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- ########## FILE: ecommerce_sku_rebate_image_alter.sql ##########

-- 已有 ec_product / ec_sku 表时追加 SKU 退点、图片字段，SPU 图片字段（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- ec_product: main_image -> image_name，或新增 image_name
SET @main_image_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'main_image'
);
SET @image_name_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_product' AND COLUMN_NAME = 'image_name'
);
SET @sql = IF(@main_image_exists = 1 AND @image_name_exists = 0,
    'ALTER TABLE ec_product CHANGE COLUMN main_image image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名''',
    IF(@image_name_exists = 0,
        'ALTER TABLE ec_product ADD COLUMN image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名'' AFTER rebate_pct',
        'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'rebate_pct'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN rebate_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT ''退点(百分比，计算以 SKU 为准)'' AFTER spec_name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_sku' AND COLUMN_NAME = 'image_name'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE ec_sku ADD COLUMN image_name VARCHAR(256) DEFAULT NULL COMMENT ''图片文件名'' AFTER rebate_pct',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有 SKU 退点默认继承所属 SPU
UPDATE ec_sku s
INNER JOIN ec_product p ON s.product_id = p.id
SET s.rebate_pct = p.rebate_pct
WHERE s.rebate_pct = 0 OR s.rebate_pct IS NULL;



-- ########## FILE: ecommerce_inventory.sql ##########

-- 电商库存表 / 库存操作记录（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_inventory (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存主键',
    sku_code        VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    quantity        INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
    ignore_alert    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否忽略预警 1是 0否',
    alert_threshold INT          NOT NULL DEFAULT 0 COMMENT '预警数量(库存<=该值且未忽略预警时报警)',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_inventory_sku_code (sku_code),
    KEY idx_ec_inventory_quantity (quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商库存';

CREATE TABLE IF NOT EXISTS ec_inventory_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录主键',
    inventory_id BIGINT      NOT NULL COMMENT '库存表 ID',
    change_type  VARCHAR(16) NOT NULL COMMENT '改动方式 DEDUCT扣除 RECLAIM回收',
    change_qty   INT         NOT NULL COMMENT '改动数量(正数)',
    deleted      TINYINT     NOT NULL DEFAULT 0,
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_inventory_log_inventory (inventory_id),
    KEY idx_ec_inventory_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商库存操作记录';

-- ========== 演示数据 ==========

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'MUG-W-350', 120, 0, 20
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'MUG-W-350');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'MUG-B-350', 8, 0, 10
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'MUG-B-350');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'BOX-S-3L', 200, 0, 30
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'BOX-S-3L');

INSERT INTO ec_inventory (sku_code, quantity, ignore_alert, alert_threshold)
SELECT 'BOX-L-8L', 45, 1, 15
WHERE NOT EXISTS (SELECT 1 FROM ec_inventory WHERE sku_code = 'BOX-L-8L');

INSERT INTO ec_inventory_log (inventory_id, change_type, change_qty)
SELECT i.id, 'RECLAIM', 50
FROM ec_inventory i
WHERE i.sku_code = 'MUG-W-350'
  AND NOT EXISTS (
      SELECT 1 FROM ec_inventory_log l
      WHERE l.inventory_id = i.id AND l.change_type = 'RECLAIM' AND l.change_qty = 50
  )
LIMIT 1;

INSERT INTO ec_inventory_log (inventory_id, change_type, change_qty)
SELECT i.id, 'DEDUCT', 12
FROM ec_inventory i
WHERE i.sku_code = 'MUG-W-350'
  AND NOT EXISTS (
      SELECT 1 FROM ec_inventory_log l
      WHERE l.inventory_id = i.id AND l.change_type = 'DEDUCT' AND l.change_qty = 12
  )
LIMIT 1;



-- ########## FILE: ecommerce_express.sql ##########

-- 电商快递站点 / 价格 / 须知（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_express_station (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '站点主键',
    name        VARCHAR(128) NOT NULL COMMENT '快递名称',
    contact     VARCHAR(256) DEFAULT NULL COMMENT '联系方式',
    address     VARCHAR(512) DEFAULT NULL COMMENT '地址',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_express_station_name (name),
    KEY idx_ec_express_station_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递站点';

CREATE TABLE IF NOT EXISTS ec_express_price (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '价格主键',
    station_id            BIGINT        NOT NULL COMMENT '站点ID',
    province_name         VARCHAR(64)   NOT NULL COMMENT '省份名称',
    price_w03_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '0.3kg价格',
    price_w05_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '0.5kg价格',
    price_w1_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '1kg价格',
    price_w15_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '1.5kg价格',
    price_w2_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '2kg价格',
    price_w25_kg          DECIMAL(12, 2) DEFAULT NULL COMMENT '2.5kg价格',
    price_w3_kg           DECIMAL(12, 2) DEFAULT NULL COMMENT '3kg价格',
    over3_first_price     DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg首重价格',
    over3_additional_price DECIMAL(12, 2) DEFAULT NULL COMMENT '超3kg续重价格',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_express_price_station_province (station_id, province_name),
    KEY idx_ec_express_price_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递价格';

CREATE TABLE IF NOT EXISTS ec_express_notice (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '须知主键',
    station_id    BIGINT       NOT NULL COMMENT '站点ID',
    content       TEXT         NOT NULL COMMENT '须知内容',
    highlight_red TINYINT      NOT NULL DEFAULT 0 COMMENT '是否标红 1是 0否',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_express_notice_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商快递须知';

-- ========== 演示数据 ==========

INSERT INTO ec_express_station (id, name, contact, address, is_default)
SELECT 1, '顺丰标快', '95338', '广东省深圳市南山区顺丰总部营业点', 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_station WHERE id = 1);

INSERT INTO ec_express_station (id, name, contact, address, is_default)
SELECT 2, '中通快递', '95311', '浙江省杭州市余杭区中通转运中心', 0
WHERE NOT EXISTS (SELECT 1 FROM ec_express_station WHERE id = 2);

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 1, '广东省', 12.00, 14.00, 18.00, 22.00, 26.00, 30.00, 34.00, 34.00, 6.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 1 AND province_name = '广东省');

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 1, '北京市', 15.00, 17.00, 22.00, 26.00, 30.00, 34.00, 38.00, 38.00, 8.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 1 AND province_name = '北京市');

INSERT INTO ec_express_price (station_id, province_name, price_w03_kg, price_w05_kg, price_w1_kg, price_w15_kg, price_w2_kg, price_w25_kg, price_w3_kg, over3_first_price, over3_additional_price)
SELECT 2, '广东省', 8.00, 9.00, 11.00, 13.00, 15.00, 17.00, 19.00, 19.00, 4.00
WHERE NOT EXISTS (SELECT 1 FROM ec_express_price WHERE station_id = 2 AND province_name = '广东省');

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 1, '生鲜、易碎品请提前告知客服并加强包装。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 1 AND sort_order = 1);

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 1, '偏远地区可能产生附加费用，以实际揽收为准。', 0, 2
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 1 AND sort_order = 2);

INSERT INTO ec_express_notice (station_id, content, highlight_red, sort_order)
SELECT 2, '中通不支持液体、粉末等违禁品寄递。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ec_express_notice WHERE station_id = 2 AND sort_order = 1);



-- ########## FILE: ecommerce_platform_link.sql ##########

-- 电商平台 / 店铺 / 上架链接 / 链接 SKU（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- ========== 1. 平台表 ==========
-- platform_code 与 Java 枚举 EcPlatformCode 一致；channel_type 区分线上/线下渠道
CREATE TABLE IF NOT EXISTS ec_platform (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '平台主键',
    name          VARCHAR(128) NOT NULL COMMENT '平台名称',
    name_en       VARCHAR(128) DEFAULT NULL COMMENT '平台英文名称',
    platform_code INT          NOT NULL COMMENT '平台标识(枚举 int)',
    channel_type  VARCHAR(16)  NOT NULL DEFAULT 'ONLINE' COMMENT '渠道模式 ONLINE/OFFLINE',
    remark        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_platform_code (platform_code),
    KEY idx_ec_platform_channel (channel_type),
    KEY idx_ec_platform_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商平台';

-- ========== 2. 店铺表 ==========
-- 手续费字段说明（各平台常见口径，店铺级默认费率/金额，可按类目再细化）：
--   category_commission_pct  类目/交易佣金（天猫 2~5%、抖店按类目、1688 B2B 多为 0）
--   tech_service_fee_pct     基础技术服务费/软件服务费（淘系约 0.6%、拼多多约 0.6%）
--   payment_fee_pct          支付/金融服务费（支付宝等约 0.6%）
--   promotion_fee_pct        默认推广/广告扣点估算（全站推广、直通车等预留）
--   fulfillment_fee_pct      履约/代发/平台物流服务费
--   return_service_fee_pct   退货/逆向物流相关费率
--   installment_fee_pct      分期/花呗/信用卡分期手续费
--   annual_platform_fee      平台年费/软件订阅（元/年，如诚信通、部分 SaaS）
--   deposit_amount           店铺保证金（元）
--   shipping_insurance_fee   默认单笔运费险（元）
--   activity_service_fee_pct 大促/活动技术服务费（如百亿补贴通道费）
--   other_fee_pct            其他综合扣点
CREATE TABLE IF NOT EXISTS ec_shop (
    id                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '店铺主键',
    name                     VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    name_en                  VARCHAR(128)  DEFAULT NULL COMMENT '店铺英文名称',
    platform_id              BIGINT        NOT NULL COMMENT '所属平台 ID',
    remark                   VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    category_commission_pct  DECIMAL(5, 2) DEFAULT NULL COMMENT '类目/交易佣金%',
    tech_service_fee_pct     DECIMAL(5, 2) DEFAULT NULL COMMENT '基础技术服务费%',
    payment_fee_pct          DECIMAL(5, 2) DEFAULT NULL COMMENT '支付手续费%',
    promotion_fee_pct        DECIMAL(5, 2) DEFAULT NULL COMMENT '推广/广告默认扣点%',
    fulfillment_fee_pct      DECIMAL(5, 2) DEFAULT NULL COMMENT '履约/代发服务费%',
    return_service_fee_pct     DECIMAL(5, 2) DEFAULT NULL COMMENT '退货/逆向物流服务费率%',
    installment_fee_pct      DECIMAL(5, 2) DEFAULT NULL COMMENT '分期/花呗手续费%',
    activity_service_fee_pct   DECIMAL(5, 2) DEFAULT NULL COMMENT '活动/大促技术服务费%',
    annual_platform_fee      DECIMAL(12, 2) DEFAULT NULL COMMENT '平台年费/软件服务费(元/年)',
    deposit_amount           DECIMAL(12, 2) DEFAULT NULL COMMENT '保证金(元)',
    shipping_insurance_fee   DECIMAL(10, 2) DEFAULT NULL COMMENT '默认单笔运费险(元)',
    other_fee_pct            DECIMAL(5, 2) DEFAULT NULL COMMENT '其他综合扣点%',
    other_fee_remark         VARCHAR(256)  DEFAULT NULL COMMENT '其他费用说明',
    status                   VARCHAR(16)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted                  TINYINT       NOT NULL DEFAULT 0,
    create_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_shop_platform (platform_id),
    KEY idx_ec_shop_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商店铺';

-- ========== 3. 上架链接表 ==========
CREATE TABLE IF NOT EXISTS ec_listing_link (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '链接主键',
    name         VARCHAR(256) NOT NULL COMMENT '链接名称',
    shop_id      BIGINT       NOT NULL COMMENT '所属店铺 ID',
    platform_url VARCHAR(1024) DEFAULT NULL COMMENT '平台商品链接URL',
    listing_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
    remark       VARCHAR(512) DEFAULT NULL COMMENT '备注',
    status       VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_listing_link_shop (shop_id),
    KEY idx_ec_listing_link_time (listing_time),
    KEY idx_ec_listing_link_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商上架链接';

-- ========== 3.1 链接关联商品（多对多） ==========
CREATE TABLE IF NOT EXISTS ec_listing_link_product (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    link_id     BIGINT   NOT NULL COMMENT '上架链接 ID',
    product_id  BIGINT   NOT NULL COMMENT '商品 SPU ID',
    sort_order  INT      NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_listing_link_product (link_id, product_id),
    KEY idx_ec_listing_link_product_link (link_id),
    KEY idx_ec_listing_link_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架链接关联商品';

-- ========== 4. 链接 SKU 信息表 ==========
CREATE TABLE IF NOT EXISTS ec_listing_link_sku (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    link_id        BIGINT        NOT NULL COMMENT '上架链接 ID',
    sku_name       VARCHAR(256)  NOT NULL COMMENT '链接 SKU 展示名称',
    sku_codes      VARCHAR(1024) NOT NULL COMMENT '对应 SKU 货号，多个英文逗号分隔',
    discount_pct   DECIMAL(5, 2) NOT NULL DEFAULT 100.00 COMMENT '折扣折数(90=9折)',
    coupon_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠券金额(元)',
    min_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT '最低设置金额(元)',
    cost_price         DECIMAL(12, 2) DEFAULT NULL COMMENT '成本价格(元，含平台费)',
    base_cost_amount   DECIMAL(12, 2) DEFAULT NULL COMMENT '基础成本=SKU+纸箱+快递',
    platform_fee_amount DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费(盈亏平衡口径)',
    actual_set_amount  DECIMAL(12, 2) DEFAULT NULL COMMENT '真实设置金额(元，可手动填写)',
    profit             DECIMAL(12, 2) DEFAULT NULL COMMENT '利润(元)',
    sort_order         INT           NOT NULL DEFAULT 0 COMMENT '排序',
    deleted        TINYINT       NOT NULL DEFAULT 0,
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_listing_link_sku_link (link_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架链接 SKU 信息';

-- ========== 演示数据 ==========
-- platform_code: 0线下 1=1688 2=淘宝 3=天猫 4=拼多多 5=抖店 6=京东 99=其他

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 1, '1688', '1688', 1, 'ONLINE', '阿里巴巴 1688 批发/采购', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 1);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 2, '淘宝', 'Taobao', 2, 'ONLINE', '淘宝 C 店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 2);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 3, '天猫', 'Tmall', 3, 'ONLINE', '天猫 B 店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 3);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 4, '拼多多', 'Pinduoduo', 4, 'ONLINE', '拼多多 POP 店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 4);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 5, '抖店', 'Douyin Shop', 5, 'ONLINE', '抖音电商/抖店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 5);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 6, '京东', 'JD', 6, 'ONLINE', '京东 POP 店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 6);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 7, '线下门店', 'Offline Store', 0, 'OFFLINE', '直营/加盟线下门店', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 7);

INSERT INTO ec_platform (id, name, name_en, platform_code, channel_type, remark, status)
SELECT 8, '线下批发', 'Offline Wholesale', 0, 'OFFLINE', '档口/展会/线下批发', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_platform WHERE id = 8);

-- 1688：交易技术服务费约 0.6%，支付约 0.6%，B2B 通常无 C 端类目佣金
INSERT INTO ec_shop (id, name, name_en, platform_id, remark,
                     category_commission_pct, tech_service_fee_pct, payment_fee_pct,
                     promotion_fee_pct, annual_platform_fee, deposit_amount, other_fee_remark, status)
SELECT 1, '1688 源头工厂店', '1688 Source Factory', 1, '主供批发补货',
       0.00, 0.60, 0.60, 0.00, 6688.00, 3000.00, '诚信通年费按档，此处为示例', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_shop WHERE id = 1);

-- 淘宝：0.6% 基础软件服务费 + 类目佣金（日用百货示例 2%）+ 支付 0.6%
INSERT INTO ec_shop (id, name, name_en, platform_id, remark,
                     category_commission_pct, tech_service_fee_pct, payment_fee_pct,
                     promotion_fee_pct, shipping_insurance_fee, status)
SELECT 2, '淘宝 C 店-日用家居', 'Taobao Home Store', 2, 'C 店主店',
       2.00, 0.60, 0.60, 5.00, 0.50, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_shop WHERE id = 2);

-- 拼多多：技术服务费约 0.6%，部分类目有额外扣点，活动通道费另计
INSERT INTO ec_shop (id, name, name_en, platform_id, remark,
                     category_commission_pct, tech_service_fee_pct, payment_fee_pct,
                     activity_service_fee_pct, shipping_insurance_fee, status)
SELECT 3, '拼多多旗舰店', 'PDD Flagship', 4, '百亿补贴活动店',
       0.60, 0.60, 0.60, 1.00, 0.30, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_shop WHERE id = 3);

-- 抖店：类目佣金因品类而异（示例 3%），商品卡/部分品类有免佣政策
INSERT INTO ec_shop (id, name, name_en, platform_id, remark,
                     category_commission_pct, tech_service_fee_pct, payment_fee_pct,
                     promotion_fee_pct, deposit_amount, status)
SELECT 4, '抖店官方店', 'Douyin Official', 5, '直播+商品卡',
       3.00, 0.00, 0.60, 8.00, 5000.00, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_shop WHERE id = 4);

-- 线下：无平台扣点，可填人工/租金等综合成本占位
INSERT INTO ec_shop (id, name, name_en, platform_id, remark,
                     other_fee_pct, other_fee_remark, status)
SELECT 5, '东莞展厅直营店', 'Dongguan Showroom', 7, '线下零售',
       0.00, '线下无平台佣金，毛利在商品定价中体现', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_shop WHERE id = 5);

INSERT INTO ec_listing_link (id, name, shop_id, listing_time, remark, status)
SELECT 1, '马克杯双规格链接', 2, '2025-06-01 10:00:00', '淘宝主链接', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM ec_listing_link WHERE id = 1);

INSERT INTO ec_listing_link_product (link_id, product_id, sort_order)
SELECT 1, 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM ec_listing_link WHERE id = 1)
  AND NOT EXISTS (
    SELECT 1 FROM ec_listing_link_product WHERE link_id = 1 AND product_id = 1
  );

INSERT INTO ec_listing_link_sku (link_id, sku_name, sku_codes, discount_pct, coupon_amount, min_set_amount, cost_price, actual_set_amount, profit, sort_order)
SELECT 1, '白色 350ml', 'MUG-W-350', 90.00, 2.00, 54.67, 47.40, 59.90, 4.71, 1
WHERE NOT EXISTS (
    SELECT 1 FROM ec_listing_link_sku WHERE link_id = 1 AND sku_name = '白色 350ml'
);

INSERT INTO ec_listing_link_sku (link_id, sku_name, sku_codes, discount_pct, coupon_amount, min_set_amount, cost_price, actual_set_amount, profit, sort_order)
SELECT 1, '黑色 350ml', 'MUG-B-350', 90.00, 2.00, 56.89, 49.40, 62.90, 4.75, 2
WHERE NOT EXISTS (
    SELECT 1 FROM ec_listing_link_sku WHERE link_id = 1 AND sku_name = '黑色 350ml'
);



-- ########## FILE: ecommerce_listing_link_product.sql ##########

-- 上架链接关联多个商品 SPU（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_listing_link_product (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    link_id     BIGINT   NOT NULL COMMENT '上架链接 ID',
    product_id  BIGINT   NOT NULL COMMENT '商品 SPU ID',
    sort_order  INT      NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_listing_link_product (link_id, product_id),
    KEY idx_ec_listing_link_product_link (link_id),
    KEY idx_ec_listing_link_product_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架链接关联商品';

-- 从 ec_listing_link.product_id 迁移（若列存在）
SET @has_product_id := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND COLUMN_NAME = 'product_id'
);
SET @sql_migrate := IF(
    @has_product_id > 0,
    'INSERT INTO ec_listing_link_product (link_id, product_id, sort_order)
     SELECT l.id, l.product_id, 0
     FROM ec_listing_link l
     WHERE l.product_id IS NOT NULL AND l.deleted = 0
       AND NOT EXISTS (
         SELECT 1 FROM ec_listing_link_product lp
         WHERE lp.link_id = l.id AND lp.product_id = l.product_id AND lp.deleted = 0
       )',
    'SELECT 1'
);
PREPARE stmt FROM @sql_migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 可选：移除旧单列（新环境可跳过）
SET @sql_drop := IF(
    @has_product_id > 0,
    'ALTER TABLE ec_listing_link DROP COLUMN product_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND INDEX_NAME = 'idx_ec_listing_link_product'
);
SET @sql_drop_idx := IF(
    @idx > 0,
    'ALTER TABLE ec_listing_link DROP INDEX idx_ec_listing_link_product',
    'SELECT 1'
);
PREPARE stmt FROM @sql_drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO ec_listing_link_product (link_id, product_id, sort_order)
SELECT 1, 1, 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM ec_listing_link WHERE id = 1)
  AND NOT EXISTS (
    SELECT 1 FROM ec_listing_link_product WHERE link_id = 1 AND product_id = 1
  );



-- ########## FILE: ecommerce_listing_link_enhance.sql ##########

-- 上架链接模块增强（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- 店铺默认收货省（快递试算）
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_shop' AND COLUMN_NAME = 'default_receive_province'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_shop ADD COLUMN default_receive_province VARCHAR(64) DEFAULT ''广东省'' COMMENT ''默认收货省份(快递试算)'' AFTER other_fee_remark',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ec_shop SET default_receive_province = '广东省'
WHERE default_receive_province IS NULL OR default_receive_province = '';

-- 链接：平台 URL、关联 SPU
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND COLUMN_NAME = 'platform_url'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link ADD COLUMN platform_url VARCHAR(1024) DEFAULT NULL COMMENT ''平台商品链接URL'' AFTER shop_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND COLUMN_NAME = 'product_id'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link ADD COLUMN product_id BIGINT DEFAULT NULL COMMENT ''关联商品SPU ID'' AFTER platform_url',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link' AND INDEX_NAME = 'idx_ec_listing_link_product'
);
SET @sql := IF(@idx = 0,
    'ALTER TABLE ec_listing_link ADD KEY idx_ec_listing_link_product (product_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- SKU 行：基础成本、平台费（盈亏平衡口径）
SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link_sku' AND COLUMN_NAME = 'base_cost_amount'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN base_cost_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''基础成本=SKU+纸箱+快递'' AFTER cost_price',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ec_listing_link_sku' AND COLUMN_NAME = 'platform_fee_amount'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN platform_fee_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''平台费(盈亏平衡口径)'' AFTER base_cost_amount',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 演示：马克杯链接关联 SPU id=1
UPDATE ec_listing_link SET product_id = 1 WHERE id = 1 AND product_id IS NULL;

-- 已有 cost_price 需按新公式重算：部署后调用 POST /api/ecommerce/listing-links/recalculate-all
-- 或等待定时任务 EcListingLinkPricingRecalcJob 执行



-- ########## FILE: ecommerce_platform_link_alter.sql ##########

-- 上架链接 SKU：展示金额改名为最低设置金额，并新增成本价格（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- 已有 display_price 列时重命名为 min_set_amount
SET @has_display_price := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'display_price'
);
SET @sql_rename := IF(
    @has_display_price > 0,
    'ALTER TABLE ec_listing_link_sku CHANGE COLUMN display_price min_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''最低设置金额(元)''',
    'SELECT 1'
);
PREPARE stmt FROM @sql_rename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新建表场景直接确保列存在
SET @has_min_set_amount := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'min_set_amount'
);
SET @sql_add_min := IF(
    @has_min_set_amount = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN min_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''最低设置金额(元)'' AFTER coupon_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_min;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_cost_price := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'cost_price'
);
SET @sql_add_cost := IF(
    @has_cost_price = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN cost_price DECIMAL(12, 2) DEFAULT NULL COMMENT ''成本价格(元)=SKU售价+纸箱+快递'' AFTER min_set_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_cost;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 若已执行过上一版 alter，请继续执行：
-- 1. ecommerce_platform_link_data_backfill.sql（回填 cost_price / min_set_amount）
-- 2. ecommerce_platform_link_alter_actual_profit.sql（新增真实设置金额、利润）



-- ########## FILE: ecommerce_platform_link_alter_actual_profit.sql ##########

-- 上架链接 SKU：新增真实设置金额、利润（在 ai_manager_admin 库执行）
USE ai_manager_admin;

SET @has_actual_set_amount := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'actual_set_amount'
);
SET @sql_add_actual := IF(
    @has_actual_set_amount = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN actual_set_amount DECIMAL(12, 2) DEFAULT NULL COMMENT ''真实设置金额(元，可手动填写)'' AFTER cost_price',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_actual;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_profit := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND COLUMN_NAME = 'profit'
);
SET @sql_add_profit := IF(
    @has_profit = 0,
    'ALTER TABLE ec_listing_link_sku ADD COLUMN profit DECIMAL(12, 2) DEFAULT NULL COMMENT ''利润(元)=((真实设置金额-优惠券)×折扣)-成本'' AFTER actual_set_amount',
    'SELECT 1'
);
PREPARE stmt FROM @sql_add_profit;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- ########## FILE: ecommerce_sales_order.sql ##########

-- 销售订单 MVP v2（在 ai_manager_admin 库执行）
--
-- 约定：
--   1. Excel 导入时用「链接名称 + SKU展示名称」在内存中匹配，入库后明细只存 listing_link_sku_id
--      匹配规则：trim(excel.链接名称) = ec_listing_link.name
--               trim(excel.SKU规格名称) = ec_listing_link_sku.sku_name（同 shop_id 内）
--   2. 订单状态以明细为准；订单头 status 由明细聚合（支持部分退款/部分发货）
--   3. 发货扣库存方案 B：扣至 0，欠货写入 ec_sales_order_shortage
--   4. 订单头 express_station_id 关联快递站点，用于后期按收货省试算/核对真实运费
--   5. 已发货退款/退货退款：不回收库存，按行记 loss_amount 亏损
USE ai_manager_admin;

-- ========== 1. 销售订单头 ==========
CREATE TABLE IF NOT EXISTS ec_sales_order (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '销售订单主键',
    order_no              VARCHAR(32)   NOT NULL COMMENT '系统订单号 SOyyyyMMddxxxx',
    shop_id               BIGINT        NOT NULL COMMENT '所属店铺 ID',
    platform_order_no     VARCHAR(64)   DEFAULT NULL COMMENT '平台订单号（导入去重键）',
    source                VARCHAR(16)   NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL/IMPORT/API',
    -- 聚合状态（由明细同步，见文末状态说明）
    status                VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PAID/PARTIAL_SHIPPED/SHIPPED/PARTIAL_REFUND/COMPLETED/CANCELLED/REFUNDED',
    platform_status       VARCHAR(64)   DEFAULT NULL COMMENT '平台原始状态文案',
    express_station_id    BIGINT        DEFAULT NULL COMMENT '快递站点 ID，关联 ec_express_station.id，用于计算真实运费',
    order_time            DATETIME      NOT NULL COMMENT '下单时间',
    pay_time              DATETIME      DEFAULT NULL COMMENT '支付时间',
    ship_time             DATETIME      DEFAULT NULL COMMENT '首条明细发货时间',
    complete_time         DATETIME      DEFAULT NULL COMMENT '订单完成时间',
    buyer_name            VARCHAR(128)  DEFAULT NULL COMMENT '买家昵称/姓名',
    buyer_phone           VARCHAR(32)   DEFAULT NULL COMMENT '买家电话',
    receive_province      VARCHAR(64)   DEFAULT NULL COMMENT '收货省(由 receive_address 自动解析)',
    receive_city          VARCHAR(64)   DEFAULT NULL COMMENT '收货市',
    receive_district      VARCHAR(64)   DEFAULT NULL COMMENT '收货区',
    receive_address       VARCHAR(512)  DEFAULT NULL COMMENT '详细地址',
    tracking_number       VARCHAR(64)   DEFAULT NULL COMMENT '快递单号',
    buyer_remark          VARCHAR(512)  DEFAULT NULL COMMENT '买家留言',
    seller_remark         VARCHAR(512)  DEFAULT NULL COMMENT '卖家备注',
    received_amount       DECIMAL(12, 2) DEFAULT NULL COMMENT '订单实收金额(元)',
    total_cost_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '订单总成本(元，明细汇总快照)',
    freight_amount        DECIMAL(12, 2) DEFAULT NULL COMMENT '买家付运费(元)',
    estimated_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '试算运费(元，按站点+省+重量)',
    actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '真实运费(元，月结快递账单按运单号回填)',
    order_coupon_amount   DECIMAL(12, 2) DEFAULT NULL COMMENT '订单级优惠券(元)',
    platform_fee_amount   DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费合计快照(元)',
    profit_amount         DECIMAL(12, 2) DEFAULT NULL COMMENT '利润合计快照(元，含退款亏损后)',
    total_loss_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '退款亏损合计(元)',
    has_shortage          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否存在欠货 1是 0否',
    import_batch_id       BIGINT        DEFAULT NULL COMMENT '关联导入批次 ID',
    platform_raw_json     JSON          DEFAULT NULL COMMENT '平台/导入原始 JSON 快照',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_sales_order_no (order_no),
    UNIQUE KEY uk_ec_sales_order_platform (shop_id, platform_order_no),
    KEY idx_ec_sales_order_shop (shop_id),
    KEY idx_ec_sales_order_status (status),
    KEY idx_ec_sales_order_order_time (order_time),
    KEY idx_ec_sales_order_express_station (express_station_id),
    KEY idx_ec_sales_order_tracking (tracking_number),
    KEY idx_ec_sales_order_import_batch (import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商销售订单';

-- ========== 2. 销售订单明细 ==========
-- 不在明细表存 Excel 链接名/规格名；导入匹配后只填 listing_link_sku_id，并快照展示名称等供展示与计算。
CREATE TABLE IF NOT EXISTS ec_sales_order_line (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id                BIGINT        NOT NULL COMMENT '销售订单 ID',
    sort_order              INT           NOT NULL DEFAULT 0 COMMENT '排序',
    listing_link_sku_id     BIGINT        DEFAULT NULL COMMENT '上架链接 SKU ID（匹配 ec_listing_link_sku.id）',
    link_name               VARCHAR(256)  DEFAULT NULL COMMENT '链接名称快照 ec_listing_link.name',
    sku_spec_name           VARCHAR(256)  DEFAULT NULL COMMENT 'SKU展示名称快照 ec_listing_link_sku.sku_name',
    sku_codes               VARCHAR(1024) DEFAULT NULL COMMENT '货号快照，逗号分隔',
    sku_quantity            INT           NOT NULL DEFAULT 1 COMMENT 'SKU数量(链接SKU套数)',
    shipped_quantity        INT           NOT NULL DEFAULT 0 COMMENT '已发货套数',
    short_quantity          INT           NOT NULL DEFAULT 0 COMMENT '欠货套数',
    -- 明细状态（退款/发货以行为单位）
    status                  VARCHAR(16)   NOT NULL DEFAULT 'PAID' COMMENT 'PAID/SHIPPED/COMPLETED/CANCELLED/PARTIAL_REFUND/REFUNDED/RETURNED',
    platform_line_status    VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单原始状态',
    refund_type             VARCHAR(16)   DEFAULT NULL COMMENT '退款类型 NONE/REFUND_ONLY已发货退款/RETURN_REFUND退货退款',
    refund_time             DATETIME      DEFAULT NULL COMMENT '退款/退货时间',
    refund_amount           DECIMAL(12, 2) DEFAULT NULL COMMENT '退款金额(元)',
    loss_amount             DECIMAL(12, 2) DEFAULT NULL COMMENT '亏损金额(元)，已发货退款/退货不退库存，按成本记亏',
    unit_price              DECIMAL(12, 2) DEFAULT NULL COMMENT '成交单价(元/套)',
    discount_pct            DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣折数快照(90=9折)',
    line_coupon_amount      DECIMAL(12, 2) DEFAULT NULL COMMENT '行级优惠券(元)',
    line_received_amount    DECIMAL(12, 2) DEFAULT NULL COMMENT '行实收金额(元)',
    sku_amount              DECIMAL(12, 2) DEFAULT NULL COMMENT 'SKU售价合计快照',
    carton_amount           DECIMAL(12, 2) DEFAULT NULL COMMENT '纸箱成本快照',
    express_amount          DECIMAL(12, 2) DEFAULT NULL COMMENT '试算快递成本快照',
    base_cost_amount        DECIMAL(12, 2) DEFAULT NULL COMMENT '基础成本快照',
    platform_fee_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '平台费快照',
    cost_price              DECIMAL(12, 2) DEFAULT NULL COMMENT '行总成本快照(含平台费盈亏平衡口径)',
    min_set_amount          DECIMAL(12, 2) DEFAULT NULL COMMENT '最低设置金额快照',
    profit                  DECIMAL(12, 2) DEFAULT NULL COMMENT '行利润快照',
    pricing_risk            VARCHAR(16)   DEFAULT NULL COMMENT 'OK/BELOW_MIN/NEGATIVE_PROFIT',
    platform_line_no        VARCHAR(64)   DEFAULT NULL COMMENT '平台子订单号',
    platform_item_name      VARCHAR(512)  DEFAULT NULL COMMENT '平台商品标题快照',
    deleted                 TINYINT       NOT NULL DEFAULT 0,
    create_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_line_order (order_id),
    KEY idx_ec_sales_order_line_link_sku (listing_link_sku_id),
    KEY idx_ec_sales_order_line_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商销售订单明细';

-- ========== 3. 发货欠货明细（方案 B） ==========
CREATE TABLE IF NOT EXISTS ec_sales_order_shortage (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '欠货主键',
    order_id            BIGINT       NOT NULL COMMENT '销售订单 ID',
    order_line_id       BIGINT       NOT NULL COMMENT '订单明细 ID',
    sku_code            VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    need_qty            INT          NOT NULL COMMENT '应扣数量',
    deducted_qty        INT          NOT NULL DEFAULT 0 COMMENT '实扣数量',
    short_qty           INT          NOT NULL DEFAULT 0 COMMENT '欠货数量',
    status              VARCHAR(16)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLEARED',
    cleared_qty         INT          NOT NULL DEFAULT 0 COMMENT '已核销数量',
    cleared_ref_type    VARCHAR(32)  DEFAULT NULL COMMENT '核销来源',
    cleared_ref_id      BIGINT       DEFAULT NULL COMMENT '核销业务 ID',
    cleared_time        DATETIME     DEFAULT NULL COMMENT '核销时间',
    remark              VARCHAR(512) DEFAULT NULL COMMENT '备注',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_shortage_order (order_id),
    KEY idx_ec_sales_order_shortage_line (order_line_id),
    KEY idx_ec_sales_order_shortage_sku (sku_code),
    KEY idx_ec_sales_order_shortage_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单发货欠货';

-- ========== 4. 发货扣库存流水 ==========
CREATE TABLE IF NOT EXISTS ec_sales_order_inventory_deduct (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id            BIGINT       NOT NULL COMMENT '销售订单 ID',
    order_line_id       BIGINT       NOT NULL COMMENT '订单明细 ID',
    shortage_id         BIGINT       DEFAULT NULL COMMENT '关联欠货记录 ID',
    sku_code            VARCHAR(64)  NOT NULL COMMENT 'SKU 货号',
    inventory_id        BIGINT       DEFAULT NULL COMMENT 'ec_inventory.id',
    inventory_log_id    BIGINT       DEFAULT NULL COMMENT 'ec_inventory_log.id',
    deduct_qty          INT          NOT NULL COMMENT '本次扣除数量',
    before_qty          INT          DEFAULT NULL COMMENT '扣前库存',
    after_qty           INT          DEFAULT NULL COMMENT '扣后库存',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_sales_order_deduct_order (order_id),
    KEY idx_ec_sales_order_deduct_line (order_line_id),
    KEY idx_ec_sales_order_deduct_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单库存扣减记录';

-- ========== 5. 订单导入配置与批次 ==========
-- 通用表见 admin-backend/sql/sys_import.sql（sys_import_profile / sys_import_batch）
-- 请先执行 sys_import.sql；若已有 ec_order_import_* 请再执行 sys_import_migrate_from_ec_order.sql

-- ========== 6. 导入原始行（Excel 链接名/规格名只存这里，用于预览与排错） ==========
CREATE TABLE IF NOT EXISTS ec_order_import_row (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_id            BIGINT       NOT NULL COMMENT '批次 ID',
    row_no              INT          NOT NULL COMMENT 'Excel 行号',
    parse_status        VARCHAR(16)  NOT NULL DEFAULT 'OK' COMMENT 'OK/ERROR/SKIP',
    platform_order_no   VARCHAR(64)  DEFAULT NULL COMMENT '平台订单号',
    link_name           VARCHAR(256) DEFAULT NULL COMMENT 'Excel 链接名称',
    sku_spec_name       VARCHAR(256) DEFAULT NULL COMMENT 'Excel SKU规格/展示名称',
    match_status        VARCHAR(16)  DEFAULT NULL COMMENT 'MATCHED/UNMATCHED',
    listing_link_sku_id BIGINT       DEFAULT NULL COMMENT '匹配到的 ec_listing_link_sku.id',
    manual_cost_price   DECIMAL(12, 2) DEFAULT NULL COMMENT '手动成本(元/套，未匹配链接时使用)',
    platform_line_status VARCHAR(64)  DEFAULT NULL COMMENT 'Excel 平台子订单/退款状态原文',
    line_status         VARCHAR(16)  DEFAULT NULL COMMENT '解析或人工指定的系统行状态',
    status_match_status VARCHAR(16)  DEFAULT NULL COMMENT '状态映射 MATCHED/UNMATCHED',
    sales_order_id      BIGINT       DEFAULT NULL COMMENT '入库订单 ID',
    sales_order_line_id BIGINT       DEFAULT NULL COMMENT '入库明细 ID',
    error_message       VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    raw_json            JSON         DEFAULT NULL COMMENT '原始行 JSON',
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_order_import_row_batch (batch_id),
    KEY idx_ec_order_import_row_platform_no (platform_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单导入原始行';

-- ========== 8. 状态与退款规则（实现于 Service） ==========
-- 明细 status: PAID -> SHIPPED -> COMPLETED | CANCELLED | PARTIAL_REFUND | REFUNDED | RETURNED
--
-- 退款规则（不退库存，直接记亏）：
--   REFUND_ONLY  已发货仅退款：status=REFUNDED, refund_type=REFUND_ONLY
--                不 RECLAIM 库存；loss_amount = 行成本(cost_price*sku_quantity) + 分摊真实运费 - 退款后留存收入(0或部分)
--   RETURN_REFUND 退货退款：status=RETURNED, refund_type=RETURN_REFUND
--                同样不 RECLAIM 库存；货品视为损耗，loss_amount 同上口径
--   未发货取消：status=CANCELLED，无 loss_amount，未扣库存则无需处理
--
-- 行 profit 退款后重算：profit = line_received_amount - cost_price*sku_quantity - 分摊费用 - loss_adjustment
-- 或退款完成后 profit 置 0，loss_amount 记录净亏损
--
-- 头 status 聚合：
--   全部 PAID                     -> PAID
--   存在 SHIPPED 且非全部完成     -> PARTIAL_SHIPPED
--   全部 SHIPPED/COMPLETED        -> SHIPPED/COMPLETED
--   存在 REFUNDED/RETURNED 且混合 -> PARTIAL_REFUND
--   全部 REFUNDED/RETURNED/CANCELLED -> REFUNDED/CANCELLED
--
-- 4. express_station_id + 收货省(由地址解析，空则用店铺默认省) + 明细重量 → estimated_freight_amount
--    actual_freight_amount 由月结快递账单上传后按 tracking_number 回填，订单 Excel 不导入

-- ========== 9. 内置导入模板 ==========
-- 见 sys_import.sql 中 SALES_ORDER 默认 profile

-- ========== 10. 链接匹配辅助索引 ==========
SET @idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link'
      AND INDEX_NAME = 'idx_ec_listing_link_shop_name'
);
SET @sql := IF(
    @idx = 0,
    'ALTER TABLE ec_listing_link ADD KEY idx_ec_listing_link_shop_name (shop_id, name(128))',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx2 := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_listing_link_sku'
      AND INDEX_NAME = 'idx_ec_listing_link_sku_link_name'
);
SET @sql2 := IF(
    @idx2 = 0,
    'ALTER TABLE ec_listing_link_sku ADD KEY idx_ec_listing_link_sku_link_name (link_id, sku_name(128))',
    'SELECT 1'
);
PREPARE stmt FROM @sql2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- ########## FILE: ec_sales_order_seller_remark.sql ##########

-- 订单头卖家备注（若库中尚无该列则执行）
USE ai_manager_admin;

ALTER TABLE ec_sales_order
    ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(512) DEFAULT NULL COMMENT '买家留言' AFTER tracking_number;

ALTER TABLE ec_sales_order
    ADD COLUMN IF NOT EXISTS seller_remark VARCHAR(512) DEFAULT NULL COMMENT '卖家备注' AFTER buyer_remark;



-- ########## FILE: ecommerce_sales_order_freight.sql ##########

-- 销售订单：试算运费字段（真实运费由月结快递账单按运单号回填 actual_freight_amount）
USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_sales_order'
      AND COLUMN_NAME = 'estimated_freight_amount'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ec_sales_order
        ADD COLUMN estimated_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''试算运费(元，按站点+省+重量)'' AFTER freight_amount,
        MODIFY COLUMN actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''真实运费(元，月结快递账单按运单号回填)''',
    'ALTER TABLE ec_sales_order
        MODIFY COLUMN actual_freight_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00
            COMMENT ''真实运费(元，月结快递账单按运单号回填)''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- ########## FILE: ecommerce_sales_order_import_manual_cost.sql ##########

-- 导入行：未匹配链接时可填手动成本（元/套）
USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ec_order_import_row'
      AND COLUMN_NAME = 'manual_cost_price'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ec_order_import_row
        ADD COLUMN manual_cost_price DECIMAL(12, 2) DEFAULT NULL
            COMMENT ''手动成本(元/套，未匹配链接时使用)'' AFTER listing_link_sku_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;



-- ########## FILE: ec_order_import_row_status.sql ##########

-- 导入预览行：平台状态与系统行状态（支持状态未映射时人工指定）
USE ai_manager_admin;

ALTER TABLE ec_order_import_row
    ADD COLUMN platform_line_status VARCHAR(64) DEFAULT NULL COMMENT 'Excel 平台子订单/退款状态原文' AFTER manual_cost_price,
    ADD COLUMN line_status VARCHAR(16) DEFAULT NULL COMMENT '解析或人工指定的系统行状态' AFTER platform_line_status,
    ADD COLUMN status_match_status VARCHAR(16) DEFAULT NULL COMMENT '状态映射 MATCHED/UNMATCHED' AFTER line_status;



-- ########## FILE: ecommerce_inbound.sql ##########

-- 进货单 + 库存日志扩展（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_inbound_order (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '进货单主键',
    order_no    VARCHAR(32)  NOT NULL COMMENT '进货单号',
    factory_id  BIGINT       DEFAULT NULL COMMENT '所属工厂',
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    remark      VARCHAR(512) DEFAULT NULL COMMENT '备注',
    order_time  DATETIME     DEFAULT NULL COMMENT '下单时间',
    expected_delivery_time DATETIME DEFAULT NULL COMMENT '预收货时间',
    actual_receipt_time DATETIME DEFAULT NULL COMMENT '实际收货时间',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_inbound_order_no (order_no),
    KEY idx_ec_inbound_order_status (status),
    KEY idx_ec_inbound_order_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商进货单';

CREATE TABLE IF NOT EXISTS ec_inbound_order_line (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id    BIGINT      NOT NULL COMMENT '进货单 ID',
    sku_code    VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    quantity    INT         NOT NULL COMMENT '下单数量',
    received_quantity INT   DEFAULT NULL COMMENT '实际收货数量',
    deleted     TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_inbound_order_line_order (order_id),
    KEY idx_ec_inbound_order_line_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商进货单明细';

-- 库存操作记录扩展（关联进货单等，重复执行若列已存在会报错可忽略）
ALTER TABLE ec_inventory_log
    ADD COLUMN ref_type VARCHAR(32) DEFAULT NULL COMMENT '关联类型 INBOUND_ORDER 等' AFTER change_qty,
    ADD COLUMN ref_id BIGINT DEFAULT NULL COMMENT '关联业务 ID' AFTER ref_type,
    ADD COLUMN remark VARCHAR(512) DEFAULT NULL COMMENT '备注' AFTER ref_id;



-- ########## FILE: ecommerce_inbound_v2.sql ##########

-- 进货单时间字段 + 明细实收数量（在 ai_manager_admin 库执行，列已存在可忽略报错）
USE ai_manager_admin;

ALTER TABLE ec_inbound_order
    ADD COLUMN order_time DATETIME DEFAULT NULL COMMENT '下单时间' AFTER remark,
    ADD COLUMN expected_delivery_time DATETIME DEFAULT NULL COMMENT '预收货时间' AFTER order_time,
    ADD COLUMN actual_receipt_time DATETIME DEFAULT NULL COMMENT '实际收货时间' AFTER expected_delivery_time;

ALTER TABLE ec_inbound_order_line
    ADD COLUMN received_quantity INT DEFAULT NULL COMMENT '实际收货数量' AFTER quantity;



-- ########## FILE: ecommerce_stocktake.sql ##########

-- 盘点单（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_stocktake_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '盘点单主键',
    order_no        VARCHAR(32)  NOT NULL COMMENT '盘点单号',
    factory_id      BIGINT       DEFAULT NULL COMMENT '所属工厂',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    remark          VARCHAR(512) DEFAULT NULL COMMENT '备注',
    stocktake_time  DATETIME     DEFAULT NULL COMMENT '盘点时间',
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ec_stocktake_order_no (order_no),
    KEY idx_ec_stocktake_order_status (status),
    KEY idx_ec_stocktake_order_factory (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商盘点单';

CREATE TABLE IF NOT EXISTS ec_stocktake_order_line (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    order_id         BIGINT      NOT NULL COMMENT '盘点单 ID',
    sku_code         VARCHAR(64) NOT NULL COMMENT 'SKU 货号',
    book_quantity    INT         NOT NULL COMMENT '账面数量(保存时快照)',
    actual_quantity  INT         DEFAULT NULL COMMENT '实盘数量',
    deleted          TINYINT     NOT NULL DEFAULT 0,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_stocktake_order_line_order (order_id),
    KEY idx_ec_stocktake_order_line_sku (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商盘点单明细';



-- ########## FILE: ecommerce_monthly_settlement.sql ##########

-- 月结统计：买家排除配置、订单纳入决策、快递账单导入记录
USE ai_manager_admin;

-- 统计时排除的买家昵称（可按店铺，shop_id 为空表示全平台生效）
CREATE TABLE IF NOT EXISTS ec_settlement_buyer_exclude (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id     BIGINT       DEFAULT NULL COMMENT '店铺 ID，空=全部店铺',
    buyer_name  VARCHAR(128) NOT NULL COMMENT '买家昵称（精确匹配 trim）',
    remark      VARCHAR(256) DEFAULT NULL COMMENT '备注',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_settlement_buyer_exclude_shop (shop_id),
    KEY idx_settlement_buyer_exclude_name (buyer_name(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结统计-买家排除';

-- 待确认订单的人工纳入决策（按自然月 YYYY-MM）
CREATE TABLE IF NOT EXISTS ec_settlement_order_decision (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id          BIGINT      NOT NULL COMMENT '店铺 ID',
    order_id         BIGINT      NOT NULL COMMENT '销售订单 ID',
    settlement_month CHAR(7)     NOT NULL COMMENT '统计月份 YYYY-MM',
    included         TINYINT     NOT NULL DEFAULT 0 COMMENT '1纳入 0不纳入',
    deleted          TINYINT     NOT NULL DEFAULT 0,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_order_month (order_id, settlement_month),
    KEY idx_settlement_decision_shop_month (shop_id, settlement_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结统计-订单纳入决策';

-- 月结快递账单导入批次（按运单号回填 actual_freight_amount）
CREATE TABLE IF NOT EXISTS ec_settlement_express_bill (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_month    CHAR(7)       NOT NULL COMMENT '账单月份 YYYY-MM',
    file_name     VARCHAR(256)  DEFAULT NULL COMMENT '上传文件名',
    total_rows    INT           NOT NULL DEFAULT 0,
    matched_rows  INT           NOT NULL DEFAULT 0,
    unmatched_rows INT          NOT NULL DEFAULT 0,
    status        VARCHAR(16)   NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED/FAILED',
    error_message VARCHAR(512)  DEFAULT NULL,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_settlement_express_bill_month (bill_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结快递账单导入批次';



-- ########## FILE: ec_settlement_express_bill_enhance.sql ##########

-- 月结快递账单：明细入库、快递公司、列映射配置
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN express_station_id BIGINT       DEFAULT NULL COMMENT '快递站点 ID' AFTER bill_month,
    ADD COLUMN column_mapping   TEXT           DEFAULT NULL COMMENT '列映射 JSON 快照',
    ADD COLUMN header_row       INT            NOT NULL DEFAULT 1 COMMENT '表头行号(1-based)',
    ADD COLUMN data_start_row   INT            NOT NULL DEFAULT 2 COMMENT '数据起始行(1-based)',
    ADD COLUMN import_mode      VARCHAR(16)    NOT NULL DEFAULT 'FILE' COMMENT 'FILE/MANUAL/MIXED',
    ADD COLUMN gap_order_rows   INT            NOT NULL DEFAULT 0 COMMENT '未匹配发货/完成订单数',
    ADD COLUMN manual_applied_rows INT         NOT NULL DEFAULT 0 COMMENT '手动补录并应用条数',
    ADD KEY idx_settlement_express_bill_station (express_station_id);

CREATE TABLE IF NOT EXISTS ec_settlement_express_bill_line (
    id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    bill_id            BIGINT         NOT NULL COMMENT '批次 ID',
    source             VARCHAR(16)    NOT NULL COMMENT 'FILE/GAP_ORDER/MANUAL',
    order_id           BIGINT         DEFAULT NULL COMMENT '匹配订单 ID',
    platform_order_no  VARCHAR(64)    DEFAULT NULL COMMENT '平台订单号',
    order_no           VARCHAR(64)    DEFAULT NULL COMMENT '系统订单号',
    tracking_number    VARCHAR(128)   DEFAULT NULL COMMENT '运单号',
    freight_amount     DECIMAL(12, 2) DEFAULT NULL COMMENT '运费',
    match_status       VARCHAR(16)    NOT NULL DEFAULT 'PENDING' COMMENT 'MATCHED/UNMATCHED/PENDING/APPLIED',
    remark             VARCHAR(256)   DEFAULT NULL COMMENT '备注',
    deleted            TINYINT        NOT NULL DEFAULT 0,
    create_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_express_bill_line_bill (bill_id),
    KEY idx_express_bill_line_order (order_id),
    KEY idx_express_bill_line_tracking (tracking_number(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月结快递账单明细';

-- 列映射配置统一使用 sys_import_profile（biz_type=SETTLEMENT_EXPRESS_BILL, scope_key=express_station:{id}）
-- 若曾创建 ec_settlement_express_bill_profile，请执行 ec_settlement_express_bill_profile_to_sys_import.sql



-- ########## FILE: ec_settlement_express_bill_line_extend.sql ##########

-- 快递账单明细：结算目的地、重量、发货时间
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill_line
    ADD COLUMN settlement_destination VARCHAR(128) DEFAULT NULL COMMENT '结算目的地' AFTER freight_amount,
    ADD COLUMN weight               DECIMAL(10, 3) DEFAULT NULL COMMENT '重量(kg)' AFTER settlement_destination,
    ADD COLUMN ship_time            DATETIME       DEFAULT NULL COMMENT '发货时间' AFTER weight;



-- ########## FILE: ec_settlement_express_bill_station_filter.sql ##########

-- 快递账单：明细快递公司、其他快递公司标记
USE ai_manager_admin;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN other_express TINYINT NOT NULL DEFAULT 0 COMMENT '1=其他快递公司（未匹配系统站点）' AFTER express_station_id;

ALTER TABLE ec_settlement_express_bill_line
    ADD COLUMN express_station_id BIGINT DEFAULT NULL COMMENT '快递公司 ID' AFTER bill_id,
    ADD KEY idx_express_bill_line_station (express_station_id);



-- ########## FILE: ec_express_station_label_price.sql ##########

-- 快递站点：面单价格；账单批次：是否叠加面单价格
USE ai_manager_admin;

ALTER TABLE ec_express_station
    ADD COLUMN label_price DECIMAL(10, 2) DEFAULT NULL COMMENT '面单价格(元/单)' AFTER address;

ALTER TABLE ec_settlement_express_bill
    ADD COLUMN include_label_price TINYINT NOT NULL DEFAULT 0 COMMENT '1=运费叠加面单价格' AFTER express_station_id;



-- ########## FILE: ec_express_station_name_alias.sql ##########

-- 快递站点导入名称别名（存储于 sys_import_profile，无需改表）
-- biz_type = 'EXPRESS_STATION_NAME'
-- scope_key = 'express_station:{站点ID}'
-- value_mapping = { "平台别名": "站点ID", ... }
-- 在「快递管理 → 站点基本信息」中配置；订单导入解析 express_station_name 时会先精确匹配站点名称，再匹配别名。



-- ########## FILE: sys_import.sql ##########

-- ============================================================
-- 通用文档导入：配置 + 批次（多业务复用，biz_type 区分）
-- 执行库：ai_manager_admin
-- 订单等业务原始行仍用业务表（如 ec_order_import_row）
-- ============================================================

USE ai_manager_admin;

-- ========== 1. 导入列映射配置 ==========
CREATE TABLE IF NOT EXISTS sys_import_profile (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键',
    name                 VARCHAR(128) NOT NULL COMMENT '配置名称',
    biz_type             VARCHAR(32)  NOT NULL COMMENT '业务类型 SALES_ORDER/...',
    platform_id          BIGINT       DEFAULT NULL COMMENT '绑定平台 ec_platform.id（订单导入按平台）',
    scope_key            VARCHAR(64)  DEFAULT NULL COMMENT '作用域键，如 platform:2',
    shop_id              BIGINT       DEFAULT NULL COMMENT '绑定店铺（可选，一般不用）',
    file_type            VARCHAR(16)  NOT NULL DEFAULT 'XLSX' COMMENT 'XLSX/XLS/CSV',
    header_row           INT          NOT NULL DEFAULT 1 COMMENT '表头行号（1-based）',
    data_start_row       INT          NOT NULL DEFAULT 2 COMMENT '数据起始行号（1-based）',
    sheet_name           VARCHAR(64)  DEFAULT NULL COMMENT '工作表名，空则首个 sheet',
    column_mapping       JSON         NOT NULL COMMENT '后端字段 -> 文档列名',
    value_mapping        JSON         DEFAULT NULL COMMENT '值映射，如平台状态 -> 系统状态',
    extra_config         JSON         DEFAULT NULL COMMENT '扩展配置 JSON',
    enabled              TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    remark               VARCHAR(512) DEFAULT NULL COMMENT '备注',
    deleted              TINYINT      NOT NULL DEFAULT 0,
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_import_profile_biz (biz_type),
    KEY idx_sys_import_profile_platform (platform_id),
    KEY idx_sys_import_profile_shop (shop_id),
    KEY idx_sys_import_profile_scope (biz_type, scope_key),
    UNIQUE KEY uk_sys_import_profile_biz_platform_name (biz_type, platform_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文档导入列映射配置';

-- column_mapping 示例（SALES_ORDER）：
--   link_name -> 链接名称
--   sku_spec_name -> SKU规格名称
--   sku_quantity -> SKU数量
--   express_station_name -> 快递站点
--   platform_order_no, order_time, received_amount, tracking_number ...

-- ========== 2. 导入批次 ==========
CREATE TABLE IF NOT EXISTS sys_import_batch (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次主键',
    batch_no            VARCHAR(32)  NOT NULL COMMENT '批次号',
    profile_id          BIGINT       DEFAULT NULL COMMENT '使用的导入配置',
    biz_type            VARCHAR(32)  NOT NULL COMMENT '业务类型',
    biz_context         JSON         DEFAULT NULL COMMENT '业务上下文，如 {"shopId":1}',
    file_name           VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    file_path           VARCHAR(512) DEFAULT NULL COMMENT '存储路径',
    detected_columns    JSON         DEFAULT NULL COMMENT '上传时检测到的列名',
    source              VARCHAR(16)  NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/SCHEDULED',
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREVIEWED/COMMITTED/FAILED',
    total_rows          INT          NOT NULL DEFAULT 0,
    success_rows        INT          NOT NULL DEFAULT 0,
    failed_rows         INT          NOT NULL DEFAULT 0,
    unmatched_rows      INT          NOT NULL DEFAULT 0 COMMENT '业务层未匹配行数',
    error_summary       VARCHAR(1024) DEFAULT NULL,
    operator            VARCHAR(64)  DEFAULT NULL,
    committed_time      DATETIME     DEFAULT NULL,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_import_batch_no (batch_no),
    KEY idx_sys_import_batch_biz (biz_type),
    KEY idx_sys_import_batch_profile (profile_id),
    KEY idx_sys_import_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用文档导入批次';

-- ========== 3. 销售订单导入内置模板（按平台） ==========
INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '1688excel模版', 'SALES_ORDER', 1, 'platform:1', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单号',
           'order_time', '下单时间',
           'express_station_name', '物流公司',
           'received_amount', '实付款',
           'tracking_number', '运单号',
           'buyer_name', '买家',
           'receive_address', '收货地址',
           'link_name', '货品标题',
           'sku_spec_name', '规格',
           'sku_quantity', '数量',
           'platform_line_status', '退款状态',
           'platform_status', '订单状态'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退款成功', 'REFUNDED', '部分退款', 'PARTIAL_REFUND', '退款中', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '1688 平台订单导出默认列映射，可按实际导出列名调整'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 1 AND name = '1688excel模版');

INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '淘宝excel模版', 'SALES_ORDER', 2, 'platform:2', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单编号',
           'order_time', '买家下单时间',
           'pay_time', '买家付款时间',
           'ship_time', '发货时间',
           'complete_time', '确认收货时间',
           'express_station_name', '物流公司',
           'received_amount', '买家实付金额',
           'tracking_number', '运单号',
           'buyer_name', '买家会员名',
           'buyer_phone', '联系手机',
           'receive_address', '收货地址',
           'link_name', '宝贝标题',
           'sku_spec_name', '宝贝规格',
           'sku_quantity', '宝贝总数量',
           'platform_line_status', '订单状态',
           'platform_status', '订单状态'
       ),
       JSON_OBJECT(
           '交易成功', 'COMPLETED',
           '交易关闭', 'CANCELLED',
           '卖家已发货', 'SHIPPED',
           '等待买家确认', 'SHIPPED',
           '买家已付款', 'PAID',
           '等待卖家发货', 'PAID',
           '待发货', 'PAID',
           '已发货', 'SHIPPED',
           '已完成', 'COMPLETED',
           '已退款', 'REFUNDED',
           '退货退款', 'RETURNED',
           '已取消', 'CANCELLED'
       ),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '淘宝平台订单导出默认列映射，可按实际导出列名调整'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 2 AND name = '淘宝excel模版');



-- ########## FILE: sys_import_alter_add_platform.sql ##########

-- ============================================================
-- sys_import_profile 增加 platform_id（已有 sys_import 表时执行）
-- ============================================================

USE ai_manager_admin;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_import_profile'
      AND COLUMN_NAME = 'platform_id'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_import_profile
        ADD COLUMN platform_id BIGINT DEFAULT NULL COMMENT ''绑定平台 ec_platform.id'' AFTER biz_type,
        ADD KEY idx_sys_import_profile_platform (platform_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 从 extra_config.platformCode 回填 platform_id
UPDATE sys_import_profile sp
INNER JOIN ec_platform ep ON ep.platform_code = CAST(JSON_UNQUOTE(JSON_EXTRACT(sp.extra_config, '$.platformCode')) AS UNSIGNED)
SET sp.platform_id = ep.id,
    sp.scope_key = CONCAT('platform:', ep.id)
WHERE sp.platform_id IS NULL
  AND JSON_EXTRACT(sp.extra_config, '$.platformCode') IS NOT NULL;

-- 删除旧的全局模板，插入按平台模板（若不存在）
DELETE FROM sys_import_profile
WHERE biz_type = 'SALES_ORDER' AND platform_id IS NULL AND name = '系统标准模板';

INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '1688excel模版', 'SALES_ORDER', 1, 'platform:1', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单号',
           'order_time', '下单时间',
           'express_station_name', '物流公司',
           'received_amount', '实付款',
           'tracking_number', '运单号',
           'buyer_name', '买家',
           'receive_address', '收货地址',
           'link_name', '货品标题',
           'sku_spec_name', '规格',
           'sku_quantity', '数量',
           'line_received_amount', '单品金额'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '1688 平台订单导出默认列映射'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 1 AND name = '1688excel模版');

INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, value_mapping, extra_config, remark)
SELECT '淘宝excel模版', 'SALES_ORDER', 2, 'platform:2', 'XLSX', 1, 2,
       JSON_OBJECT(
           'platform_order_no', '订单编号',
           'order_time', '买家下单时间',
           'pay_time', '买家付款时间',
           'express_station_name', '物流公司',
           'received_amount', '买家实付金额',
           'tracking_number', '运单号',
           'buyer_name', '买家会员名',
           'buyer_phone', '联系手机',
           'receive_address', '收货地址',
           'link_name', '宝贝标题',
           'sku_spec_name', '宝贝规格',
           'sku_quantity', '宝贝总数量',
           'buyer_goods_amount', '买家应付货款',
           'line_received_amount', '买家应付货款',
           'received_amount', '买家实付金额',
           'platform_line_status', '订单状态'
       ),
       JSON_OBJECT('待发货', 'PAID', '已发货', 'SHIPPED', '已完成', 'COMPLETED', '已退款', 'REFUNDED', '退货退款', 'RETURNED', '已取消', 'CANCELLED'),
       JSON_OBJECT('defaultLineStatus', 'PAID'),
       '淘宝平台订单导出默认列映射'
WHERE NOT EXISTS (SELECT 1 FROM sys_import_profile WHERE biz_type = 'SALES_ORDER' AND platform_id = 2 AND name = '淘宝excel模版');



-- ########## FILE: sys_import_alter_taobao_status_mapping.sql ##########

-- 更新淘宝导入模板的订单状态 value_mapping（支持「交易成功」「交易关闭」等淘宝导出文案）
UPDATE sys_import_profile
SET value_mapping = JSON_OBJECT(
        '交易成功', 'COMPLETED',
        '交易关闭', 'CANCELLED',
        '卖家已发货', 'SHIPPED',
        '等待买家确认', 'SHIPPED',
        '买家已付款', 'PAID',
        '等待卖家发货', 'PAID',
        '待发货', 'PAID',
        '已发货', 'SHIPPED',
        '已完成', 'COMPLETED',
        '已退款', 'REFUNDED',
        '退货退款', 'RETURNED',
        '已取消', 'CANCELLED'
    ),
    update_time = NOW()
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 2
  AND name = '淘宝excel模版';



-- ########## FILE: sys_import_alter_taobao_datetime_status.sql ##########

-- 淘宝导入模板：补充发货/完成时间与平台状态列映射
UPDATE sys_import_profile
SET column_mapping = JSON_MERGE_PATCH(
        COALESCE(column_mapping, JSON_OBJECT()),
        JSON_OBJECT(
            'ship_time', '发货时间',
            'complete_time', '确认收货时间',
            'platform_status', '订单状态'
        )
    ),
    update_time = NOW()
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 2
  AND name = '淘宝excel模版';



-- ########## FILE: sys_import_alter_partial_refund_status.sql ##########

-- 状态映射：平台「部分退款」映射为 PARTIAL_REFUND（非 REFUNDED）
USE ai_manager_admin;

UPDATE sys_import_profile
SET value_mapping = JSON_SET(
        COALESCE(value_mapping, JSON_OBJECT()),
        '$.部分退款', 'PARTIAL_REFUND'
    )
WHERE biz_type = 'SALES_ORDER'
  AND JSON_UNQUOTE(JSON_EXTRACT(value_mapping, '$.部分退款')) = 'REFUNDED';



-- ########## FILE: sys_import_alter_1688_refund_status.sql ##########

-- 1688 导入：补充退款状态列映射与状态文案映射（支持订单部分退款聚合为 PARTIAL_REFUND）
-- 执行前请备份 sys_import_profile

UPDATE sys_import_profile
SET column_mapping = JSON_SET(
        column_mapping,
        '$.platform_line_status', '退款状态',
        '$.platform_status', '订单状态'
    ),
    value_mapping = JSON_MERGE_PATCH(
        COALESCE(value_mapping, JSON_OBJECT()),
        JSON_OBJECT(
            '退款成功', 'REFUNDED',
            '部分退款', 'PARTIAL_REFUND',
            '退款中', 'REFUNDED'
        )
    )
WHERE biz_type = 'SALES_ORDER'
  AND platform_id = 1
  AND name = '1688excel模版';



-- ########## FILE: sys_import_alter_remove_line_received.sql ##########

-- 移除销售订单导入配置中不再提供的列映射项
-- （手动成本、行实收、支付详情、买家应付货款 — 改由系统逻辑或入库界面处理）
-- 执行前请备份 sys_import_profile

UPDATE sys_import_profile
SET column_mapping = JSON_REMOVE(
        JSON_REMOVE(
                JSON_REMOVE(
                        JSON_REMOVE(column_mapping, '$.line_received_amount'),
                        '$.buyer_goods_amount'),
                '$.pay_detail'),
        '$.manual_cost_price')
WHERE biz_type = 'SALES_ORDER'
  AND (
        JSON_CONTAINS_PATH(column_mapping, 'one', '$.line_received_amount')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.buyer_goods_amount')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.pay_detail')
        OR JSON_CONTAINS_PATH(column_mapping, 'one', '$.manual_cost_price')
    );



-- ########## FILE: ec_settlement_express_bill_profile_to_sys_import.sql ##########

-- 快递账单列映射并入 sys_import_profile，移除独立配置表
USE ai_manager_admin;

-- 迁移已有 ec_settlement_express_bill_profile 到 sys_import_profile（若表存在）
INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, enabled, remark)
SELECT CONCAT('快递账单-', COALESCE(s.name, p.express_station_id)), 'SETTLEMENT_EXPRESS_BILL', NULL,
       CONCAT('express_station:', p.express_station_id), 'XLSX', p.header_row, p.data_start_row, p.column_mapping, 1,
       '从 ec_settlement_express_bill_profile 迁移'
FROM ec_settlement_express_bill_profile p
         LEFT JOIN ec_express_station s ON s.id = p.express_station_id
WHERE p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_import_profile sip
    WHERE sip.biz_type = 'SETTLEMENT_EXPRESS_BILL'
      AND sip.scope_key = CONCAT('express_station:', p.express_station_id)
      AND sip.deleted = 0
);

DROP TABLE IF EXISTS ec_settlement_express_bill_profile;



-- ########## FILE: ecommerce_sku_carton_backfill.sql ##########

-- 为已有 SKU 按单品尺寸匹配并回填 carton_id（需先执行 ecommerce_sku_carton_alter.sql）
USE ai_manager_admin;

-- 演示 SKU（与 EcCartonMatcher 计算结果一致）
UPDATE ec_sku SET carton_id = 1 WHERE sku_code IN ('MUG-W-350', 'MUG-B-350');
UPDATE ec_sku SET carton_id = 2 WHERE sku_code = 'BOX-S-3L';
UPDATE ec_sku SET carton_id = 2 WHERE sku_code = 'BOX-L-8L';
UPDATE ec_sku SET carton_id = 3 WHERE sku_code IN ('MAT-4PC-GR', 'MAT-4PC-BE');

-- 其余有完整单品尺寸、尚未配置 carton_id 的 SKU：优先同工厂最小体积纸箱（同向尺寸比较）
UPDATE ec_sku s
INNER JOIN ec_product p ON s.product_id = p.id
INNER JOIN (
    SELECT
        s2.id AS sku_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(c.id ORDER BY (c.length_cm * c.width_cm * c.height_cm) ASC, c.id ASC),
            ',', 1
        ) + 0 AS carton_id
    FROM ec_sku s2
    INNER JOIN ec_product p2 ON s2.product_id = p2.id
    INNER JOIN ec_carton c ON c.deleted = 0
        AND c.length_cm >= s2.product_length_cm
        AND c.width_cm >= s2.product_width_cm
        AND c.height_cm >= s2.product_height_cm
        AND (p2.factory_id IS NULL OR c.factory_id = p2.factory_id)
    WHERE s2.deleted = 0
      AND s2.product_length_cm IS NOT NULL
      AND s2.product_width_cm IS NOT NULL
      AND s2.product_height_cm IS NOT NULL
      AND s2.product_length_cm > 0
      AND s2.product_width_cm > 0
      AND s2.product_height_cm > 0
      AND (s2.carton_id IS NULL OR s2.carton_id = 0)
    GROUP BY s2.id
) matched ON matched.sku_id = s.id
SET s.carton_id = matched.carton_id
WHERE s.carton_id IS NULL OR s.carton_id = 0;

-- 同工厂无匹配时，放宽到全部纸箱
UPDATE ec_sku s
INNER JOIN (
    SELECT
        s2.id AS sku_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(c.id ORDER BY (c.length_cm * c.width_cm * c.height_cm) ASC, c.id ASC),
            ',', 1
        ) + 0 AS carton_id
    FROM ec_sku s2
    INNER JOIN ec_carton c ON c.deleted = 0
        AND c.length_cm >= s2.product_length_cm
        AND c.width_cm >= s2.product_width_cm
        AND c.height_cm >= s2.product_height_cm
    WHERE s2.deleted = 0
      AND (s2.carton_id IS NULL OR s2.carton_id = 0)
      AND s2.product_length_cm IS NOT NULL
      AND s2.product_width_cm IS NOT NULL
      AND s2.product_height_cm IS NOT NULL
      AND s2.product_length_cm > 0
      AND s2.product_width_cm > 0
      AND s2.product_height_cm > 0
    GROUP BY s2.id
) matched ON matched.sku_id = s.id
SET s.carton_id = matched.carton_id
WHERE s.carton_id IS NULL OR s.carton_id = 0;



-- ########## FILE: ecommerce_platform_link_data_backfill.sql ##########

-- 上架链接 SKU：按新公式回填 cost_price / min_set_amount（在 ai_manager_admin 库执行）
-- 前提：已执行 ecommerce_platform_link_alter.sql
-- 说明：
--   1. 仅处理 sku_codes 为单个货号（不含英文逗号）的行；组合货号请在管理端重新保存
--   2. 成本 = SKU 售价 + 纸箱单价 + 快递费（默认站点 + 广东省）
--   3. 最低设置金额 = 成本 ÷ (折扣/100) + 优惠券
--   4. 计费重量 = 外箱毛重 ÷ 每箱装数（装数>1 时），否则用外箱毛重，缺省 0.3kg
USE ai_manager_admin;

UPDATE ec_listing_link_sku s
INNER JOIN ec_sku sk
        ON sk.deleted = 0
       AND sk.sku_code = TRIM(s.sku_codes)
       AND s.sku_codes NOT LIKE '%,%'
LEFT JOIN ec_carton c
       ON c.deleted = 0
      AND c.id = sk.carton_id
INNER JOIN ec_express_station st
        ON st.deleted = 0
       AND st.is_default = 1
INNER JOIN ec_express_price ep
        ON ep.deleted = 0
       AND ep.station_id = st.id
       AND ep.province_name = '广东省'
SET
    s.cost_price = ROUND(
        sk.sale_price
        + COALESCE(c.unit_price, 0)
        + (
            CASE
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 0.3 THEN ep.price_w03_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 0.5 THEN ep.price_w05_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 1.0 THEN ep.price_w1_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 1.5 THEN ep.price_w15_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 2.0 THEN ep.price_w2_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 2.5 THEN ep.price_w25_kg
                WHEN GREATEST(
                    COALESCE(
                        CASE
                            WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                THEN sk.carton_gross_weight_kg / sk.units_per_carton
                            ELSE sk.carton_gross_weight_kg
                        END,
                        0.3
                    ),
                    0.001
                ) <= 3.0 THEN ep.price_w3_kg
                ELSE ep.over3_first_price
                     + ep.over3_additional_price * GREATEST(
                         CEILING(
                             GREATEST(
                                 COALESCE(
                                     CASE
                                         WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                             THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                         ELSE sk.carton_gross_weight_kg
                                     END,
                                     0.3
                                 ),
                                 0.001
                             )
                         ) - 3,
                         1
                     )
            END
        ),
        2
    ),
    s.min_set_amount = ROUND(
        (
            sk.sale_price
            + COALESCE(c.unit_price, 0)
            + (
                CASE
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 0.3 THEN ep.price_w03_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 0.5 THEN ep.price_w05_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 1.0 THEN ep.price_w1_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 1.5 THEN ep.price_w15_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 2.0 THEN ep.price_w2_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 2.5 THEN ep.price_w25_kg
                    WHEN GREATEST(
                        COALESCE(
                            CASE
                                WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                    THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                ELSE sk.carton_gross_weight_kg
                            END,
                            0.3
                        ),
                        0.001
                    ) <= 3.0 THEN ep.price_w3_kg
                    ELSE ep.over3_first_price
                         + ep.over3_additional_price * GREATEST(
                             CEILING(
                                 GREATEST(
                                     COALESCE(
                                         CASE
                                             WHEN sk.units_per_carton IS NOT NULL AND sk.units_per_carton > 1
                                                 THEN sk.carton_gross_weight_kg / sk.units_per_carton
                                             ELSE sk.carton_gross_weight_kg
                                         END,
                                         0.3
                                     ),
                                     0.001
                                 )
                             ) - 3,
                             1
                         )
                END
            )
        ) / (s.discount_pct / 100) + s.coupon_amount,
        2
    )
WHERE s.deleted = 0
  AND sk.sale_price IS NOT NULL;

-- 回填后抽查（马克杯演示链接预期：白色 cost=47.40 min=54.67，黑色 cost=49.40 min=56.89）
-- SELECT id, sku_name, sku_codes, discount_pct, coupon_amount, min_set_amount, cost_price
-- FROM ec_listing_link_sku
-- WHERE deleted = 0
-- ORDER BY link_id, sort_order;


