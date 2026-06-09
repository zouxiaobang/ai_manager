package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcListingLinkPricingRequest {

    private Long shopId;

    private String skuCodes;

    private BigDecimal discountPct;

    private BigDecimal couponAmount;

    private String provinceName;

    private BigDecimal actualSetAmount;
}