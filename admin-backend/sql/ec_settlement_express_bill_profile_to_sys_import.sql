-- 快递账单列映射并入 sys_import_profile，移除独立配置表
USE ai_manager_admin;

-- 迁移已有 ec_settlement_express_bill_profile 到 sys_import_profile（若表存在）
INSERT INTO sys_import_profile (name, biz_type, platform_id, scope_key, file_type, header_row, data_start_row, column_mapping, enabled, remark)
SELECT CONCAT('快递账单-', COALESCE(s.name, p.express_station_id)), 'SETTLEMENT_EXPRESS_BILL', NULL,
       CONCAT('express_station:', p.express_station_id), 'XLSX', p.header_row, p.data_start_row, p.column_mapping, 1,
       '从 ec_settlement_express_bill_profile 迁移'
FROM ec_settlement_express_bill_profile p
         LEFT JOIN ec_express_station s ON s.id = p.express_station_id
WHERE p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_import_profile sip
    WHERE sip.biz_type = 'SETTLEMENT_EXPRESS_BILL'
      AND sip.scope_key = CONCAT('express_station:', p.express_station_id)
      AND sip.deleted = 0
);

DROP TABLE IF EXISTS ec_settlement_express_bill_profile;
