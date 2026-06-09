package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcInboundOrderConfirmLineItem {

    private Long lineId;

    private Integer receivedQuantity;
}
