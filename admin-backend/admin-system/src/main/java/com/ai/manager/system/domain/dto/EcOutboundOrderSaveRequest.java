package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcOutboundOrderSaveRequest {

    private Long factoryId;

    private Long customerFactoryId;

    private String remark;

    private LocalDateTime orderTime;

    private LocalDateTime expectedShipTime;

    private List<EcOutboundOrderLineItem> lines;
}
