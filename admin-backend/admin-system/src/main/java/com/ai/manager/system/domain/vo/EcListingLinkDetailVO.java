package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcListingLinkDetailVO {

    private Long id;

    private String name;

    private Long shopId;

    private String shopName;

    private Long platformId;

    private String platformName;

    private String platformUrl;

    /** 关联商品列表 */
    private List<EcListingLinkProductVO> products;

    /** 关联商品名称摘要（列表展示） */
    private String productNames;

    private LocalDateTime listingTime;

    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcListingLinkSkuVO> skus;

    private Integer skuCount;

    private String costFormula;
}
