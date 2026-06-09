package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EcStocktakeOrderSaveRequest {

    private Long factoryId;

    private String remark;

    private LocalDateTime stocktakeTime;

    private List<EcStocktakeOrderLineItem> lines;
}
