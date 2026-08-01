-- AI 知识库系统参数（键值 JSON）
-- 在 ai_manager_admin 库执行
USE ai_manager_admin;

CREATE TABLE IF NOT EXISTS ai_knowledge_config (
    config_key   VARCHAR(64)  NOT NULL COMMENT '配置键 (model_config / rag_config)',
    config_json  TEXT         NOT NULL COMMENT '配置 JSON',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库系统参数';

-- 插入默认模型配置
INSERT INTO ai_knowledge_config (config_key, config_json) VALUES
('model_config', '{"provider":"openai","apiKey":"","apiBaseUrl":"https://api.openai.com/v1","model":"gpt-4o","temperature":0.7,"maxTokens":4096,"embeddingModel":"text-embedding-3-small"}')
ON DUPLICATE KEY UPDATE config_key = config_key;
