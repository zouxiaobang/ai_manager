-- ============================================================
-- RAG 知识库 - MySQL 建表语句
-- 在 ai_manager_admin 库执行
-- ============================================================
USE ai_manager_admin;

-- RAG 文档表
CREATE TABLE IF NOT EXISTS rag_document (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name     VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_type     VARCHAR(50)   NOT NULL COMMENT '文件类型 (pdf/txt/md/html/docx)',
    file_size     BIGINT        NOT NULL COMMENT '文件大小（bytes）',
    file_path     VARCHAR(500)  NOT NULL COMMENT '服务器存储路径',
    chunk_count   INT           DEFAULT 0 COMMENT '分块数',
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending'
                  COMMENT '状态: pending / processing / ready / failed',
    error_message TEXT          COMMENT '错误信息',
    retry_count   INT           NOT NULL DEFAULT 0
                  COMMENT '处理重试次数（达上限后启动不再自动重投，防止死循环）',
    indexed_at    DATETIME      COMMENT '索引完成时间',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 知识库文档';

-- RAG 文档分块表
CREATE TABLE IF NOT EXISTS rag_chunk (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   BIGINT       NOT NULL COMMENT '所属文档 ID',
    chunk_index   INT          NOT NULL COMMENT '块序号（从 0 开始）',
    content       MEDIUMTEXT   NOT NULL COMMENT '分块文本内容',
    token_count   INT          DEFAULT 0 COMMENT '预估 Token 数',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_document_id (document_id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES rag_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 文档分块';
