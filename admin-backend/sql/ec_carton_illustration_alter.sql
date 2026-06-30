-- 纸箱材质（0~2：牛皮 / 白卡 / 瓦楞）
USE ai_manager_admin;

ALTER TABLE ec_carton
    ADD COLUMN illustration_variant TINYINT DEFAULT NULL COMMENT '纸箱材质 0~2' AFTER remark;
