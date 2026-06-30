-- 采购单系统配置（在 ai_manager_admin 库执行）
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ec_purchase_order_config (
    id                 BIGINT       NOT NULL COMMENT '固定为 1',
    title              VARCHAR(128) NOT NULL DEFAULT '唯十嘉采购单' COMMENT '采购单标题',
    address            VARCHAR(512) DEFAULT NULL COMMENT '地址',
    tel                VARCHAR(64)  DEFAULT NULL COMMENT '联系电话',
    requirement_items  TEXT         DEFAULT NULL COMMENT '订单要求 JSON 数组',
    note_items         TEXT         DEFAULT NULL COMMENT '注意事项 JSON 数组',
    prepared_by        VARCHAR(64)  DEFAULT NULL COMMENT '制单人签名',
    prepared_phone     VARCHAR(64)  DEFAULT NULL COMMENT '制单人电话',
    receiver_name      VARCHAR(64)  DEFAULT NULL COMMENT '收货人',
    receiver_phone     VARCHAR(64)  DEFAULT NULL COMMENT '收货电话',
    receiver_address   VARCHAR(512) DEFAULT NULL COMMENT '收货地址',
    company_no         VARCHAR(64)  DEFAULT NULL COMMENT '公司编号',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电商采购单系统配置';

INSERT INTO ec_purchase_order_config (
    id, title, address, tel, requirement_items, note_items,
    prepared_by, prepared_phone, receiver_name, receiver_phone, receiver_address, company_no
) VALUES (
    1,
    '唯十嘉采购单',
    '地址：汕头市澄海区莲下镇东湾文化公园',
    'TEL：18819446360',
    '["出货时间、日期不能再拖后！产品必须通过美国站全检测以及CPSIA，CPC检测。","大货产品颜色必须跟左上角图片一样，不能更改。","按新纸箱规格包装，确保货物稳固，运输过程中不会破损。"]',
    '["本单为采购合同，请工厂签字盖章回传，并妥善保管。","产品名称必须与采购单一致，如有变更请提前沟通确认。","请按约定时间交货，如有延误请第一时间通知我司。","货款与发货事宜请与采购联系人核对后执行。"]',
    '张小姐',
    '18819446360',
    '张小姐',
    '18819446360',
    '见上面地址',
    ''
) ON DUPLICATE KEY UPDATE id = id;
