package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeployLogTailVO {
    private String logFile;
    private boolean fileExists;
    private int requestedLines;
    private int returnedLines;
    private List<DeployLogEntryVO> entries = new ArrayList<>();
}
