package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcProductListItemVO {

    private Long id;

    private String name;

    private String imageName;

    private Long factoryId;

    private String factoryName;

    private BigDecimal rebatePct;

    private String status;

    private Integer skuCount;

    private LocalDateTime updateTime;
}
