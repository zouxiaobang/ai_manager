package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcNotificationSettingsVO {

    private Boolean inventoryAlertEnabled = true;

    private Boolean zeroStockAlertEnabled = true;

    private Boolean settlementRemindEnabled = true;

    private Integer settlementRemindDayOfMonth = 25;

    private LocalDateTime updateTime;
}
