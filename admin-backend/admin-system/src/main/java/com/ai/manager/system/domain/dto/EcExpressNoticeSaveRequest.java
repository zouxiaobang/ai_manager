package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcExpressNoticeSaveRequest {

    private Long stationId;

    private String content;

    private Boolean highlightRed;

    private Integer sortOrder;
}
