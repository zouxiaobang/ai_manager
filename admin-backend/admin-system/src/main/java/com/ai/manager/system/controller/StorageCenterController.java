package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageCenterOverviewVO;
import com.ai.manager.system.domain.vo.StorageCleanupResultVO;
import com.ai.manager.system.domain.vo.StorageImageItemVO;
import com.ai.manager.system.domain.vo.StorageOrphanPreviewVO;
import com.ai.manager.system.domain.vo.StorageOrphanZonePreviewVO;
import com.ai.manager.system.service.NoteContentSyncService;
import com.ai.manager.system.service.StorageCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage-center")
@RequiredArgsConstructor
public class StorageCenterController {

    private final StorageCenterService storageCenterService;
    private final NoteContentSyncService noteContentSyncService;

    @GetMapping("/overview")
    public ApiResult<StorageCenterOverviewVO> overview() {
        return ApiResult.ok(storageCenterService.getOverview());
    }

    @GetMapping("/config")
    public ApiResult<StorageCenterConfigVO> config() {
        return ApiResult.ok(storageCenterService.getConfig());
    }

    @PutMapping("/config")
    public ApiResult<StorageCenterConfigVO> saveConfig(@RequestBody StorageCenterConfigSaveRequest request) {
        return ApiResult.ok(storageCenterService.saveConfig(request));
    }

    @PostMapping("/cleanup/orphans")
    public ApiResult<StorageCleanupResultVO> cleanupOrphans(
            @RequestParam(defaultValue = "ECOMMERCE_IMAGES") String zone,
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(storageCenterService.cleanupOrphans(zone, dryRun));
    }

    @PostMapping("/cleanup/orphans/preview")
    public ApiResult<StorageOrphanPreviewVO> previewAllOrphans() {
        return ApiResult.ok(storageCenterService.previewAllOrphans());
    }

    @PostMapping("/cleanup/orphans/preview-zone")
    public ApiResult<StorageOrphanZonePreviewVO> previewOrphanZone(
            @RequestParam String zone
    ) {
        return ApiResult.ok(storageCenterService.previewOrphanZone(zone));
    }

    @PostMapping("/cleanup/orphans/execute-all")
    public ApiResult<StorageOrphanPreviewVO> cleanupAllOrphans() {
        return ApiResult.ok(storageCenterService.cleanupAllOrphans(false));
    }

    @PostMapping("/cleanup/orphans/file")
    public ApiResult<StorageCleanupResultVO> deleteOrphanFile(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        return ApiResult.ok(storageCenterService.deleteOrphanFile(zone, relativePath));
    }

    @PostMapping("/cleanup/cache")
    public ApiResult<StorageCleanupResultVO> cleanupCache(
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(storageCenterService.cleanupCache(dryRun));
    }

    @PostMapping("/sync/note-content")
    public ApiResult<Void> syncNoteContent() {
        noteContentSyncService.scheduleReconcileAll();
        return ApiResult.ok(null);
    }

    @GetMapping("/images")
    public ApiResult<PageResult<StorageImageItemVO>> browseImages(
            @RequestParam(defaultValue = "ECOMMERCE_IMAGES") String zone,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "24") Long pageSize
    ) {
        return ApiResult.ok(storageCenterService.browseProjectImages(zone, keyword, page, pageSize));
    }
}
