package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import org.springframework.util.StringUtils;

/**
 * 存储中心配置纯逻辑
 *
 * <p>从 {@code StorageCenterServiceImpl} 提取：默认配置构建、保存请求覆盖、默认值合并、
 * 参数校验与配额换算。不访问任何数据源，参数全部传入，便于独立单元测试。</p>
 */
public final class StorageConfigSupport {

    private StorageConfigSupport() {
    }

    /** 内置默认配置（缓存上限/TTL 由配置属性提供） */
    public static StorageCenterConfigVO defaultConfig(long cacheMaxMb, long cacheTtlSeconds) {
        StorageCenterConfigVO config = new StorageCenterConfigVO();
        config.setLocalQuotaMb(10240L);
        config.setEcommerceImagesQuotaMb(5120L);
        config.setNotebookImagesQuotaMb(2048L);
        config.setNotebookContentQuotaMb(1024L);
        config.setImportFilesQuotaMb(2048L);
        config.setCacheMaxMb(cacheMaxMb);
        config.setCacheTtlSeconds(cacheTtlSeconds);
        config.setOverLimitStrategy(StorageOverLimitStrategySupport.REJECT);
        config.setLocalQuotaOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_OLDEST);
        config.setEcommerceImagesOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_LARGEST);
        config.setNotebookImagesOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_OLDEST);
        config.setNotebookContentOverLimitStrategy(StorageOverLimitStrategySupport.REJECT);
        config.setImportFilesOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_OLDEST);
        config.setCacheOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_LARGEST);
        config.setDualStorageEnabled(true);
        return config;
    }

    /** 用默认配置补齐存量配置的缺失字段（就地修改并返回） */
    public static StorageCenterConfigVO mergeDefaults(StorageCenterConfigVO value, StorageCenterConfigVO fallback) {
        if (value.getLocalQuotaMb() == null) {
            value.setLocalQuotaMb(fallback.getLocalQuotaMb());
        }
        if (value.getEcommerceImagesQuotaMb() == null) {
            value.setEcommerceImagesQuotaMb(fallback.getEcommerceImagesQuotaMb());
        }
        if (value.getNotebookImagesQuotaMb() == null) {
            value.setNotebookImagesQuotaMb(fallback.getNotebookImagesQuotaMb());
        }
        if (value.getNotebookContentQuotaMb() == null) {
            value.setNotebookContentQuotaMb(fallback.getNotebookContentQuotaMb());
        }
        if (value.getImportFilesQuotaMb() == null) {
            value.setImportFilesQuotaMb(fallback.getImportFilesQuotaMb());
        }
        if (value.getCacheMaxMb() == null) {
            value.setCacheMaxMb(fallback.getCacheMaxMb());
        }
        if (value.getCacheTtlSeconds() == null) {
            value.setCacheTtlSeconds(fallback.getCacheTtlSeconds());
        }
        if (!StringUtils.hasText(value.getOverLimitStrategy())) {
            value.setOverLimitStrategy(fallback.getOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getLocalQuotaOverLimitStrategy())) {
            value.setLocalQuotaOverLimitStrategy(fallback.getLocalQuotaOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getEcommerceImagesOverLimitStrategy())) {
            value.setEcommerceImagesOverLimitStrategy(fallback.getEcommerceImagesOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getNotebookImagesOverLimitStrategy())) {
            value.setNotebookImagesOverLimitStrategy(fallback.getNotebookImagesOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getNotebookContentOverLimitStrategy())) {
            value.setNotebookContentOverLimitStrategy(fallback.getNotebookContentOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getImportFilesOverLimitStrategy())) {
            value.setImportFilesOverLimitStrategy(fallback.getImportFilesOverLimitStrategy());
        }
        if (!StringUtils.hasText(value.getCacheOverLimitStrategy())) {
            value.setCacheOverLimitStrategy(fallback.getCacheOverLimitStrategy());
        }
        if (value.getDualStorageEnabled() == null) {
            value.setDualStorageEnabled(fallback.getDualStorageEnabled());
        }
        return value;
    }

    /** 校验保存请求：各超限策略合法、配额非负、缓存 TTL 不小于 60 秒 */
    public static void validateConfig(StorageCenterConfigSaveRequest request) {
        validateStrategy(request.getOverLimitStrategy(), "全局默认超限策略");
        validateStrategy(request.getLocalQuotaOverLimitStrategy(), "本地总配额超限策略");
        validateStrategy(request.getEcommerceImagesOverLimitStrategy(), "电商图片超限策略");
        validateStrategy(request.getNotebookImagesOverLimitStrategy(), "笔记图片超限策略");
        validateStrategy(request.getNotebookContentOverLimitStrategy(), "笔记正文超限策略");
        validateStrategy(request.getImportFilesOverLimitStrategy(), "导入文件超限策略");
        validateStrategy(request.getCacheOverLimitStrategy(), "Redis 缓存超限策略");
        validatePositive(request.getLocalQuotaMb(), "本地总配额");
        validatePositive(request.getEcommerceImagesQuotaMb(), "电商图片配额");
        validatePositive(request.getNotebookImagesQuotaMb(), "笔记图片配额");
        validatePositive(request.getNotebookContentQuotaMb(), "笔记正文配额");
        validatePositive(request.getImportFilesQuotaMb(), "导入文件配额");
        validatePositive(request.getCacheMaxMb(), "缓存上限");
        if (request.getCacheTtlSeconds() != null && request.getCacheTtlSeconds() < 60) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缓存 TTL 不能小于 60 秒");
        }
    }

    /** 将保存请求的非空字段覆盖到当前配置（策略字段统一大写），供 saveConfig 组装最终配置 */
    public static void applyRequestToConfig(StorageCenterConfigVO current, StorageCenterConfigSaveRequest request) {
        if (request.getLocalQuotaMb() != null) {
            current.setLocalQuotaMb(request.getLocalQuotaMb());
        }
        if (request.getEcommerceImagesQuotaMb() != null) {
            current.setEcommerceImagesQuotaMb(request.getEcommerceImagesQuotaMb());
        }
        if (request.getNotebookImagesQuotaMb() != null) {
            current.setNotebookImagesQuotaMb(request.getNotebookImagesQuotaMb());
        }
        if (request.getNotebookContentQuotaMb() != null) {
            current.setNotebookContentQuotaMb(request.getNotebookContentQuotaMb());
        }
        if (request.getImportFilesQuotaMb() != null) {
            current.setImportFilesQuotaMb(request.getImportFilesQuotaMb());
        }
        if (request.getCacheMaxMb() != null) {
            current.setCacheMaxMb(request.getCacheMaxMb());
        }
        if (request.getCacheTtlSeconds() != null) {
            current.setCacheTtlSeconds(request.getCacheTtlSeconds());
        }
        if (StringUtils.hasText(request.getOverLimitStrategy())) {
            current.setOverLimitStrategy(request.getOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getLocalQuotaOverLimitStrategy())) {
            current.setLocalQuotaOverLimitStrategy(request.getLocalQuotaOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getEcommerceImagesOverLimitStrategy())) {
            current.setEcommerceImagesOverLimitStrategy(request.getEcommerceImagesOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getNotebookImagesOverLimitStrategy())) {
            current.setNotebookImagesOverLimitStrategy(request.getNotebookImagesOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getNotebookContentOverLimitStrategy())) {
            current.setNotebookContentOverLimitStrategy(request.getNotebookContentOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getImportFilesOverLimitStrategy())) {
            current.setImportFilesOverLimitStrategy(request.getImportFilesOverLimitStrategy().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getCacheOverLimitStrategy())) {
            current.setCacheOverLimitStrategy(request.getCacheOverLimitStrategy().trim().toUpperCase());
        }
        if (request.getDualStorageEnabled() != null) {
            current.setDualStorageEnabled(request.getDualStorageEnabled());
        }
    }

    /** 缓存上限：配置未显式设置时回退到属性默认值 */
    public static long resolveCacheMaxMb(StorageCenterConfigVO config, long fallbackCacheMaxMb) {
        if (config.getCacheMaxMb() != null) {
            return config.getCacheMaxMb();
        }
        return fallbackCacheMaxMb;
    }

    /** 缓存 TTL：配置未显式设置时回退到属性默认值 */
    public static long resolveCacheTtl(StorageCenterConfigVO config, long fallbackCacheTtlSeconds) {
        if (config.getCacheTtlSeconds() != null) {
            return config.getCacheTtlSeconds();
        }
        return fallbackCacheTtlSeconds;
    }

    /** 配额 MB → 字节；空值或非正返回 0 */
    public static long mbToBytes(Long mb) {
        if (mb == null || mb <= 0) {
            return 0L;
        }
        return mb * 1024L * 1024L;
    }

    /** 使用量占比（0-100），配额非正返回 0 */
    public static int calcPercent(long used, long quota) {
        if (quota <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(used * 100.0 / quota));
    }

    private static void validatePositive(Long value, String label) {
        if (value != null && value < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "不能为负数");
        }
    }

    private static void validateStrategy(String strategy, String label) {
        if (strategy != null && !StorageOverLimitStrategySupport.isValid(strategy)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "无效");
        }
    }
}
