package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcListingLinkSkuItem {

    private Long id;

    private String skuName;

    private String skuCodes;

    private BigDecimal discountPct;

    private BigDecimal couponAmount;

    /** 真实设置金额（可手动填写，利润由服务端自动计算） */
    private BigDecimal actualSetAmount;

    private Integer sortOrder;
}
