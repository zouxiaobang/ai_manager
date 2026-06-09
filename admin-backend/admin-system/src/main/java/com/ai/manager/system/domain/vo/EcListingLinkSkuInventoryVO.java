package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcListingLinkSkuInventoryVO {

    private String skuCode;

    private String specName;

    private String skuStatus;

    private Integer quantity;

    private Integer alertThreshold;

    private Boolean alertActive;

    private Boolean inboundAllowed;
}
