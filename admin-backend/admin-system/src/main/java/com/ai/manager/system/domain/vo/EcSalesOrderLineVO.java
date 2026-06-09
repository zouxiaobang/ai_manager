package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcSalesOrderLineVO {

    private Long id;

    private Long orderId;

    private Integer sortOrder;

    private Long listingLinkSkuId;

    private String linkName;

    private String skuSpecName;

    private String skuCodes;

    private Integer skuQuantity;

    private Integer shippedQuantity;

    private Integer shortQuantity;

    private String status;

    private String platformLineStatus;

    private String refundType;

    private LocalDateTime refundTime;

    private BigDecimal refundAmount;

    private BigDecimal lossAmount;

    private BigDecimal unitPrice;

    private BigDecimal discountPct;

    private BigDecimal lineCouponAmount;

    private BigDecimal lineReceivedAmount;

    private BigDecimal skuAmount;

    private BigDecimal cartonAmount;

    private BigDecimal expressAmount;

    private BigDecimal baseCostAmount;

    private BigDecimal platformFeeAmount;

    private BigDecimal costPrice;

    private BigDecimal minSetAmount;

    private BigDecimal profit;

    private String pricingRisk;

    private String platformLineNo;

    private String platformItemName;

    private List<EcSalesOrderShortageVO> shortages;
}
