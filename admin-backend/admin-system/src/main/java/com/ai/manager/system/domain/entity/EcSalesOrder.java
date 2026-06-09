package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ec_sales_order")
public class EcSalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long shopId;

    private String platformOrderNo;

    private String source;

    private String status;

    private String platformStatus;

    private Long expressStationId;

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

    private Integer hasShortage;

    private Long importBatchId;

    private String platformRawJson;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
