package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcInboundOrderSaveRequest {

    private Long factoryId;

    private String remark;

    private LocalDateTime orderTime;

    private LocalDateTime expectedDeliveryTime;

    private List<EcInboundOrderLineItem> lines;
}
