package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcInventorySettingsSaveRequest {

    @NotNull
    @Min(0)
    @Max(99999)
    private Integer defaultAlertThreshold;

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer slowMovingDays;

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer slowMovingFallbackDays;
}
