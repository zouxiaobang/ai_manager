package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class EcOrderImportStatusSettingsVO {

    private String defaultLineStatus = "PAID";

    private Map<String, String> statusMapping = new LinkedHashMap<>();

    private LocalDateTime updateTime;
}
