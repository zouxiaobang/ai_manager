package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcInventoryInboundRequest {

    private String skuCode;

    private Integer quantity;

    private Integer alertThreshold;

    private Boolean ignoreAlert;

    private String remark;
}
