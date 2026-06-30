-- 平台头像（上传文件名，与电商图片目录一致）
ALTER TABLE ec_platform
    ADD COLUMN avatar_url VARCHAR(256) DEFAULT NULL COMMENT '平台头像(上传文件名)' AFTER name_en;
