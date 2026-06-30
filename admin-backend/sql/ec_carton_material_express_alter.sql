-- 纸箱材质扩展：3 = 普通快递盒（黄色）
ALTER TABLE ec_carton
    MODIFY COLUMN illustration_variant TINYINT DEFAULT NULL COMMENT '纸箱材质 0~3（牛皮/白卡/瓦楞/普通快递盒）';
