package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcCartonListItemVO {

    private Long id;

    private Long factoryId;

    private String factoryName;

    private String name;

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal unitPrice;

    private String remark;

    private LocalDateTime updateTime;
}
