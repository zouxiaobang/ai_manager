package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcInventoryAdjustRequest {

    /** DEDUCT 扣除 / RECLAIM 回收 */
    private String changeType;

    private Integer changeQty;
}
