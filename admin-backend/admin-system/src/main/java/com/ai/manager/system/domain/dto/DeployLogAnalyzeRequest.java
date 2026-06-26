package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class DeployLogAnalyzeRequest {
    private Integer lines;
    private String question;
}
