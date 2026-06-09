package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcInventoryInboundBriefVO {

    private Long id;

    private String orderNo;

    private String status;

    private Integer quantity;

    private Integer receivedQuantity;

    private LocalDateTime orderTime;

    private LocalDateTime expectedDeliveryTime;

    private LocalDateTime actualReceiptTime;
}
