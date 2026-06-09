-- 导入预览行：平台状态与系统行状态（支持状态未映射时人工指定）
USE ai_manager_admin;

ALTER TABLE ec_order_import_row
    ADD COLUMN platform_line_status VARCHAR(64) DEFAULT NULL COMMENT 'Excel 平台子订单/退款状态原文' AFTER manual_cost_price,
    ADD COLUMN line_status VARCHAR(16) DEFAULT NULL COMMENT '解析或人工指定的系统行状态' AFTER platform_line_status,
    ADD COLUMN status_match_status VARCHAR(16) DEFAULT NULL COMMENT '状态映射 MATCHED/UNMATCHED' AFTER line_status;
