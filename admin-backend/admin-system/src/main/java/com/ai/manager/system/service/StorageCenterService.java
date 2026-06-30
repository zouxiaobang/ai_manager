package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageCenterOverviewVO;
import com.ai.manager.system.domain.vo.StorageCleanupResultVO;
import com.ai.manager.system.domain.vo.StorageImageItemVO;
import com.ai.manager.system.domain.vo.StorageOrphanPreviewVO;
import com.ai.manager.system.domain.vo.StorageOrphanZonePreviewVO;

public interface StorageCenterService {

    StorageCenterOverviewVO getOverview();

    StorageCenterConfigVO getConfig();

    StorageCenterConfigVO saveConfig(StorageCenterConfigSaveRequest request);

    StorageCleanupResultVO cleanupOrphans(String zone, boolean dryRun);

    StorageOrphanPreviewVO previewAllOrphans();

    StorageOrphanPreviewVO cleanupAllOrphans(boolean dryRun);

    StorageOrphanZonePreviewVO previewOrphanZone(String zone);

    StorageCleanupResultVO deleteOrphanFile(String zone, String relativePath);

    StorageCleanupResultVO cleanupCache(boolean dryRun);

    boolean isDualStorageEnabled();

    String resolveOverLimitStrategy(String zoneKey);

    void enforceCacheLimit(long incomingBytes);

    void assertWritable(String zoneKey, long additionalBytes);

    void onFileWritten(String zoneKey, long bytesWritten);

    PageResult<StorageImageItemVO> browseProjectImages(
            String zone,
            String keyword,
            Long page,
            Long pageSize
    );
}
