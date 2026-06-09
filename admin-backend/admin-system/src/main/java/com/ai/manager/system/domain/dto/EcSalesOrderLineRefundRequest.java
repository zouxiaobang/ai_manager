package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcSalesOrderLineRefundRequest {

    /** REFUND_ONLY / RETURN_REFUND */
    private String refundType;

    private BigDecimal refundAmount;
}
