package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcSalesOrderSaveRequest {

    private Long shopId;

    private String platformOrderNo;

    private Long expressStationId;

    private LocalDateTime orderTime;

    private LocalDateTime payTime;

    private String platformStatus;

    private String receiveProvince;

    private String buyerName;

    private String buyerPhone;

    private String receiveCity;

    private String receiveDistrict;

    private String receiveAddress;

    private String trackingNumber;

    private String buyerRemark;

    private String sellerRemark;

    private BigDecimal receivedAmount;

    private BigDecimal freightAmount;

    private BigDecimal orderCouponAmount;

    private List<EcSalesOrderLineItem> lines;
}
