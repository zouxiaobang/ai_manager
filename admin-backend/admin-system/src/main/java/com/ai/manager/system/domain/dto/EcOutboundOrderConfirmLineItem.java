package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcOutboundOrderConfirmLineItem {

    private Long lineId;

    private Integer shippedQuantity;
}
