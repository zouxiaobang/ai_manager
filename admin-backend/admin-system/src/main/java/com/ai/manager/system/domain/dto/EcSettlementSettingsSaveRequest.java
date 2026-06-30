package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EcSettlementSettingsSaveRequest {

    @NotBlank
    @Pattern(regexp = "ESTIMATED|ACTUAL_PREFERRED")
    private String profitDisplayMode;

    @NotNull
    private Boolean costIncludesFreight;
}
