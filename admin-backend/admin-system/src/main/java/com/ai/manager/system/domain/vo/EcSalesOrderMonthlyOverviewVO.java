package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcSalesOrderMonthlyOverviewVO {

    private String orderMonth;

    private int totalOrderCount;

    private int importedShopCount;

    private int totalShopCount;

    private int pendingReviewCount;

    private LocalDateTime lastImportTime;

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
