package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcNotificationSettingsSaveRequest {

    @NotNull
    private Boolean inventoryAlertEnabled;

    @NotNull
    private Boolean zeroStockAlertEnabled;

    @NotNull
    private Boolean settlementRemindEnabled;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer settlementRemindDayOfMonth;
}
