package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EcOrderImportSettingsSaveRequest {

    @NotNull
    @Min(1)
    @Max(100)
    private Integer headerRow;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer dataStartRow;

    @NotBlank
    @Size(max = 64)
    private String dateFormat;
}
