package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcSettlementBuyerExcludeSaveRequest {

    private Long id;

    private Long shopId;

    private String buyerName;

    private String remark;

    private Integer enabled;
}
