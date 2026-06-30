package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcInventoryOutboundRequest {

    private String skuCode;

    private Integer quantity;

    private String remark;
}
