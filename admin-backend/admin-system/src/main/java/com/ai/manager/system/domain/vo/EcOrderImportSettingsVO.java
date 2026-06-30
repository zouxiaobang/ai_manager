package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcOrderImportSettingsVO {

    private Integer headerRow = 1;

    private Integer dataStartRow = 2;

    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    private LocalDateTime updateTime;
}
