package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcSettlementBuyerExcludeVO {

    private Long id;

    private Long shopId;

    private String shopName;

    private String buyerName;

    private String remark;

    private Integer enabled;

    private LocalDateTime createTime;
}
