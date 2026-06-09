package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcMonthlySettlementVO {

    private String settlementMonth;

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
        private BigDecimal profitAmount;
        private BigDecimal receivedAmount;
    }

    @Data
    public static class PendingOrder {
        private Long orderId;
        private String orderNo;
        private String platformOrderNo;
        private String status;
        private String buyerName;
        private BigDecimal receivedAmount;
        private LocalDateTime orderTime;
        /** 是否已人工决策 */
        private Boolean decided;
        /** 人工决策：true纳入 false不纳入 null未决策 */
        private Boolean included;
    }
}
