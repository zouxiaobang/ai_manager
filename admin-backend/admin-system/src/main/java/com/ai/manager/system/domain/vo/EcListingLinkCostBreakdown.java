package com.ai.manager.system.domain.vo;

import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcListingLinkCostBreakdown {

    private BigDecimal skuAmount;
    private BigDecimal cartonAmount;
    private BigDecimal expressAmount;
    /** SKU + 纸箱 + 快递 */
    private BigDecimal baseCostAmount;
    /** 平台扣费（盈亏平衡口径） */
    private BigDecimal platformFeeAmount;
    /** 含平台费的盈亏平衡成本 */
    private BigDecimal costPrice;
    private BigDecimal platformFeePct;
    private BigDecimal fixedPlatformFee;
    private BigDecimal shipWeightKg;
    private String provinceName;
    private EcListingLinkPricingRisk pricingRisk;
    /** 成本计算公式说明 */
    private String costFormula;
}
