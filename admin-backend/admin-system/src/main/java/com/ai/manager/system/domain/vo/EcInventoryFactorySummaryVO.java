package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcInventoryFactorySummaryVO {

    private Long factoryId;

    private String factoryName;

    private Long skuCount;

    private Long totalQuantity;

    private BigDecimal totalStockValue;

    private Long alertSkuCount;
}
