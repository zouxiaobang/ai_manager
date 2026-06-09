package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class EcCartonBackfillTaskVO {

    private String taskId;

    /** PENDING | RUNNING | COMPLETED | FAILED */
    private String status;

    private int total;

    private int processed;

    private int updated;

    private String message;
}
