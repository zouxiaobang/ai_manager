package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StorageCenterConfigVO {

    private Long localQuotaMb;

    private Long ecommerceImagesQuotaMb;

    private Long notebookImagesQuotaMb;

    private Long notebookContentQuotaMb;

    private Long importFilesQuotaMb;

    private Long cacheMaxMb;

    private Long cacheTtlSeconds;

    private String overLimitStrategy;

    private String localQuotaOverLimitStrategy;

    private String ecommerceImagesOverLimitStrategy;

    private String notebookImagesOverLimitStrategy;

    private String notebookContentOverLimitStrategy;

    private String importFilesOverLimitStrategy;

    private String cacheOverLimitStrategy;

    private Boolean dualStorageEnabled;

    private LocalDateTime updateTime;

    /** 上次执行孤立文件清理的时间 */
    private LocalDateTime orphanLastCleanupAt;
}
