package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EcInventoryPackingEstimateVO {

    /** 出库数量（用于估算） */
    private Integer outboundQty;

    /** 每箱装货数 */
    private Integer unitsPerCarton;

    /** 预估占用箱数（向上取整） */
    private Integer cartonsNeeded;

    /** 匹配纸箱 ID */
    private Long cartonId;

    /** 匹配纸箱名称 */
    private String cartonName;

    /** 单箱体积（cm³） */
    private BigDecimal cartonVolumeCm3;

    /** 预估总体积（cm³） */
    private BigDecimal totalVolumeCm3;
}
