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

    /** SKU 图片文件名（优先 SKU，否则商品主图） */
    private String imageName;
}
