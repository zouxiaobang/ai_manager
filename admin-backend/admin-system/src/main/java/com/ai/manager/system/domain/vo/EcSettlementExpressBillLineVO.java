package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcSettlementExpressBillLineVO {

    private Long id;

    private Long billId;

    private Long expressStationId;

    private String source;

    private Long orderId;

    /** 订单所属店铺名称（手动补录列表展示） */
    private String shopName;

    private String platformOrderNo;

    private String orderNo;

    private String trackingNumber;

    private BigDecimal freightAmount;

    private String settlementDestination;

    private BigDecimal weight;

    private LocalDateTime shipTime;

    private String matchStatus;

    private String remark;
}
