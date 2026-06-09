package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SysImportProfileVO {

    private Long id;

    private String name;

    private String bizType;

    private Long platformId;

    private String platformName;

    private String scopeKey;

    private Long shopId;

    private String fileType;

    private Integer headerRow;

    private Integer dataStartRow;

    private String sheetName;

    private Map<String, String> columnMapping;

    private Map<String, String> valueMapping;

    private Map<String, Object> extraConfig;

    private Integer enabled;

    private String remark;

    private LocalDateTime updateTime;
}
