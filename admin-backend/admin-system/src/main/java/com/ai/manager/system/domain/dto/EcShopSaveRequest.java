package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcShopSaveRequest {

    /** 店铺名称 */
    @NotBlank(message = "店铺名称不能为空")
    private String name;

    private String nameEn;

    private String avatarUrl;

    /** 所属平台 */
    @NotNull(message = "平台不能为空")
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
