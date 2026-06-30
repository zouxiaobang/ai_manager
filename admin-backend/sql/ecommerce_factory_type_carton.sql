-- 工厂类型扩展：CARTON 纸箱厂（在 ai_manager_admin 库执行，无需改表结构）
-- factory_type 字段已支持 VARCHAR(16)，新增枚举值 CARTON

USE ai_manager_admin;

-- 可选：更新字段注释
ALTER TABLE ec_factory
    MODIFY COLUMN factory_type VARCHAR(16) NOT NULL DEFAULT 'PRODUCTION'
        COMMENT 'PRODUCTION/CUSTOMER/CARTON';
