package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EcSalesOrderImportManualCostUpdateRequest {

    private List<Item> items;

    /** 提交时排除的行状态（对应行不纳入核对，未匹配 SKU 行手动成本默认为 0） */
    private List<String> excludedLineStatuses;

    @Data
    public static class Item {
        private Long rowId;
        private BigDecimal manualCostPrice;
        /** 人工指定的系统行状态（PAID/SHIPPED/COMPLETED/CANCELLED/PARTIAL_REFUND/REFUNDED/RETURNED） */
        private String lineStatus;
    }
}
