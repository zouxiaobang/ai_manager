package com.ai.manager.system.domain.vo;

import com.ai.manager.system.domain.enums.EcListingLinkPricingRisk;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EcListingLinkSkuVO {

    private Long id;

    private String skuName;

    private String skuCodes;

    private BigDecimal discountPct;

    private BigDecimal couponAmount;

    private BigDecimal minSetAmount;

    private BigDecimal costPrice;

    private BigDecimal baseCostAmount;

    private BigDecimal platformFeeAmount;

    private BigDecimal actualSetAmount;

    private BigDecimal profit;

    private BigDecimal skuAmount;

    private BigDecimal cartonAmount;

    private BigDecimal expressAmount;

    private BigDecimal platformFeePct;

    private String provinceName;

    private EcListingLinkPricingRisk pricingRisk;

    private Integer sortOrder;

    private List<EcListingLinkSkuInventoryVO> inventories;
}
