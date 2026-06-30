-- 电商平台 / 店铺 / 上架链接 / 链接 SKU（在 ai_manager_admin 库执行）
USE ai_manager_admin;

-- ========== 1. 平台表 ==========
-- platform_code 与 Java 枚举 EcPlatformCode 一致；channel_type 区分线上/线下渠道
CREATE TABLE IF NOT EXISTS ec_platform (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '平台主键',
    name          VARCHAR(128) NOT NULL COMMENT '平台名称',
    name_en       VARCHAR(128) DEFAULT NULL COMMENT '平台英文名称',
    avatar_url    VARCHAR(256) DEFAULT NULL COMMENT '平台头像(上传文件名)',
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
