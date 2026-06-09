package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class EcSettlementOrderDecisionBatchRequest {

    private String settlementMonth;

    private List<Item> items;

    @Data
    public static class Item {
        private Long orderId;
        private Boolean included;
    }
}
