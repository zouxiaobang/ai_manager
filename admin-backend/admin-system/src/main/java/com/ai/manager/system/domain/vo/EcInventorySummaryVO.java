package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 库存统计数据 VO（专用汇总接口，替代前端从列表 API 拉取全量数据计算的方式）
 */
@Data
public class EcInventorySummaryVO {

    /** SKU 总数 */
    private int skuCount;

    /** 库存总数量 */
    private int totalQuantity;

    /** 库存总价值（元） */
    private long totalStockValue;

    /** 预警 SKU 数量 */
    private int alertCount;

    /** 各状态 SKU 数量分布 { normal, low, zero } */
    private Map<String, Integer> statusCounts;

    /** 库存健康度评分（0-100） */
    private int healthScore;

    /** 累计入库货值（元），按工厂过滤时返回该工厂的值 */
    private BigDecimal inboundValue;
}
