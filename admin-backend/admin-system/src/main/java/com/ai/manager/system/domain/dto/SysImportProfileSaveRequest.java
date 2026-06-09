package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SysImportProfileSaveRequest {

    private Long id;

    private String name;

    private String bizType;

    private Long platformId;

    /** 作用域键，如 platform:1 / express_station:3 */
    private String scopeKey;

    private Long shopId;

    private String fileType;

    private Integer headerRow;

    private Integer dataStartRow;

    private String sheetName;

    /** backend field -> document column name */
    private Map<String, String> columnMapping;

    private Map<String, String> valueMapping;

    private Map<String, Object> extraConfig;

    private String remark;
}
