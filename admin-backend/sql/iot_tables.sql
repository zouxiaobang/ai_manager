-- =============================================================================
-- IoT 设备功能域表（在 ai_manager_admin 库执行）
-- 对应文档《new-architecture-design.md》7.6 数据模型
-- 约定：snake_case，含 deleted/create_time/update_time，utf8mb4
-- =============================================================================

USE ai_manager_admin;

-- 设备表
CREATE TABLE IF NOT EXISTS iot_device (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    uuid             VARCHAR(64)  NOT NULL COMMENT '设备全局唯一标识(后端签发)',
    client_id        VARCHAR(64)  DEFAULT NULL COMMENT '客户端 UUID(设备固件生成)',
    mac              VARCHAR(64)  NOT NULL COMMENT 'MAC 地址(小写去冒号)',
    model            VARCHAR(64)  DEFAULT NULL COMMENT '机型 supermini-c3 / kyle-s3-lcd',
    chip             VARCHAR(32)  DEFAULT NULL COMMENT '芯片 esp32c3 / esp32s3',
    firmware_version VARCHAR(64)  DEFAULT NULL COMMENT '当前固件版本',
    ws_token         VARCHAR(128) DEFAULT NULL COMMENT 'WebSocket 握手 Bearer Token',
    activated_at     DATETIME     DEFAULT NULL COMMENT '激活时间',
    last_seen_at     DATETIME     DEFAULT NULL COMMENT '最近在线/上报时间',
    status           VARCHAR(16)  NOT NULL DEFAULT 'UNBOUND' COMMENT 'UNBOUND/BOUND/ACTIVATED/ONLINE/OFFLINE',
    session_id       VARCHAR(64)  DEFAULT NULL COMMENT '最近会话 ID',
    ota_state        VARCHAR(16)  NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/UPGRADING/SUCCESS/FAILED',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device_mac (mac),
    KEY idx_iot_device_uuid (uuid),
    KEY idx_iot_device_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 设备';

-- 固件表
CREATE TABLE IF NOT EXISTS iot_firmware (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    version      VARCHAR(64)  NOT NULL COMMENT '固件版本号',
    file_path    VARCHAR(512) NOT NULL COMMENT '本地存储路径',
    file_hash    CHAR(64)     NOT NULL COMMENT '文件 SHA-256',
    size         BIGINT       NOT NULL DEFAULT 0 COMMENT '文件字节数',
    force_upgrade TINYINT     NOT NULL DEFAULT 0 COMMENT '是否强制升级 0/1',
    status       VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    release_note VARCHAR(1024) DEFAULT NULL COMMENT '版本说明',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_firmware_version (version),
    KEY idx_iot_firmware_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 固件';

-- OTA 升级记录表
CREATE TABLE IF NOT EXISTS iot_ota_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   BIGINT       NOT NULL COMMENT '设备 ID',
    firmware_id BIGINT       NOT NULL COMMENT '固件 ID',
    state       VARCHAR(16)  NOT NULL DEFAULT 'UPGRADING' COMMENT 'UPGRADING/SUCCESS/FAILED/CANCELED',
    progress    INT          NOT NULL DEFAULT 0 COMMENT '下载进度 0~100',
    started_at  DATETIME     DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME     DEFAULT NULL COMMENT '完成时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_iot_ota_record_device (device_id),
    KEY idx_iot_ota_record_firmware (firmware_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT OTA 升级记录';

-- 语音会话表
CREATE TABLE IF NOT EXISTS iot_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   BIGINT       NOT NULL COMMENT '设备 ID',
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话 ID(下发设备，贯穿协议)',
    started_at  DATETIME     NOT NULL COMMENT '会话开始时间',
    ended_at    DATETIME     DEFAULT NULL COMMENT '会话结束时间',
    turn_count  INT          NOT NULL DEFAULT 0 COMMENT '会话内唤醒/对话轮次',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_session_id (session_id),
    KEY idx_iot_session_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 语音会话';
