package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 库存概览 VO（服务端聚合，无需前端自行计算）
 */
@Data
public class EcInventoryOverviewVO {

    /** SKU 总数 */
    private int skuCount;

    /** 库存总数量 */
    private int totalQuantity;

    /** 库存总价值（元） */
    private int totalStockValue;

    /** 各状态 SKU 数量 { normal, low, zero } */
    private Map<String, Integer> statusCounts;
}
