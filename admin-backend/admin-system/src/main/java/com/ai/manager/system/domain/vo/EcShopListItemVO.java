package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcShopListItemVO {

    private Long id;

    private String name;

    private String nameEn;

    private String avatarUrl;

    private Long platformId;

    private String platformName;

    private Integer platformCode;

    private String channelType;

    private String remark;

    private BigDecimal categoryCommissionPct;

    private BigDecimal techServiceFeePct;

    private BigDecimal paymentFeePct;

    private BigDecimal promotionFeePct;

    private BigDecimal fulfillmentFeePct;

    private BigDecimal returnServiceFeePct;

    private BigDecimal installmentFeePct;

    private BigDecimal activityServiceFeePct;

    private BigDecimal annualPlatformFee;

    private BigDecimal depositAmount;

    private BigDecimal shippingInsuranceFee;

    private BigDecimal otherFeePct;

    private String otherFeeRemark;

    private String defaultReceiveProvince;

    private String status;

    private LocalDateTime updateTime;
}
