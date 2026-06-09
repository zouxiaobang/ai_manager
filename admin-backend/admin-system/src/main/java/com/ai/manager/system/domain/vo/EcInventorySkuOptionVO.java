package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcInventorySkuOptionVO {

    private String skuCode;

    private String specName;

    private String productName;

    private Long factoryId;

    private String factoryName;

    private Long productId;

    private String skuStatus;

    private Boolean inboundAllowed;

    private Boolean hasInventory;

    private Integer quantity;

    private Integer alertThreshold;

    private Boolean ignoreAlert;
}
