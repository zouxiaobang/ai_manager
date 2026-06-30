package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcMonthlySettlementVO {

    private String settlementMonth;

    /** 是否已有入库快照（仅读取快照接口返回） */
    private Boolean saved;

    /** 快照统计完成时间 */
    private LocalDateTime calculatedAt;

    /** 当月是否已导入快递账单 */
    private Boolean expressBillImported;

    private List<ShopSummary> shops;

    @Data
    public static class ShopSummary {
        private Long shopId;
        private String shopName;
        private BigDecimal totalRevenue;
        private BigDecimal estimatedTotalCost;
        private BigDecimal actualTotalCost;
        private BigDecimal estimatedTotalProfit;
        private BigDecimal actualTotalProfit;
        private BigDecimal totalActualFreight;
        private Integer includedOrderCount;
        private Integer excludedOrderCount;
        private Integer pendingOrderCount;
        private MaxProfitOrder maxProfitOrder;
        private List<PendingOrder> pendingOrders;
    }

    @Data
    public static class MaxProfitOrder {
        private Long orderId;
        private String orderNo;
        private String platformOrderNo;
        private String productName;
        private String skuName;
        private String productImageUrl;
        private LocalDateTime orderTime;
        private BigDecimal receivedAmount;
        private BigDecimal estimatedCostAmount;
        private BigDecimal actualCostAmount;
        /** 预估利润 */
        private BigDecimal profitAmount;
        /** 实际利润 */
        private BigDecimal actualProfitAmount;
        /** 实际利润未知时的原因码：EXPRESS_BILL_NOT_IMPORTED / ACTUAL_FREIGHT_MISSING */
        private String actualProfitUnknownReason;
    }

    @Data
    public static class PendingOrder {
        private Long orderId;
        private String orderNo;
        private String platformOrderNo;
        private String status;
        private String buyerName;
        private String productName;
        private String skuName;
        private BigDecimal receivedAmount;
        private LocalDateTime orderTime;
        /** 是否已人工决策 */
        private Boolean decided;
        /** 人工决策：true纳入 false不纳入 null未决策 */
        private Boolean included;
    }
}
