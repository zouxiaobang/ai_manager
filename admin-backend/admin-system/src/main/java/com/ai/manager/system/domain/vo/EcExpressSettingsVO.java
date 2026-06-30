package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcExpressSettingsVO {

    private Integer headerRow = 1;

    private Integer dataStartRow = 2;

    private Boolean includeLabelPriceDefault = false;

    private LocalDateTime updateTime;
}
