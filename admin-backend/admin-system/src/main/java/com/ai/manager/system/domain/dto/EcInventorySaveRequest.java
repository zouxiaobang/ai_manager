package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcInventorySaveRequest {

    private String skuCode;

    private Integer quantity;

    private Boolean ignoreAlert;

    private Integer alertThreshold;
}
