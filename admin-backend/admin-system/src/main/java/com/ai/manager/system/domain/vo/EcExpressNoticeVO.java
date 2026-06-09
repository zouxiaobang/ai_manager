package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcExpressNoticeVO {

    private Long id;

    private Long stationId;

    private String content;

    private Boolean highlightRed;

    private Integer sortOrder;

    private LocalDateTime updateTime;
}
