package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class DocLibraryStatsVO {

    private Long totalFiles;

    private Long totalSize;

    private Long imageCount;

    private Long documentCount;

    private Long archiveCount;

    private Long videoCount;

    private Long otherCount;

    private Long kbReadyCount;

    private Long kbProcessingCount;
}
