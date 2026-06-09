package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EcProductSaveRequest {

    private String name;

    private Long factoryId;

    private String description;

    /** 退点(百分比) */
    private BigDecimal rebatePct;

    /** 图片文件名 */
    private String imageName;

    private String status;

    private List<EcSkuSaveItem> skus;
}
