package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EcOutboundOrderConfigSaveRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    @Size(max = 512)
    private String address;

    @Size(max = 64)
    private String tel;

    @Size(max = 64)
    private String preparedBy;

    @Size(max = 64)
    private String approvedBy;

    @Size(max = 64)
    private String warehouseKeeper;

    private List<String> requirementItems;

    private List<String> noteItems;
}
