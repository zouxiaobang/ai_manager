package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployLogEntryVO {
    private long lineNumber;
    private String timestamp;
    private String level;
    private String logger;
    private String message;
    private String raw;
}
