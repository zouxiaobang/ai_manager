package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcSettlementSettingsVO {

    /** ESTIMATED | ACTUAL_PREFERRED */
    private String profitDisplayMode = "ACTUAL_PREFERRED";

    private Boolean costIncludesFreight = true;

    private LocalDateTime updateTime;
}
