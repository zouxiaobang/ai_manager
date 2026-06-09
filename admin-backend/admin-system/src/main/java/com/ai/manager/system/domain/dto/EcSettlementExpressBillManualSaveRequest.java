package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcSettlementExpressBillManualSaveRequest {

    private Long billId;

    private Long expressStationId;

    private List<ManualLineItem> lines;

    @Data
    public static class ManualLineItem {

        private Long lineId;

        private Long orderId;

        private String platformOrderNo;

        private String orderNo;

        private String trackingNumber;

        private BigDecimal freightAmount;

        private String settlementDestination;

        private BigDecimal weight;

        private LocalDateTime shipTime;

        private String remark;
    }
}
