package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DeployVersionVO {
    private String id;
    private String target;
    private boolean success;
    private int exitCode;
    private String deployMode;
    private long startedAt;
    private long finishedAt;
    private long durationMs;
    private String deployedAt;
    private String gitCommit;
    private String gitMessage;
    private String gitBranch;
    private String gitAuthor;
    private String projectRoot;
}
