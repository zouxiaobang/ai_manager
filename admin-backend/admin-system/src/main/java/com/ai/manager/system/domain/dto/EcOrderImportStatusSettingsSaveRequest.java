package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
public class EcOrderImportStatusSettingsSaveRequest {

    @NotBlank
    @Pattern(regexp = "PAID|SHIPPED|COMPLETED|CANCELLED|PARTIAL_REFUND|REFUNDED|RETURNED")
    private String defaultLineStatus;

    private Map<String, String> statusMapping;
}
