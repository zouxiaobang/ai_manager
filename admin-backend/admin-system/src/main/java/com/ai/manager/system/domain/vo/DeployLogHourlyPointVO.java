package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployLogHourlyPointVO {
    private String hour;
    private long total;
    private long errorCount;
    private long warnCount;
}
