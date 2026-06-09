package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcInboundOrderLineVO {

    private Long id;

    private String skuCode;

    private String specName;

    private String productName;

    /** 下单数量 */
    private Integer quantity;

    /** 实际收货数量 */
    private Integer receivedQuantity;
}
