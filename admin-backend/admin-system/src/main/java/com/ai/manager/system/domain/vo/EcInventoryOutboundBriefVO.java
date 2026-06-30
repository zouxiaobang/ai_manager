package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcInventoryOutboundBriefVO {

    private Long id;

    private String orderNo;

    private String status;

    private Integer quantity;

    private Integer shippedQuantity;

    private LocalDateTime orderTime;

    private LocalDateTime expectedShipTime;

    private LocalDateTime actualShipTime;
}
