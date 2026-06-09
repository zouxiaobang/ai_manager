package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcShopSaveRequest {

    private String name;

    private String nameEn;

    private Long platformId;

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
}
