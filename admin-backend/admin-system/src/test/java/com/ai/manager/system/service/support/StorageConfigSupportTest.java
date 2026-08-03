package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StorageConfigSupport 单元测试
 * 纯逻辑组件，直接调用静态方法断言默认配置、请求覆盖、校验与配额换算。
 */
class StorageConfigSupportTest {

    private StorageCenterConfigVO fullConfig() {
        StorageCenterConfigVO config = new StorageCenterConfigVO();
        config.setLocalQuotaMb(1000L);
        config.setEcommerceImagesQuotaMb(500L);
        config.setNotebookImagesQuotaMb(300L);
        config.setNotebookContentQuotaMb(200L);
        config.setImportFilesQuotaMb(400L);
        config.setCacheMaxMb(256L);
        config.setCacheTtlSeconds(3600L);
        config.setOverLimitStrategy(StorageOverLimitStrategySupport.REJECT);
        config.setDualStorageEnabled(false);
        return config;
    }

    private StorageCenterConfigSaveRequest request() {
        StorageCenterConfigSaveRequest req = new StorageCenterConfigSaveRequest();
        req.setLocalQuotaMb(2000L);
        req.setCacheTtlSeconds(120L);
        req.setOverLimitStrategy(" reject ");
        req.setDualStorageEnabled(true);
        return req;
    }

    // ==================== defaultConfig ====================

    @Test
    void defaultConfig_shouldBuildBuiltInQuotas() {
        StorageCenterConfigVO config = StorageConfigSupport.defaultConfig(512, 3600);

        assertThat(config.getLocalQuotaMb()).isEqualTo(10240L);
        assertThat(config.getEcommerceImagesQuotaMb()).isEqualTo(5120L);
        assertThat(config.getNotebookImagesQuotaMb()).isEqualTo(2048L);
        assertThat(config.getNotebookContentQuotaMb()).isEqualTo(1024L);
        assertThat(config.getImportFilesQuotaMb()).isEqualTo(2048L);
        assertThat(config.getCacheMaxMb()).isEqualTo(512L);
        assertThat(config.getCacheTtlSeconds()).isEqualTo(3600L);
        assertThat(config.getDualStorageEnabled()).isTrue();
        // 各分区超限策略默认值非空
        assertThat(config.getOverLimitStrategy()).isNotBlank();
        assertThat(config.getLocalQuotaOverLimitStrategy()).isNotBlank();
    }

    // ==================== mergeDefaults ====================

    @Test
    void mergeDefaults_shouldFillMissingFieldsFromFallback() {
        StorageCenterConfigVO fallback = StorageConfigSupport.defaultConfig(512, 3600);
        StorageCenterConfigVO value = new StorageCenterConfigVO();
        value.setLocalQuotaMb(999L);

        StorageConfigSupport.mergeDefaults(value, fallback);

        // 已有字段保留，缺失字段用默认补齐
        assertThat(value.getLocalQuotaMb()).isEqualTo(999L);
        assertThat(value.getEcommerceImagesQuotaMb()).isEqualTo(5120L);
        assertThat(value.getCacheMaxMb()).isEqualTo(512L);
        assertThat(value.getDualStorageEnabled()).isTrue();
    }

    @Test
    void mergeDefaults_shouldKeepExistingStrategyFields() {
        StorageCenterConfigVO fallback = StorageConfigSupport.defaultConfig(512, 3600);
        StorageCenterConfigVO value = new StorageCenterConfigVO();
        value.setOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_LARGEST);

        StorageConfigSupport.mergeDefaults(value, fallback);

        assertThat(value.getOverLimitStrategy()).isEqualTo(StorageOverLimitStrategySupport.CLEANUP_LARGEST);
        // 未设置的策略字段仍被默认补齐
        assertThat(value.getCacheOverLimitStrategy()).isNotBlank();
    }

    // ==================== validateConfig ====================

    @Test
    void validateConfig_shouldPassForValidRequest() {
        StorageCenterConfigSaveRequest req = request();
        req.setOverLimitStrategy(StorageOverLimitStrategySupport.REJECT);
        req.setEcommerceImagesOverLimitStrategy(StorageOverLimitStrategySupport.CLEANUP_OLDEST);
        req.setCacheMaxMb(100L);

        StorageConfigSupport.validateConfig(req); // 不抛异常即通过
    }

    @Test
    void validateConfig_shouldRejectInvalidStrategy() {
        StorageCenterConfigSaveRequest req = request();
        req.setOverLimitStrategy("UNKNOWN_STRATEGY");

        assertThatThrownBy(() -> StorageConfigSupport.validateConfig(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全局默认超限策略无效");
    }

    @Test
    void validateConfig_shouldRejectNegativeQuota() {
        StorageCenterConfigSaveRequest req = request();
        req.setLocalQuotaMb(-1L);

        assertThatThrownBy(() -> StorageConfigSupport.validateConfig(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本地总配额不能为负数");
    }

    @Test
    void validateConfig_shouldRejectCacheTtlBelow60() {
        StorageCenterConfigSaveRequest req = request();
        req.setCacheTtlSeconds(30L);

        assertThatThrownBy(() -> StorageConfigSupport.validateConfig(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缓存 TTL 不能小于 60 秒");
    }

    @Test
    void validateConfig_shouldAllowNullCacheTtl() {
        StorageCenterConfigSaveRequest req = request();
        req.setCacheTtlSeconds(null);

        StorageConfigSupport.validateConfig(req); // null 不参与 TTL 校验
    }

    // ==================== applyRequestToConfig ====================

    @Test
    void applyRequestToConfig_shouldOverwriteNonNullFields() {
        StorageCenterConfigVO current = fullConfig();
        StorageCenterConfigSaveRequest req = request();

        StorageConfigSupport.applyRequestToConfig(current, req);

        assertThat(current.getLocalQuotaMb()).isEqualTo(2000L);
        assertThat(current.getCacheTtlSeconds()).isEqualTo(120L);
        assertThat(current.getDualStorageEnabled()).isTrue();
        // 未设置的字段保持原值
        assertThat(current.getEcommerceImagesQuotaMb()).isEqualTo(500L);
        assertThat(current.getImportFilesQuotaMb()).isEqualTo(400L);
    }

    @Test
    void applyRequestToConfig_shouldTrimAndUppercaseStrategy() {
        StorageCenterConfigVO current = fullConfig();
        StorageCenterConfigSaveRequest req = request();
        req.setLocalQuotaOverLimitStrategy(" cleanup_oldest ");

        StorageConfigSupport.applyRequestToConfig(current, req);

        assertThat(current.getLocalQuotaOverLimitStrategy()).isEqualTo("CLEANUP_OLDEST");
        assertThat(current.getOverLimitStrategy()).isEqualTo("REJECT");
    }

    @Test
    void applyRequestToConfig_shouldSkipEmptyStrategy() {
        StorageCenterConfigVO current = fullConfig();
        StorageCenterConfigSaveRequest req = request();
        req.setOverLimitStrategy("   ");

        StorageConfigSupport.applyRequestToConfig(current, req);

        // 空字符串策略被忽略，保留原值
        assertThat(current.getOverLimitStrategy()).isEqualTo(StorageOverLimitStrategySupport.REJECT);
    }

    // ==================== resolveCacheMaxMb / resolveCacheTtl ====================

    @Test
    void resolveCacheMaxMb_shouldUseConfigWhenPresent() {
        StorageCenterConfigVO config = fullConfig();
        assertThat(StorageConfigSupport.resolveCacheMaxMb(config, 512)).isEqualTo(256L);
    }

    @Test
    void resolveCacheMaxMb_shouldFallbackToProperty() {
        StorageCenterConfigVO config = fullConfig();
        config.setCacheMaxMb(null);
        assertThat(StorageConfigSupport.resolveCacheMaxMb(config, 512)).isEqualTo(512L);
    }

    @Test
    void resolveCacheTtl_shouldFallbackToProperty() {
        StorageCenterConfigVO config = fullConfig();
        config.setCacheTtlSeconds(null);
        assertThat(StorageConfigSupport.resolveCacheTtl(config, 3600)).isEqualTo(3600L);
    }

    // ==================== mbToBytes / calcPercent ====================

    @Test
    void mbToBytes_shouldConvertMegabytes() {
        assertThat(StorageConfigSupport.mbToBytes(1L)).isEqualTo(1024L * 1024L);
        assertThat(StorageConfigSupport.mbToBytes(10L)).isEqualTo(10L * 1024L * 1024L);
    }

    @Test
    void mbToBytes_shouldReturnZeroForNullOrNonPositive() {
        assertThat(StorageConfigSupport.mbToBytes(null)).isZero();
        assertThat(StorageConfigSupport.mbToBytes(0L)).isZero();
        assertThat(StorageConfigSupport.mbToBytes(-5L)).isZero();
    }

    @Test
    void calcPercent_shouldRoundAndCapAt100() {
        assertThat(StorageConfigSupport.calcPercent(50, 100)).isEqualTo(50);
        assertThat(StorageConfigSupport.calcPercent(150, 100)).isEqualTo(100);
        assertThat(StorageConfigSupport.calcPercent(33, 100)).isEqualTo(33);
    }

    @Test
    void calcPercent_shouldReturnZeroForNonPositiveQuota() {
        assertThat(StorageConfigSupport.calcPercent(50, 0)).isZero();
        assertThat(StorageConfigSupport.calcPercent(50, -1)).isZero();
    }
}
