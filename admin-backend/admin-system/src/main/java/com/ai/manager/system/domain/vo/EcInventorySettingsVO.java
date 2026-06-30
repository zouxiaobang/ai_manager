package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class EcInventorySettingsVO {

    private Integer defaultAlertThreshold = 10;

    private Integer slowMovingDays = 45;

    private Integer slowMovingFallbackDays = 90;

    private LocalDateTime updateTime;
}
