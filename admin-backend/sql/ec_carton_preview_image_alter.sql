-- 纸箱 3D 预览图（保存时生成，存于 uploads/ecommerce/）
USE ai_manager_admin;

ALTER TABLE ec_carton
    ADD COLUMN preview_image VARCHAR(256) DEFAULT NULL COMMENT '3D预览图文件名' AFTER illustration_variant;
