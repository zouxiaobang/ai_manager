package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class StorageCenterOverviewVO {

    private long totalLocalUsedBytes;

    private long totalLocalQuotaBytes;

    private int totalLocalUsagePercent;

    private long cacheUsedBytes;

    private long cacheMaxBytes;

    private long cacheTtlSeconds;

    private boolean baiduPanAuthorized;

    private String baiduPanAuthorizeUrl;

    /** 当前环境百度网盘根目录，如 /apps/ai_blog 或 /apps/ai_blog/dev */
    private String baiduPanCloudRoot;

    private boolean dualStorageEnabled;

    private List<StorageZoneVO> zones;
}
