package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class EcSalesOrderMonthlyOverviewVO {

    private String orderMonth;

    private int totalOrderCount;

    private int importedShopCount;

    private int totalShopCount;

    private int pendingReviewCount;

    private LocalDateTime lastImportTime;

    /** 各状态订单数，如 {"DRAFT": 5, "PAID": 10, ...} */
    private Map<String, Integer> statusCounts;

    /** 总营收（所有订单 received_amount 之和） */
    private BigDecimal totalRevenue;

    /** 总利润（所有订单 profit_amount 之和） */
    private BigDecimal totalProfit;

    private List<ShopImportStatus> shops;

    @Data
    public static class ShopImportStatus {

        private Long shopId;

        private String shopName;

        private String platformName;

        private Integer platformCode;

        private String shopAvatarUrl;

        private String platformAvatarUrl;

        private int orderCount;

        /** NOT_IMPORTED | IMPORTED | PENDING_REVIEW */
        private String status;

        private LocalDateTime lastImportTime;

        private Long pendingBatchId;

        private int pendingReviewRows;
    }
}
