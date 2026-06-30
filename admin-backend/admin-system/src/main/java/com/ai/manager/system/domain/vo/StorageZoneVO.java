package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class StorageZoneVO {

    private String key;

    private String label;

    private String localPath;

    private String cloudPath;

    private long usedBytes;

    private long quotaBytes;

    private long fileCount;

    private int usagePercent;

    private boolean dualStorageEnabled;

    private boolean cloudAvailable;

    /** 该分区超限丢弃策略 */
    private String overLimitStrategy;
}
