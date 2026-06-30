-- 快递站点：自定义头像（上传图片文件名，存于 uploads/ecommerce/）
USE ai_manager_admin;

ALTER TABLE ec_express_station
    ADD COLUMN avatar_url VARCHAR(256) DEFAULT NULL COMMENT '站点头像(上传文件名)' AFTER name;
