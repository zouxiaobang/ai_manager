package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.entity.EcSystemConfig;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.entity.SysImportBatch;
import com.ai.manager.system.domain.vo.BaiduPanAuthStatusVO;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageCenterOverviewVO;
import com.ai.manager.system.domain.vo.StorageCleanupResultVO;
import com.ai.manager.system.domain.vo.StorageImageItemVO;
import com.ai.manager.system.domain.vo.StorageOrphanFileItemVO;
import com.ai.manager.system.domain.vo.StorageOrphanPreviewVO;
import com.ai.manager.system.domain.vo.StorageOrphanZonePreviewVO;
import com.ai.manager.system.domain.vo.StorageZoneVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.mapper.EcSystemConfigMapper;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.mapper.SysImportBatchMapper;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.StorageCenterService;
import com.ai.manager.system.service.support.StorageOverLimitStrategySupport;
import com.ai.manager.system.service.support.StoragePathSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCenterServiceImpl implements StorageCenterService {

    public static final String ZONE_ECOMMERCE_IMAGES = "ECOMMERCE_IMAGES";
    public static final String ZONE_NOTEBOOK_IMAGES = "NOTEBOOK_IMAGES";
    public static final String ZONE_NOTEBOOK_CONTENT = "NOTEBOOK_CONTENT";
    public static final String ZONE_IMPORT_FILES = "IMPORT_FILES";

    private static final Set<String> BROWSE_IMAGE_ZONES = Set.of(ZONE_ECOMMERCE_IMAGES, ZONE_NOTEBOOK_IMAGES);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private static final String CONFIG_KEY = "storage_center";
    private static final String CACHE_PREFIX = "nb:content:";
    private static final int ORPHAN_FILE_LIST_LIMIT = 100;
    private static final Pattern NOTEBOOK_IMAGE_REF_PATTERN = Pattern.compile(
            "notebook/images[/\\\\]([^\"'\\s>?#]+)", Pattern.CASE_INSENSITIVE);

    private final EcSystemConfigMapper ecSystemConfigMapper;
    private final ObjectMapper objectMapper;
    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanProperties baiduPanProperties;
    private final NoteStorageProperties noteStorageProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final EcProductMapper ecProductMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcShopMapper ecShopMapper;
    private final EcExpressStationMapper ecExpressStationMapper;
    private final NbNoteMapper nbNoteMapper;
    private final SysImportBatchMapper sysImportBatchMapper;

    @Value("${ai-manager.upload.ecommerce-path:uploads/ecommerce}")
    private String ecommerceUploadPath;

    @Value("${ai-manager.upload.notebook-images-path:uploads/notebook/images}")
    private String notebookImagesPath;

    @Override
    public StorageCenterOverviewVO getOverview() {
        StorageCenterConfigVO config = getConfig();
        BaiduPanAuthStatusVO panStatus = baiduPanAuthService.getStatus();
        boolean cloudAvailable = panStatus.isAuthorized();

        Path ecommerceBase = StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath);
        Path ecommerceImagesDir = ecommerceBase;
        Path importDir = ecommerceBase.resolve("imports").resolve("sales-orders");
        Path notebookImagesDir = StoragePathSupport.resolveUploadBasePath(notebookImagesPath);
        Path notebookContentDir = StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot());

        List<StorageZoneVO> zones = new ArrayList<>();
        zones.add(buildZone(
                config,
                ZONE_ECOMMERCE_IMAGES,
                "电商图片",
                ecommerceImagesDir,
                baiduPanProperties.ecommerceImagesDir(),
                config.getEcommerceImagesQuotaMb(),
                cloudAvailable,
                config.getDualStorageEnabled(),
                true
        ));
        zones.add(buildZone(
                config,
                ZONE_NOTEBOOK_IMAGES,
                "笔记图片",
                notebookImagesDir,
                baiduPanProperties.imagesDir(),
                config.getNotebookImagesQuotaMb(),
                cloudAvailable,
                config.getDualStorageEnabled(),
                false
        ));
        zones.add(buildZone(
                config,
                ZONE_NOTEBOOK_CONTENT,
                "笔记正文",
                notebookContentDir,
                baiduPanProperties.notesDir(),
                config.getNotebookContentQuotaMb(),
                cloudAvailable,
                config.getDualStorageEnabled(),
                false
        ));
        zones.add(buildZone(
                config,
                ZONE_IMPORT_FILES,
                "销售订单导入",
                importDir,
                baiduPanProperties.salesOrderImportsDir(),
                config.getImportFilesQuotaMb(),
                cloudAvailable,
                config.getDualStorageEnabled(),
                false
        ));

        long totalUsed = zones.stream().mapToLong(StorageZoneVO::getUsedBytes).sum();
        long totalQuota = mbToBytes(config.getLocalQuotaMb());
        long cacheUsed = measureCacheBytes();
        long cacheMax = mbToBytes(resolveCacheMaxMb(config));

        StorageCenterOverviewVO overview = new StorageCenterOverviewVO();
        overview.setTotalLocalUsedBytes(totalUsed);
        overview.setTotalLocalQuotaBytes(totalQuota);
        overview.setTotalLocalUsagePercent(calcPercent(totalUsed, totalQuota));
        overview.setCacheUsedBytes(cacheUsed);
        overview.setCacheMaxBytes(cacheMax);
        overview.setCacheTtlSeconds(resolveCacheTtl(config));
        overview.setBaiduPanAuthorized(cloudAvailable);
        overview.setBaiduPanAuthorizeUrl(panStatus.getAuthorizeUrl());
        overview.setBaiduPanCloudRoot(baiduPanProperties.rootPath());
        overview.setDualStorageEnabled(Boolean.TRUE.equals(config.getDualStorageEnabled()));
        overview.setZones(zones);
        return overview;
    }

    @Override
    public StorageCenterConfigVO getConfig() {
        StorageCenterConfigVO fallback = defaultConfig();
        EcSystemConfig row = ecSystemConfigMapper.selectById(CONFIG_KEY);
        if (row == null || !StringUtils.hasText(row.getConfigJson())) {
            fallback.setUpdateTime(row != null ? row.getUpdateTime() : null);
            return fallback;
        }
        try {
            StorageCenterConfigVO value = objectMapper.readValue(row.getConfigJson(), StorageCenterConfigVO.class);
            value.setUpdateTime(row.getUpdateTime());
            return mergeDefaults(value, fallback);
        } catch (JsonProcessingException ex) {
            StorageCenterConfigVO value = fallback;
            value.setUpdateTime(row.getUpdateTime());
            return value;
        }
    }

    @Override
    public StorageCenterConfigVO saveConfig(StorageCenterConfigSaveRequest request) {
        validateConfig(request);
        StorageCenterConfigVO current = getConfig();
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

        EcSystemConfig row = ecSystemConfigMapper.selectById(CONFIG_KEY);
        if (row == null) {
            row = new EcSystemConfig();
            row.setConfigKey(CONFIG_KEY);
        }
        try {
            row.setConfigJson(objectMapper.writeValueAsString(current));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置序列化失败");
        }
        row.setUpdateTime(LocalDateTime.now());
        if (ecSystemConfigMapper.selectById(CONFIG_KEY) == null) {
            ecSystemConfigMapper.insert(row);
        } else {
            ecSystemConfigMapper.updateById(row);
        }
        return getConfig();
    }

    @Override
    public StorageCleanupResultVO cleanupOrphans(String zone, boolean dryRun) {
        String zoneKey = StringUtils.hasText(zone) ? zone.trim().toUpperCase() : ZONE_ECOMMERCE_IMAGES;
        StorageOrphanZonePreviewVO preview = scanZoneOrphans(zoneKey, dryRun, ORPHAN_FILE_LIST_LIMIT);
        if (!preview.isSupported()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的分区: " + zoneKey);
        }
        StorageCleanupResultVO result = new StorageCleanupResultVO();
        result.setZone(zoneKey);
        result.setDryRun(dryRun);
        result.setScannedCount(preview.getScannedCount());
        result.setRemovedCount(preview.getOrphanCount());
        result.setFreedBytes(preview.getFreedBytes());
        result.setSampleFiles(preview.getFiles().stream().map(StorageOrphanFileItemVO::getFileName).toList());
        if (!dryRun) {
            persistOrphanLastCleanupAt(LocalDateTime.now());
        }
        return result;
    }

    @Override
    public StorageOrphanZonePreviewVO previewOrphanZone(String zone) {
        String zoneKey = StringUtils.hasText(zone) ? zone.trim().toUpperCase() : ZONE_ECOMMERCE_IMAGES;
        StorageOrphanZonePreviewVO preview = scanZoneOrphans(zoneKey, true, 500);
        if (!preview.isSupported()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的分区: " + zoneKey);
        }
        return preview;
    }

    @Override
    public StorageCleanupResultVO deleteOrphanFile(String zone, String relativePath) {
        String zoneKey = StringUtils.hasText(zone) ? zone.trim().toUpperCase() : ZONE_ECOMMERCE_IMAGES;
        if (!isOrphanCleanupSupported(zoneKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的分区: " + zoneKey);
        }
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件路径无效");
        }

        Path dir = resolveZoneDir(zoneKey);
        if (dir == null || !Files.isDirectory(dir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分区目录不存在");
        }

        Path file = dir.resolve(relativePath.trim().replace('\\', '/')).normalize();
        Path normalizedDir = dir.normalize();
        if (!file.startsWith(normalizedDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件路径");
        }
        if (ZONE_ECOMMERCE_IMAGES.equals(zoneKey)
                && file.toString().contains(normalizedDir.resolve("imports").toString())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持删除该路径文件");
        }
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件不存在或不是普通文件");
        }

        String name = file.getFileName().toString();
        Set<String> referenced = collectReferences(zoneKey);
        if (referenced.contains(name)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件已被业务引用，无法删除");
        }

        long size;
        try {
            size = Files.size(file);
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "删除文件失败: " + ex.getMessage());
        }

        persistOrphanLastCleanupAt(LocalDateTime.now());

        StorageCleanupResultVO result = new StorageCleanupResultVO();
        result.setZone(zoneKey);
        result.setDryRun(false);
        result.setScannedCount(1);
        result.setRemovedCount(1);
        result.setFreedBytes(size);
        result.setSampleFiles(List.of(name));
        return result;
    }

    @Override
    public StorageOrphanPreviewVO previewAllOrphans() {
        return buildOrphanPreview(true);
    }

    @Override
    public StorageOrphanPreviewVO cleanupAllOrphans(boolean dryRun) {
        return buildOrphanPreview(dryRun);
    }

    private StorageOrphanPreviewVO buildOrphanPreview(boolean dryRun) {
        List<String> zoneKeys = List.of(
                ZONE_ECOMMERCE_IMAGES,
                ZONE_NOTEBOOK_IMAGES,
                ZONE_NOTEBOOK_CONTENT,
                ZONE_IMPORT_FILES
        );
        StorageOrphanPreviewVO preview = new StorageOrphanPreviewVO();
        preview.setDryRun(dryRun);
        List<StorageOrphanZonePreviewVO> zones = new ArrayList<>();
        int totalScanned = 0;
        int totalOrphans = 0;
        long totalFreed = 0L;
        for (String zoneKey : zoneKeys) {
            StorageOrphanZonePreviewVO zonePreview = scanZoneOrphans(zoneKey, dryRun, ORPHAN_FILE_LIST_LIMIT);
            zones.add(zonePreview);
            if (zonePreview.isSupported()) {
                totalScanned += zonePreview.getScannedCount();
                totalOrphans += zonePreview.getOrphanCount();
                totalFreed += zonePreview.getFreedBytes();
            }
        }
        preview.setZones(zones);
        preview.setTotalScanned(totalScanned);
        preview.setTotalOrphanCount(totalOrphans);
        preview.setTotalFreedBytes(totalFreed);
        StorageCenterConfigVO config = getConfig();
        preview.setLastOrphanCleanupAt(config.getOrphanLastCleanupAt());
        if (!dryRun) {
            persistOrphanLastCleanupAt(LocalDateTime.now());
            preview.setLastOrphanCleanupAt(LocalDateTime.now());
        }
        return preview;
    }

    private void persistOrphanLastCleanupAt(LocalDateTime cleanupAt) {
        StorageCenterConfigVO config = getConfig();
        config.setOrphanLastCleanupAt(cleanupAt);
        EcSystemConfig row = ecSystemConfigMapper.selectById(CONFIG_KEY);
        if (row == null) {
            row = new EcSystemConfig();
            row.setConfigKey(CONFIG_KEY);
        }
        try {
            row.setConfigJson(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置序列化失败");
        }
        row.setUpdateTime(LocalDateTime.now());
        if (ecSystemConfigMapper.selectById(CONFIG_KEY) == null) {
            ecSystemConfigMapper.insert(row);
        } else {
            ecSystemConfigMapper.updateById(row);
        }
    }

    @Override
    public StorageCleanupResultVO cleanupCache(boolean dryRun) {
        StorageCenterConfigVO config = getConfig();
        long cacheMax = mbToBytes(resolveCacheMaxMb(config));
        long cacheUsed = measureCacheBytes();

        StorageCleanupResultVO result = new StorageCleanupResultVO();
        result.setZone("REDIS_CACHE");
        result.setDryRun(dryRun);
        result.setScannedCount(0);
        result.setRemovedCount(0);
        result.setFreedBytes(0L);
        result.setSampleFiles(new ArrayList<>());

        if (cacheMax <= 0 || cacheUsed <= cacheMax) {
            return result;
        }

        String strategy = resolveOverLimitStrategy(config, StorageOverLimitStrategySupport.ZONE_REDIS_CACHE);
        if (StorageOverLimitStrategySupport.REJECT.equals(strategy)) {
            return result;
        }

        List<CacheEntry> entries = listCacheEntries();
        result.setScannedCount(entries.size());
        sortCacheEntries(entries, strategy);

        long targetUsed = (long) (cacheMax * 0.8);
        long currentUsed = cacheUsed;
        long freed = 0L;
        int removed = 0;
        for (CacheEntry entry : entries) {
            if (currentUsed <= targetUsed) {
                break;
            }
            if (result.getSampleFiles().size() < 8) {
                result.getSampleFiles().add(entry.key());
            }
            if (!dryRun) {
                stringRedisTemplate.delete(entry.key());
            }
            currentUsed -= entry.memoryBytes();
            freed += entry.memoryBytes();
            removed++;
        }
        result.setRemovedCount(removed);
        result.setFreedBytes(freed);
        return result;
    }

    @Override
    public boolean isDualStorageEnabled() {
        return Boolean.TRUE.equals(getConfig().getDualStorageEnabled());
    }

    @Override
    public String resolveOverLimitStrategy(String zoneKey) {
        return resolveOverLimitStrategy(getConfig(), zoneKey);
    }

    @Override
    public void enforceCacheLimit(long incomingBytes) {
        StorageCenterConfigVO config = getConfig();
        long cacheMax = mbToBytes(resolveCacheMaxMb(config));
        if (cacheMax <= 0) {
            return;
        }
        long cacheUsed = measureCacheBytes();
        if (cacheUsed + incomingBytes <= cacheMax) {
            return;
        }
        String strategy = resolveOverLimitStrategy(config, StorageOverLimitStrategySupport.ZONE_REDIS_CACHE);
        if (StorageOverLimitStrategySupport.REJECT.equals(strategy)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Redis 缓存已达上限，无法继续缓存");
        }
        cleanupCache(false);
        if (measureCacheBytes() + incomingBytes > cacheMax) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Redis 缓存已达上限，请清理后重试");
        }
        if (incomingBytes > cacheMax) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "单条内容超过 Redis 缓存上限");
        }
    }

    @Override
    public void assertWritable(String zoneKey, long additionalBytes) {
        StorageCenterConfigVO config = getConfig();
        long zoneQuota = mbToBytes(resolveZoneQuotaMb(config, zoneKey));
        long totalQuota = mbToBytes(config.getLocalQuotaMb());
        long zoneUsed = resolveZoneUsedBytes(zoneKey);
        long totalUsed = sumAllZoneUsedBytes();

        if (zoneQuota > 0 && zoneUsed + additionalBytes > zoneQuota) {
            handleOverLimit(config, zoneKey, zoneKey, additionalBytes, zoneQuota, zoneUsed);
        }
        if (totalQuota > 0 && totalUsed + additionalBytes > totalQuota) {
            handleOverLimit(
                    config,
                    StorageOverLimitStrategySupport.ZONE_LOCAL_TOTAL,
                    zoneKey,
                    additionalBytes,
                    totalQuota,
                    totalUsed
            );
        }
    }

    @Override
    public void onFileWritten(String zoneKey, long bytesWritten) {
        // 预留钩子：后续可接入实时计量或审计
        log.debug("storage write zone={} bytes={}", zoneKey, bytesWritten);
    }

    @Override
    public PageResult<StorageImageItemVO> browseProjectImages(
            String zone,
            String keyword,
            Long page,
            Long pageSize
    ) {
        String zoneKey = StringUtils.hasText(zone) ? zone.trim().toUpperCase() : ZONE_ECOMMERCE_IMAGES;
        if (!BROWSE_IMAGE_ZONES.contains(zoneKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持浏览该存储分区");
        }
        long p = page == null || page < 1 ? 1L : page;
        long ps = pageSize == null || pageSize < 1 ? 24L : Math.min(pageSize, 100L);
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";

        Path dir = resolveZoneDir(zoneKey);
        if (dir == null || !Files.isDirectory(dir)) {
            return PageResult.empty(p, ps);
        }

        List<StorageImageItemVO> items = new ArrayList<>();
        boolean excludeImports = ZONE_ECOMMERCE_IMAGES.equals(zoneKey);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                if (excludeImports && path.toString().contains(dir.resolve("imports").toString())) {
                    return;
                }
                String fileName = path.getFileName().toString();
                if (!isImageFileName(fileName)) {
                    return;
                }
                if (StringUtils.hasText(kw) && !fileName.toLowerCase().contains(kw)) {
                    return;
                }
                try {
                    StorageImageItemVO item = new StorageImageItemVO();
                    item.setZone(zoneKey);
                    item.setFileName(fileName);
                    item.setRelativePath(dir.relativize(path).toString().replace('\\', '/'));
                    item.setSizeBytes(Files.size(path));
                    item.setModifiedAt(LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            ZoneId.systemDefault()
                    ));
                    items.add(item);
                } catch (IOException ignored) {
                    // skip
                }
            });
        } catch (IOException ex) {
            log.warn("浏览项目图片失败 {}: {}", zoneKey, ex.getMessage());
            return PageResult.empty(p, ps);
        }

        items.sort(Comparator.comparing(StorageImageItemVO::getModifiedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        int from = (int) Math.min((p - 1) * ps, items.size());
        int to = (int) Math.min(from + ps, items.size());

        PageResult<StorageImageItemVO> result = new PageResult<>();
        result.setPage(p);
        result.setPageSize(ps);
        result.setTotal(items.size());
        result.setRecords(items.subList(from, to));
        return result;
    }

    private boolean isImageFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String lower = fileName.toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void handleOverLimit(
            StorageCenterConfigVO config,
            String limitScopeKey,
            String cleanupZoneKey,
            long additionalBytes,
            long quotaBytes,
            long usedBytes
    ) {
        String strategy = resolveOverLimitStrategy(config, limitScopeKey);
        if (!StorageOverLimitStrategySupport.isCleanup(strategy)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "存储空间已达上限，无法继续上传");
        }
        long needFree = usedBytes + additionalBytes - quotaBytes;
        long freed = StorageOverLimitStrategySupport.ZONE_LOCAL_TOTAL.equals(limitScopeKey)
                ? freeFilesAcrossZones(strategy, needFree)
                : freeFilesInZone(cleanupZoneKey, strategy, needFree);
        if (usedBytes - freed + additionalBytes > quotaBytes) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "存储空间不足，请清理后重试");
        }
    }

    private long freeFilesInZone(String zoneKey, String strategy, long needFreeBytes) {
        Path dir = resolveZoneDir(zoneKey);
        if (dir == null || !Files.isDirectory(dir)) {
            return 0L;
        }
        return freeFiles(collectFiles(dir, zoneKey), strategy, needFreeBytes);
    }

    private long freeFilesAcrossZones(String strategy, long needFreeBytes) {
        List<FileEntry> files = new ArrayList<>();
        for (String zoneKey : List.of(
                ZONE_ECOMMERCE_IMAGES,
                ZONE_NOTEBOOK_IMAGES,
                ZONE_NOTEBOOK_CONTENT,
                ZONE_IMPORT_FILES
        )) {
            Path dir = resolveZoneDir(zoneKey);
            if (dir != null && Files.isDirectory(dir)) {
                files.addAll(collectFiles(dir, zoneKey));
            }
        }
        return freeFiles(files, strategy, needFreeBytes);
    }

    private List<FileEntry> collectFiles(Path dir, String zoneKey) {
        List<FileEntry> files = new ArrayList<>();
        boolean excludeImports = ZONE_ECOMMERCE_IMAGES.equals(zoneKey);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                if (excludeImports && path.toString().contains(dir.resolve("imports").toString())) {
                    return;
                }
                try {
                    files.add(new FileEntry(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path)));
                } catch (IOException ignored) {
                    // skip
                }
            });
        } catch (IOException ex) {
            log.warn("扫描分区文件失败 {}: {}", zoneKey, ex.getMessage());
        }
        return files;
    }

    private long freeFiles(List<FileEntry> files, String strategy, long needFreeBytes) {
        if (StorageOverLimitStrategySupport.CLEANUP_LARGEST.equals(strategy)) {
            files.sort(Comparator.comparingLong(FileEntry::size).reversed());
        } else {
            files.sort(Comparator.comparingLong(FileEntry::modifiedAt));
        }
        long freed = 0L;
        for (FileEntry entry : files) {
            if (freed >= needFreeBytes) {
                break;
            }
            try {
                long size = Files.size(entry.path());
                Files.deleteIfExists(entry.path());
                freed += size;
            } catch (IOException ex) {
                log.warn("清理文件失败: {}", entry.path(), ex);
            }
        }
        return freed;
    }

    private void sortCacheEntries(List<CacheEntry> entries, String strategy) {
        if (StorageOverLimitStrategySupport.CLEANUP_LARGEST.equals(strategy)) {
            entries.sort(Comparator.comparingLong(CacheEntry::memoryBytes).reversed());
            return;
        }
        entries.sort(Comparator
                .comparingLong(CacheEntry::ttlSeconds)
                .thenComparingLong(CacheEntry::memoryBytes));
    }

    private String resolveOverLimitStrategy(StorageCenterConfigVO config, String zoneKey) {
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
    private StorageZoneVO buildZone(
            StorageCenterConfigVO config,
            String key,
            String label,
            Path localDir,
            String cloudPath,
            Long quotaMb,
            boolean cloudAvailable,
            Boolean dualStorageEnabled,
            boolean excludeImportsSubdir
    ) {
        long used = 0L;
        long fileCount = 0L;
        try {
            if (excludeImportsSubdir && Files.isDirectory(localDir)) {
                try (Stream<Path> stream = Files.list(localDir)) {
                    for (Path child : stream.toList()) {
                        if (Files.isDirectory(child) && "imports".equals(child.getFileName().toString())) {
                            continue;
                        }
                        if (Files.isRegularFile(child)) {
                            used += Files.size(child);
                            fileCount++;
                        } else if (Files.isDirectory(child)) {
                            used += StoragePathSupport.directorySize(child);
                            fileCount += StoragePathSupport.countRegularFiles(child);
                        }
                    }
                }
            } else {
                used = StoragePathSupport.directorySize(localDir);
                fileCount = StoragePathSupport.countRegularFiles(localDir);
            }
        } catch (IOException ex) {
            log.warn("统计存储分区失败 {}: {}", key, ex.getMessage());
        }

        long quotaBytes = mbToBytes(quotaMb);
        StorageZoneVO zone = new StorageZoneVO();
        zone.setKey(key);
        zone.setLabel(label);
        zone.setLocalPath(localDir.toString());
        zone.setCloudPath(cloudPath);
        zone.setUsedBytes(used);
        zone.setQuotaBytes(quotaBytes);
        zone.setFileCount(fileCount);
        zone.setUsagePercent(calcPercent(used, quotaBytes));
        zone.setDualStorageEnabled(Boolean.TRUE.equals(dualStorageEnabled));
        zone.setCloudAvailable(cloudAvailable);
        zone.setOverLimitStrategy(resolveOverLimitStrategy(config, key));
        return zone;
    }

    private StorageOrphanZonePreviewVO scanZoneOrphans(String zoneKey, boolean dryRun, int listLimit) {
        StorageOrphanZonePreviewVO zonePreview = new StorageOrphanZonePreviewVO();
        zonePreview.setZoneKey(zoneKey);
        zonePreview.setZoneLabel(resolveZoneLabel(zoneKey));
        zonePreview.setZonePurpose(resolveZonePurpose(zoneKey));
        zonePreview.setSupported(isOrphanCleanupSupported(zoneKey));
        zonePreview.setZoneQuotaBytes(resolveZoneQuotaBytes(zoneKey));

        Path dir = resolveZoneDir(zoneKey);
        if (dir == null || !zonePreview.isSupported()) {
            zonePreview.setLocalPath(dir == null ? "" : dir.toString());
            zonePreview.setScannedCount(0);
            zonePreview.setOrphanCount(0);
            zonePreview.setFreedBytes(0L);
            zonePreview.setFiles(List.of());
            return zonePreview;
        }
        zonePreview.setLocalPath(dir.toString());

        Set<String> referenced = collectReferences(zoneKey);
        boolean skipImportsDir = ZONE_ECOMMERCE_IMAGES.equals(zoneKey);
        OrphanScanResult scan = scanAndMaybeDelete(dir, referenced, zoneKey, skipImportsDir, dryRun, listLimit);
        zonePreview.setScannedCount(scan.scannedCount());
        zonePreview.setOrphanCount(scan.orphanCount());
        zonePreview.setFreedBytes(scan.freedBytes());
        zonePreview.setFiles(scan.files());
        return zonePreview;
    }

    private boolean isOrphanCleanupSupported(String zoneKey) {
        return ZONE_ECOMMERCE_IMAGES.equals(zoneKey)
                || ZONE_NOTEBOOK_IMAGES.equals(zoneKey)
                || ZONE_NOTEBOOK_CONTENT.equals(zoneKey)
                || ZONE_IMPORT_FILES.equals(zoneKey);
    }

    private String resolveZoneLabel(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES -> "电商图片";
            case ZONE_NOTEBOOK_IMAGES -> "笔记图片";
            case ZONE_NOTEBOOK_CONTENT -> "笔记正文";
            case ZONE_IMPORT_FILES -> "销售订单导入";
            default -> zoneKey;
        };
    }

    private String resolveZonePurpose(String zoneKey) {
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

    private Set<String> collectReferences(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES -> collectEcommerceImageReferences();
            case ZONE_NOTEBOOK_IMAGES -> collectNotebookImageReferences();
            case ZONE_NOTEBOOK_CONTENT -> collectNoteContentReferences();
            case ZONE_IMPORT_FILES -> collectImportFileReferences();
            default -> Set.of();
        };
    }

    private OrphanScanResult scanAndMaybeDelete(
            Path dir,
            Set<String> referenced,
            String zoneKey,
            boolean skipImportsDir,
            boolean dryRun,
            int listLimit
    ) {
        if (!Files.isDirectory(dir)) {
            return new OrphanScanResult(0, 0, 0L, List.of());
        }

        int scanned = 0;
        int orphanCount = 0;
        long freed = 0L;
        List<StorageOrphanFileItemVO> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.filter(Files::isRegularFile).toList();
            for (Path file : paths) {
                if (skipImportsDir && file.toString().contains(dir.resolve("imports").toString())) {
                    continue;
                }
                scanned++;
                String name = file.getFileName().toString();
                if (referenced.contains(name)) {
                    continue;
                }
                long size = Files.size(file);
                orphanCount++;
                freed += size;
                if (files.size() < listLimit) {
                    StorageOrphanFileItemVO item = new StorageOrphanFileItemVO();
                    item.setFileName(name);
                    item.setRelativePath(dir.relativize(file).toString().replace('\\', '/'));
                    item.setSizeBytes(size);
                    item.setOrphanedAt(LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(file).toInstant(),
                            ZoneId.systemDefault()));
                    files.add(item);
                }
                if (!dryRun) {
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "清理孤立文件失败: " + ex.getMessage());
        }
        return new OrphanScanResult(scanned, orphanCount, freed, files);
    }

    private record OrphanScanResult(
            int scannedCount,
            int orphanCount,
            long freedBytes,
            List<StorageOrphanFileItemVO> files
    ) {
    }

    private Set<String> collectEcommerceImageReferences() {
        Set<String> refs = new HashSet<>();
        ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                        .select(EcProduct::getImageName)
                        .isNotNull(EcProduct::getImageName))
                .forEach(row -> refs.add(row.getImageName().trim()));
        ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .select(EcSku::getImageName)
                        .isNotNull(EcSku::getImageName))
                .forEach(row -> refs.add(row.getImageName().trim()));
        ecCartonMapper.selectList(new LambdaQueryWrapper<EcCarton>()
                        .select(EcCarton::getPreviewImage)
                        .isNotNull(EcCarton::getPreviewImage))
                .forEach(row -> refs.add(row.getPreviewImage().trim()));
        ecPlatformMapper.selectList(new LambdaQueryWrapper<EcPlatform>()
                        .select(EcPlatform::getAvatarUrl)
                        .isNotNull(EcPlatform::getAvatarUrl))
                .forEach(row -> refs.add(extractFileName(row.getAvatarUrl())));
        ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                        .select(EcShop::getAvatarUrl)
                        .isNotNull(EcShop::getAvatarUrl))
                .forEach(row -> refs.add(extractFileName(row.getAvatarUrl())));
        ecExpressStationMapper.selectList(new LambdaQueryWrapper<EcExpressStation>()
                        .select(EcExpressStation::getAvatarUrl)
                        .isNotNull(EcExpressStation::getAvatarUrl))
                .forEach(row -> refs.add(extractFileName(row.getAvatarUrl())));
        refs.remove("");
        return refs;
    }

    private Set<String> collectNotebookImageReferences() {
        Set<String> refs = new HashSet<>();
        Path notesDir = StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot()).resolve("notes");
        if (!Files.isDirectory(notesDir)) {
            return refs;
        }
        try (Stream<Path> stream = Files.list(notesDir)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".html")).toList()) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    Matcher matcher = NOTEBOOK_IMAGE_REF_PATTERN.matcher(content);
                    while (matcher.find()) {
                        refs.add(matcher.group(1).trim());
                    }
                } catch (IOException ex) {
                    log.debug("扫描笔记正文图片引用失败 {}: {}", file, ex.getMessage());
                }
            }
        } catch (IOException ex) {
            log.warn("读取笔记正文目录失败: {}", ex.getMessage());
        }
        refs.remove("");
        return refs;
    }

    private Set<String> collectNoteContentReferences() {
        Set<String> refs = new HashSet<>();
        nbNoteMapper.selectList(new LambdaQueryWrapper<NbNote>()
                        .select(NbNote::getId)
                        .eq(NbNote::getDeleted, 0))
                .forEach(note -> {
                    if (note.getId() != null) {
                        refs.add(note.getId() + ".html");
                        refs.add(note.getId() + ".meta.json");
                    }
                });
        return refs;
    }

    private Set<String> collectImportFileReferences() {
        Set<String> refs = new HashSet<>();
        sysImportBatchMapper.selectList(new LambdaQueryWrapper<SysImportBatch>()
                        .select(SysImportBatch::getFilePath)
                        .isNotNull(SysImportBatch::getFilePath))
                .forEach(batch -> refs.add(extractFileName(batch.getFilePath())));
        refs.remove("");
        return refs;
    }

    private String extractFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private long resolveZoneUsedBytes(String zoneKey) {
        try {
            return switch (zoneKey) {
                case ZONE_ECOMMERCE_IMAGES -> {
                    Path dir = StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath);
                    long used = 0L;
                    if (Files.isDirectory(dir)) {
                        try (Stream<Path> stream = Files.list(dir)) {
                            for (Path child : stream.toList()) {
                                if (Files.isDirectory(child) && "imports".equals(child.getFileName().toString())) {
                                    continue;
                                }
                                used += Files.isDirectory(child)
                                        ? StoragePathSupport.directorySize(child)
                                        : Files.size(child);
                            }
                        }
                    }
                    yield used;
                }
                case ZONE_NOTEBOOK_IMAGES ->
                        StoragePathSupport.directorySize(StoragePathSupport.resolveUploadBasePath(notebookImagesPath));
                case ZONE_NOTEBOOK_CONTENT ->
                        StoragePathSupport.directorySize(StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot()));
                case ZONE_IMPORT_FILES -> StoragePathSupport.directorySize(
                        StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath)
                                .resolve("imports").resolve("sales-orders"));
                default -> 0L;
            };
        } catch (IOException ex) {
            return 0L;
        }
    }

    private long sumAllZoneUsedBytes() {
        long total = 0L;
        total += resolveZoneUsedBytes(ZONE_ECOMMERCE_IMAGES);
        total += resolveZoneUsedBytes(ZONE_NOTEBOOK_IMAGES);
        total += resolveZoneUsedBytes(ZONE_NOTEBOOK_CONTENT);
        total += resolveZoneUsedBytes(ZONE_IMPORT_FILES);
        return total;
    }

    private Path resolveZoneDir(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES -> StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath);
            case ZONE_NOTEBOOK_IMAGES -> StoragePathSupport.resolveUploadBasePath(notebookImagesPath);
            case ZONE_NOTEBOOK_CONTENT -> StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot());
            case ZONE_IMPORT_FILES -> StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath)
                    .resolve("imports").resolve("sales-orders");
            default -> null;
        };
    }

    private Long resolveZoneQuotaMb(StorageCenterConfigVO config, String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE_IMAGES -> config.getEcommerceImagesQuotaMb();
            case ZONE_NOTEBOOK_IMAGES -> config.getNotebookImagesQuotaMb();
            case ZONE_NOTEBOOK_CONTENT -> config.getNotebookContentQuotaMb();
            case ZONE_IMPORT_FILES -> config.getImportFilesQuotaMb();
            default -> 0L;
        };
    }

    private long resolveZoneQuotaBytes(String zoneKey) {
        StorageCenterConfigVO config = getConfig();
        return mbToBytes(resolveZoneQuotaMb(config, zoneKey));
    }

    private long measureCacheBytes() {
        List<CacheEntry> entries = listCacheEntries();
        return entries.stream().mapToLong(CacheEntry::memoryBytes).sum();
    }

    private List<CacheEntry> listCacheEntries() {
        List<CacheEntry> entries = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(CACHE_PREFIX + "*").count(200).build();
        try (RedisConnection connection = stringRedisTemplate.getRequiredConnectionFactory().getConnection();
             Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                String key = new String(rawKey, StandardCharsets.UTF_8);
                String value = stringRedisTemplate.opsForValue().get(key);
                long bytes = rawKey.length + (value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length);
                Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                long ttlSeconds = ttl == null || ttl < 0 ? Long.MAX_VALUE : ttl;
                entries.add(new CacheEntry(key, bytes, ttlSeconds));
            }
        } catch (Exception ex) {
            log.warn("扫描 Redis 缓存失败: {}", ex.getMessage());
        }
        return entries;
    }

    private StorageCenterConfigVO defaultConfig() {
        StorageCenterConfigVO config = new StorageCenterConfigVO();
        config.setLocalQuotaMb(10240L);
        config.setEcommerceImagesQuotaMb(5120L);
        config.setNotebookImagesQuotaMb(2048L);
        config.setNotebookContentQuotaMb(1024L);
        config.setImportFilesQuotaMb(2048L);
        config.setCacheMaxMb(noteStorageProperties.getCacheMaxMb());
        config.setCacheTtlSeconds(noteStorageProperties.getCacheTtlSeconds());
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

    private StorageCenterConfigVO mergeDefaults(StorageCenterConfigVO value, StorageCenterConfigVO fallback) {
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

    private void validateConfig(StorageCenterConfigSaveRequest request) {
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

    private void validatePositive(Long value, String label) {
        if (value != null && value < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "不能为负数");
        }
    }

    private void validateStrategy(String strategy, String label) {
        if (strategy != null && !StorageOverLimitStrategySupport.isValid(strategy)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), label + "无效");
        }
    }

    private long resolveCacheMaxMb(StorageCenterConfigVO config) {
        if (config.getCacheMaxMb() != null) {
            return config.getCacheMaxMb();
        }
        return noteStorageProperties.getCacheMaxMb();
    }

    private long resolveCacheTtl(StorageCenterConfigVO config) {
        if (config.getCacheTtlSeconds() != null) {
            return config.getCacheTtlSeconds();
        }
        return noteStorageProperties.getCacheTtlSeconds();
    }

    private long mbToBytes(Long mb) {
        if (mb == null || mb <= 0) {
            return 0L;
        }
        return mb * 1024L * 1024L;
    }

    private int calcPercent(long used, long quota) {
        if (quota <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(used * 100.0 / quota));
    }

    private record FileEntry(Path path, long modifiedAt, long size) {
    }

    private record CacheEntry(String key, long memoryBytes, long ttlSeconds) {
    }
}
