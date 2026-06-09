package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcSkuSaveItem {

    private Long id;

    private String skuCode;

    private String specName;

    /** 退点(百分比)，未填则继承 SPU */
    private BigDecimal rebatePct;

    /** 图片文件名 */
    private String imageName;

    /** 匹配纸箱，新建 SKU 可留空由系统自动匹配 */
    private Long cartonId;

    private BigDecimal salePrice;

    private BigDecimal productLengthCm;

    private BigDecimal productWidthCm;

    private BigDecimal productHeightCm;

    private BigDecimal cartonLengthCm;

    private BigDecimal cartonWidthCm;

    private BigDecimal cartonHeightCm;

    private BigDecimal cartonGrossWeightKg;

    private BigDecimal cartonNetWeightKg;

    private Integer unitsPerCarton;

    private String status;
}
