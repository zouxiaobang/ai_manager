package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcCartonCalculateResultVO {

    /** 按尺寸规则匹配的最合适纸箱 */
    private EcCartonListItemVO matchedCarton;

    /** 库存纸箱（当前与 matchedCarton 一致，后续接入库存逻辑后独立计算） */
    private EcCartonListItemVO inventoryCarton;
}
