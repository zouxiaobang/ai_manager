package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcExpressSettingsSaveRequest {

    @NotNull
    @Min(1)
    @Max(100)
    private Integer headerRow;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer dataStartRow;

    @NotNull
    private Boolean includeLabelPriceDefault;
}
