package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcDataRetentionSettingsVO {

    private Integer importHistoryRetentionDays = 365;

    private Integer inventoryLogRetentionDays = 180;

    private Boolean autoCleanupEnabled = false;

    private LocalDateTime updateTime;
}
