package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StorageOrphanPreviewVO {

    private boolean dryRun;

    private int totalScanned;

    private int totalOrphanCount;

    private long totalFreedBytes;

    private LocalDateTime lastOrphanCleanupAt;

    private List<StorageOrphanZonePreviewVO> zones;
}
