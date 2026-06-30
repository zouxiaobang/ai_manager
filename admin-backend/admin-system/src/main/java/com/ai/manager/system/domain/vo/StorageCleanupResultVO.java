package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class StorageCleanupResultVO {

    private String zone;

    private boolean dryRun;

    private int scannedCount;

    private int removedCount;

    private long freedBytes;

    private List<String> sampleFiles;
}
