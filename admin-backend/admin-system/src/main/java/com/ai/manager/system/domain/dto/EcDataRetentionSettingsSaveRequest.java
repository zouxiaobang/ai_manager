package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcDataRetentionSettingsSaveRequest {

    @NotNull
    @Min(30)
    @Max(3650)
    private Integer importHistoryRetentionDays;

    @NotNull
    @Min(30)
    @Max(3650)
    private Integer inventoryLogRetentionDays;

    @NotNull
    private Boolean autoCleanupEnabled;
}
