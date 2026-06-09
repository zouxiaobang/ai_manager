package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcSalesOrderLineItem {

    private Long listingLinkSkuId;

    /** 手工/导入匹配：链接名称 */
    private String linkName;

    /** 手工/导入匹配：SKU 展示名称 */
    private String skuSpecName;

    private Integer skuQuantity;

    private BigDecimal unitPrice;

    private BigDecimal lineReceivedAmount;

    private BigDecimal lineCouponAmount;

    private String platformLineNo;

    private String platformItemName;

    private String platformLineStatus;

    private Integer sortOrder;

    /** 未匹配链接导入：手动成本（元/套） */
    private BigDecimal manualCostPrice;
}
