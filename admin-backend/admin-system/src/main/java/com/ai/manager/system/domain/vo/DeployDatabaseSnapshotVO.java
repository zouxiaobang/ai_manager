package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeployDatabaseSnapshotVO {
    private String databaseName;
    private String syncedAt;
    private long syncedAtEpochMs;
    private int tableCount;
    private List<DeployDatabaseTableVO> tables = new ArrayList<>();
}
