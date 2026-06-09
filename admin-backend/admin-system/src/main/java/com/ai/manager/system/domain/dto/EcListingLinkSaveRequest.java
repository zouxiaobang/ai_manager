package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcListingLinkSaveRequest {

    private String name;

    private Long shopId;

    private String platformUrl;

    /** 关联商品 SPU ID 列表 */
    private List<Long> productIds;

    private LocalDateTime listingTime;

    private String remark;

    private String status;

    private List<EcListingLinkSkuItem> skus;
}
