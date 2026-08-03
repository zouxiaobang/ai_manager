package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageZoneVO;

/**
 * 存储中心分区展示组装
 *
 * <p>从 {@code StorageCenterServiceImpl} 提取：分区 VO 组装、分区中文标签/用途说明与
 * 分区超限策略解析。不访问数据源，用量/文件数由调用方从文件系统统计后传入。</p>
 */
public final class StorageZoneViewAssembler {

    /** 存储分区 key（与 impl 常量转发引用保持一致） */
    public static final String ZONE_ECOMMERCE_IMAGES = "ECOMMERCE_IMAGES";
    public static final String ZONE_NOTEBOOK_IMAGES = "NOTEBOOK_IMAGES";
    public static final String ZONE_NOTEBOOK_CONTENT = "NOTEBOOK_CONTENT";
    public static final String ZONE_IMPORT_FILES = "IMPORT_FILES";

    private StorageZoneViewAssembler() {
    }

    /** 分区统计 → 分区 VO（纯映射，用量/文件数由调用方计算） */
    public static StorageZoneVO buildZone(
            String key,
            String label,
            String localPath,
            String cloudPath,
            long usedBytes,
            long quotaBytes,
            long fileCount,
            boolean dualStorageEnabled,
            boolean cloudAvailable,
            String overLimitStrategy
    ) {
        StorageZoneVO zone = new StorageZoneVO();
        zone.setKey(key);
        zone.setLabel(label);
        zone.setLocalPath(localPath);
        zone.setCloudPath(cloudPath);
        zone.setUsedBytes(usedBytes);
        zone.setQuotaBytes(quotaBytes);
        zone.setFileCount(fileCount);
        zone.setUsagePercent(StorageConfigSupport.calcPercent(usedBytes, quotaBytes));
        zone.setDualStorageEnabled(dualStorageEnabled);
        zone.setCloudAvailable(cloudAvailable);
        zone.setOverLimitStrategy(overLimitStrategy);
        return zone;
    }

    /** 分区 key → 中文标签（概览/孤儿预览展示） */
    public static String resolveZoneLabel(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES -> "电商图片";
            case ZONE_NOTEBOOK_IMAGES -> "笔记图片";
            case ZONE_NOTEBOOK_CONTENT -> "笔记正文";
            case ZONE_IMPORT_FILES -> "销售订单导入";
            default -> zoneKey;
        };
    }

    /** 分区 key → 中文用途说明（孤儿清理文案） */
    public static String resolveZonePurpose(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES ->
                    "商品、SKU、店铺、平台、快递站点与纸箱等业务上传图片；未被任何业务记录引用的文件视为孤立。";
            case ZONE_NOTEBOOK_IMAGES ->
                    "笔记编辑器中上传的图片资源；未嵌入任何笔记正文 HTML 的文件视为孤立。";
            case ZONE_NOTEBOOK_CONTENT ->
                    "笔记正文 HTML 与版本元数据；无对应有效笔记 ID 的文件视为孤立。";
            case ZONE_IMPORT_FILES ->
                    "销售订单 Excel 导入原件；无导入批次（sys_import_batch）记录的文件视为孤立。";
            default -> "";
        };
    }

    /** 分区超限策略：分区专用配置优先，缺失回退全局默认并归一化 */
    public static String resolveOverLimitStrategy(StorageCenterConfigVO config, String zoneKey) {
        String specific = switch (zoneKey) {
            case StorageOverLimitStrategySupport.ZONE_LOCAL_TOTAL -> config.getLocalQuotaOverLimitStrategy();
            case ZONE_ECOMMERCE_IMAGES -> config.getEcommerceImagesOverLimitStrategy();
            case ZONE_NOTEBOOK_IMAGES -> config.getNotebookImagesOverLimitStrategy();
            case ZONE_NOTEBOOK_CONTENT -> config.getNotebookContentOverLimitStrategy();
            case ZONE_IMPORT_FILES -> config.getImportFilesOverLimitStrategy();
            case StorageOverLimitStrategySupport.ZONE_REDIS_CACHE -> config.getCacheOverLimitStrategy();
            default -> null;
        };
        return StorageOverLimitStrategySupport.normalize(specific, config.getOverLimitStrategy());
    }
}
