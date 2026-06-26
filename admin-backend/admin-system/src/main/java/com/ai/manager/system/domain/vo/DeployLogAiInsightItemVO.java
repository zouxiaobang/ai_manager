package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployLogAiInsightItemVO {
    /** error | warn | info | success */
    private String severity;
    private String text;
}
