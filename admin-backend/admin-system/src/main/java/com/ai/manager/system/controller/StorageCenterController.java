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

/**
 * 存储中心控制器
 *
 * <p>所属模块：存储中心模块</p>
 * <p>API路径前缀：/api/storage-center</p>
 * <p>功能描述：提供存储概览、配置管理、孤儿文件清理、缓存清理、笔记内容同步、图片浏览等存储管理功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/storage-center")
@RequiredArgsConstructor
public class StorageCenterController {

    private final StorageCenterService storageCenterService;
    private final NoteContentSyncService noteContentSyncService;

    /**
     * 获取存储概览
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/storage-center/overview</p>
     *
     * @return 存储概览信息
     */
    @GetMapping("/overview")
    public ApiResult<StorageCenterOverviewVO> overview() {
        return ApiResult.ok(storageCenterService.getOverview());
    }

    /**
     * 获取存储配置
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/storage-center/config</p>
     *
     * @return 存储配置信息
     */
    @GetMapping("/config")
    public ApiResult<StorageCenterConfigVO> config() {
        return ApiResult.ok(storageCenterService.getConfig());
    }

    /**
     * 保存存储配置
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/storage-center/config</p>
     *
     * @param request 存储配置保存请求参数
     * @return 保存后的存储配置
     */
    @PutMapping("/config")
    public ApiResult<StorageCenterConfigVO> saveConfig(@RequestBody StorageCenterConfigSaveRequest request) {
        return ApiResult.ok(storageCenterService.saveConfig(request));
    }

    /**
     * 清理孤儿文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/orphans</p>
     *
     * @param zone 存储区域
     * @param dryRun 是否试运行
     * @return 清理结果
     */
    @PostMapping("/cleanup/orphans")
    public ApiResult<StorageCleanupResultVO> cleanupOrphans(
            @RequestParam(defaultValue = "ECOMMERCE_IMAGES") String zone,
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(storageCenterService.cleanupOrphans(zone, dryRun));
    }

    /**
     * 预览所有孤儿文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/orphans/preview</p>
     *
     * @return 孤儿文件预览结果
     */
    @PostMapping("/cleanup/orphans/preview")
    public ApiResult<StorageOrphanPreviewVO> previewAllOrphans() {
        return ApiResult.ok(storageCenterService.previewAllOrphans());
    }

    /**
     * 预览指定区域的孤儿文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/orphans/preview-zone</p>
     *
     * @param zone 存储区域
     * @return 指定区域的孤儿文件预览结果
     */
    @PostMapping("/cleanup/orphans/preview-zone")
    public ApiResult<StorageOrphanZonePreviewVO> previewOrphanZone(
            @RequestParam String zone
    ) {
        return ApiResult.ok(storageCenterService.previewOrphanZone(zone));
    }

    /**
     * 清理所有孤儿文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/orphans/execute-all</p>
     *
     * @return 清理结果预览
     */
    @PostMapping("/cleanup/orphans/execute-all")
    public ApiResult<StorageOrphanPreviewVO> cleanupAllOrphans() {
        return ApiResult.ok(storageCenterService.cleanupAllOrphans(false));
    }

    /**
     * 删除单个孤儿文件
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/orphans/file</p>
     *
     * @param zone 存储区域
     * @param relativePath 相对路径
     * @return 清理结果
     */
    @PostMapping("/cleanup/orphans/file")
    public ApiResult<StorageCleanupResultVO> deleteOrphanFile(
            @RequestParam String zone,
            @RequestParam String relativePath
    ) {
        return ApiResult.ok(storageCenterService.deleteOrphanFile(zone, relativePath));
    }

    /**
     * 清理缓存
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/cleanup/cache</p>
     *
     * @param dryRun 是否试运行
     * @return 清理结果
     */
    @PostMapping("/cleanup/cache")
    public ApiResult<StorageCleanupResultVO> cleanupCache(
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return ApiResult.ok(storageCenterService.cleanupCache(dryRun));
    }

    /**
     * 同步笔记内容
     *
     * <p>HTTP方法：POST</p>
     * <p>路径：/api/storage-center/sync/note-content</p>
     *
     * @return 操作结果
     */
    @PostMapping("/sync/note-content")
    public ApiResult<Void> syncNoteContent() {
        noteContentSyncService.scheduleReconcileAll();
        return ApiResult.ok(null);
    }

    /**
     * 浏览图片
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/storage-center/images</p>
     *
     * @param zone 存储区域
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页条数
     * @return 图片分页结果
     */
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
