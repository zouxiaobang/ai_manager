package com.ai.manager.system.service.support;

import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageZoneVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StorageZoneViewAssembler 单元测试
 * 纯展示组装，断言分区 VO 映射、标签/用途解析与超限策略回退。
 */
class StorageZoneViewAssemblerTest {

    @Test
    void buildZone_shouldMapAllFieldsAndCalcPercent() {
        StorageZoneVO zone = StorageZoneViewAssembler.buildZone(
                "ECOMMERCE_IMAGES", "电商图片", "/data/img", "/cloud/img",
                50L, 1024L * 1024L, 10L, true, true, "REJECT");

        assertThat(zone.getKey()).isEqualTo("ECOMMERCE_IMAGES");
        assertThat(zone.getLabel()).isEqualTo("电商图片");
        assertThat(zone.getLocalPath()).isEqualTo("/data/img");
        assertThat(zone.getCloudPath()).isEqualTo("/cloud/img");
        assertThat(zone.getUsedBytes()).isEqualTo(50L);
        assertThat(zone.getQuotaBytes()).isEqualTo(1024L * 1024L);
        assertThat(zone.getFileCount()).isEqualTo(10L);
        assertThat(zone.getUsagePercent()).isZero();
        assertThat(zone.isDualStorageEnabled()).isTrue();
        assertThat(zone.isCloudAvailable()).isTrue();
        assertThat(zone.getOverLimitStrategy()).isEqualTo("REJECT");
    }

    @Test
    void buildZone_shouldCapUsagePercentAt100() {
        StorageZoneVO zone = StorageZoneViewAssembler.buildZone(
                "K", "L", "/a", "/b", 200L, 100L, 0L, false, false, "REJECT");

        assertThat(zone.getUsagePercent()).isEqualTo(100);
        assertThat(zone.isDualStorageEnabled()).isFalse();
    }

    @Test
    void resolveZoneLabel_shouldMapKnownZones() {
        assertThat(StorageZoneViewAssembler.resolveZoneLabel("ECOMMERCE_IMAGES")).isEqualTo("电商图片");
        assertThat(StorageZoneViewAssembler.resolveZoneLabel("NOTEBOOK_IMAGES")).isEqualTo("笔记图片");
        assertThat(StorageZoneViewAssembler.resolveZoneLabel("NOTEBOOK_CONTENT")).isEqualTo("笔记正文");
        assertThat(StorageZoneViewAssembler.resolveZoneLabel("IMPORT_FILES")).isEqualTo("销售订单导入");
    }

    @Test
    void resolveZoneLabel_shouldReturnKeyForUnknownZone() {
        assertThat(StorageZoneViewAssembler.resolveZoneLabel("UNKNOWN")).isEqualTo("UNKNOWN");
    }

    @Test
    void resolveZonePurpose_shouldReturnNonEmptyForKnownZones() {
        assertThat(StorageZoneViewAssembler.resolveZonePurpose("ECOMMERCE_IMAGES")).contains("孤立");
        assertThat(StorageZoneViewAssembler.resolveZonePurpose("IMPORT_FILES")).contains("导入批次");
    }

    @Test
    void resolveZonePurpose_shouldReturnEmptyForUnknownZone() {
        assertThat(StorageZoneViewAssembler.resolveZonePurpose("UNKNOWN")).isEmpty();
    }

    @Test
    void resolveOverLimitStrategy_shouldPreferZoneSpecific() {
        StorageCenterConfigVO config = new StorageCenterConfigVO();
        config.setEcommerceImagesOverLimitStrategy("CLEANUP_LARGEST");
        config.setOverLimitStrategy("REJECT");

        assertThat(StorageZoneViewAssembler.resolveOverLimitStrategy(config, "ECOMMERCE_IMAGES"))
                .isEqualTo("CLEANUP_LARGEST");
    }

    @Test
    void resolveOverLimitStrategy_shouldFallbackToGlobalWhenZoneNotSet() {
        StorageCenterConfigVO config = new StorageCenterConfigVO();
        config.setOverLimitStrategy("cleanup_oldest");

        assertThat(StorageZoneViewAssembler.resolveOverLimitStrategy(config, "ECOMMERCE_IMAGES"))
                .isEqualTo("CLEANUP_OLDEST");
    }

    @Test
    void resolveOverLimitStrategy_shouldDefaultRejectWhenNothingSet() {
        assertThat(StorageZoneViewAssembler.resolveOverLimitStrategy(new StorageCenterConfigVO(), "UNKNOWN"))
                .isEqualTo("REJECT");
    }
}
