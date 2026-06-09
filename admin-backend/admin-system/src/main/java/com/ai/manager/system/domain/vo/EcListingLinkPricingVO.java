package com.ai.manager.system.domain.vo;

import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcListingLinkPricingVO {

    private String skuCodes;

    private BigDecimal skuAmount;

    private BigDecimal cartonAmount;

    private BigDecimal expressAmount;

    private BigDecimal baseCostAmount;

    private BigDecimal platformFeeAmount;

    private BigDecimal platformFeePct;

    private BigDecimal fixedPlatformFee;

    private BigDecimal costPrice;

    private BigDecimal minSetAmount;

    private BigDecimal actualSetAmount;

    private BigDecimal profit;

    private BigDecimal shipWeightKg;

    private String provinceName;

    private EcListingLinkPricingRisk pricingRisk;

    private String costFormula;
}