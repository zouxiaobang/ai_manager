package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcSalesOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long shopId;

    private String shopName;

    private Long platformId;

    private String platformName;

    private String platformOrderNo;

    private String source;

    private String status;

    private String platformStatus;

    private Long expressStationId;

    private String expressStationName;

    private LocalDateTime orderTime;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime completeTime;

    private String buyerName;

    private String buyerPhone;

    private String receiveProvince;

    private String receiveCity;

    private String receiveDistrict;

    private String receiveAddress;

    private String trackingNumber;

    private String buyerRemark;

    private String sellerRemark;

    private BigDecimal receivedAmount;

    private BigDecimal totalCostAmount;

    private BigDecimal freightAmount;

    private BigDecimal estimatedFreightAmount;

    private BigDecimal actualFreightAmount;

    private BigDecimal orderCouponAmount;

    private BigDecimal platformFeeAmount;

    private BigDecimal profitAmount;

    private BigDecimal totalLossAmount;

    private Boolean hasShortage;

    private Long importBatchId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcSalesOrderLineVO> lines;

    private Integer lineCount;
}
