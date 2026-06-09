package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcSalesOrderShortageVO {

    private Long id;

    private String skuCode;

    private Integer needQty;

    private Integer deductedQty;

    private Integer shortQty;

    private String status;

    private LocalDateTime createTime;
}
