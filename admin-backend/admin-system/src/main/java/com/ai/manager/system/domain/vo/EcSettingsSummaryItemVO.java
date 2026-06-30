package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcSettingsSummaryItemVO {

    private String key;

    private String label;

    private Boolean configured;

    private LocalDateTime updateTime;
}
