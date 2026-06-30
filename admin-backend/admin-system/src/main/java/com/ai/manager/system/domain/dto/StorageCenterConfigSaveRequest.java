package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class StorageCenterConfigSaveRequest {

    /** 本地总配额（MB），0 表示不限制 */
    private Long localQuotaMb;

    private Long ecommerceImagesQuotaMb;

    private Long notebookImagesQuotaMb;

    private Long notebookContentQuotaMb;

    private Long importFilesQuotaMb;

    /** Redis 正文缓存上限（MB），0 表示不限制 */
    private Long cacheMaxMb;

    /** Redis 正文缓存 TTL（秒） */
    private Long cacheTtlSeconds;

    /** 全局默认超限策略（各分区未单独配置时使用） */
    private String overLimitStrategy;

    /** 本地总配额超限策略 */
    private String localQuotaOverLimitStrategy;

    private String ecommerceImagesOverLimitStrategy;

    private String notebookImagesOverLimitStrategy;

    private String notebookContentOverLimitStrategy;

    private String importFilesOverLimitStrategy;

    /** Redis 缓存超限策略 */
    private String cacheOverLimitStrategy;

    /** 是否启用本地+云盘双写 */
    private Boolean dualStorageEnabled;
}
