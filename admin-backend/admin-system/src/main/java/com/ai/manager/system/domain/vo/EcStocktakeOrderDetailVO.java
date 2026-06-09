package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcStocktakeOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long factoryId;

    private String factoryName;

    private String status;

    private String remark;

    private LocalDateTime stocktakeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<EcStocktakeOrderLineVO> lines;
}
