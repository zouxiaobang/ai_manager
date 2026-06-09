package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcPlatformListItemVO {

    private Long id;

    private String name;

    private String nameEn;

    private Integer platformCode;

    private String channelType;

    private String remark;

    private String status;

    private LocalDateTime updateTime;
}
