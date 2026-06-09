package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcStocktakeOrderLineVO {

    private Long id;

    private String skuCode;

    private String specName;

    private String productName;

    private Integer bookQuantity;

    private Integer actualQuantity;
}
