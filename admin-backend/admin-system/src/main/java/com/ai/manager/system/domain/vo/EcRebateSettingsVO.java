package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EcRebateSettingsVO {

    private BigDecimal defaultRebatePct = BigDecimal.ZERO;

    private LocalDateTime updateTime;
}
