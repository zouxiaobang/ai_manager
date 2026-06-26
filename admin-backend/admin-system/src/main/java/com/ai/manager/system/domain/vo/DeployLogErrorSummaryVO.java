package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployLogErrorSummaryVO {
    private String message;
    private long count;
    private String lastSeen;
}
