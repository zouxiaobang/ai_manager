package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.dto.ImageSpaceRenameRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.vo.ImageSpaceCategoryNodeVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageDetailVO;
import com.ai.manager.system.domain.vo.ImageSpaceImageItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNameCheckVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.service.ImageSpaceService;
import com.ai.manager.system.service.impl.StorageCenterServiceImpl;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeItemVO;
import com.ai.manager.system.domain.vo.ImageSpaceNormalizeResultVO;
import com.ai.manager.system.service.support.EcEcommerceImageNameSupport;
import com.ai.manager.system.service.support.StoragePathSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSpaceServiceImpl implements ImageSpaceService {

    private static final String ZONE_ECOMMERCE = StorageCenterServiceImpl.ZONE_ECOMMERCE_IMAGES;
    private static final String ZONE_NOTEBOOK = StorageCenterServiceImpl.ZONE_NOTEBOOK_IMAGES;
    private static final String CAT_ALL = "all";
    private static final String CAT_UNCATEGORIZED = "uncategorized";
    private static final String SPU_PREFIX = "spu-";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Pattern NOTEBOOK_IMAGE_REF_PATTERN = Pattern.compile(
            "notebook/images[/\\\\]([^\"'\\s>?#]+)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EcProductMapper ecProductMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcShopMapper ecShopMapper;
    private final EcExpressStationMapper ecExpressStationMapper;
    private final NoteStorageProperties noteStorageProperties;

    @Value("${ai-manager.upload.ecommerce-path:uploads/ecommerce}")
    private String ecommerceUploadPath;

    @Value("${ai-manager.upload.notebook-images-path:uploads/notebook/images}")
    private String notebookImagesPath;

    @Override
    public List<ImageSpaceCategoryNodeVO> listCategories() {
        List<ImageSpaceCategoryNodeVO> roots = new ArrayList<>();

        ImageSpaceCategoryNodeVO ecommerce = new ImageSpaceCategoryNodeVO();
        ecommerce.setId(ZONE_ECOMMERCE);
        ecommerce.setLabel("电商平台");
        ecommerce.getChildren().add(categoryNode(CAT_ALL, "全部图片", null));
        ecommerce.getChildren().add(categoryNode(CAT_UNCATEGORIZED, "未分类", null));
        ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                        .select(EcProduct::getId, EcProduct::getName)
                        .orderByAsc(EcProduct::getName))
                .forEach(product -> ecommerce.getChildren().add(
                        categoryNode(SPU_PREFIX + product.getId(), product.getName(), product.getId())
                ));
        roots.add(ecommerce);

        ImageSpaceCategoryNodeVO notebook = new ImageSpaceCategoryNodeVO();
        notebook.setId(ZONE_NOTEBOOK);
        notebook.setLabel("笔记本");
        notebook.getChildren().add(categoryNode(CAT_ALL, "全部图片", null));
        roots.add(notebook);

        return roots;
    }

    @Override
    public PageResult<ImageSpaceImageItemVO> pageImages(
            String zone,
            String categoryId,
            String keyword,
            Long page,
            Long pageSize
    ) {
        String zoneKey = normalizeZone(zone);
        long p = page == null || page < 1 ? 1L : page;
        long ps = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100L);
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String cat = StringUtils.hasText(categoryId) ? categoryId.trim() : CAT_ALL;

        ReferenceIndex index = buildReferenceIndex();
        SpuIndex spuIndex = buildSpuIndex();
        List<ImageFile> files = listZoneImageFiles(zoneKey);

        List<ImageSpaceImageItemVO> filtered = files.stream()
                .filter(file -> matchesKeyword(file, kw))
                .filter(file -> matchesCategory(zoneKey, cat, file, spuIndex))
                .map(file -> toListItem(file, index, spuIndex))
                .toList();

        int from = (int) Math.min((p - 1) * ps, filtered.size());
        int to = (int) Math.min(from + ps, filtered.size());

        PageResult<ImageSpaceImageItemVO> result = new PageResult<>();
        result.setPage(p);
        result.setPageSize(ps);
        result.setTotal(filtered.size());
        result.setRecords(filtered.subList(from, to));
        return result;
    }

    @Override
    public ImageSpaceImageDetailVO getImageDetail(String zone, String relativePath) {
        ImageFile file = resolveImageFile(normalizeZone(zone), relativePath);
        ReferenceIndex index = buildReferenceIndex();
        SpuIndex spuIndex = buildSpuIndex();
        return toDetail(file, index, spuIndex);
    }

    @Override
    public ImageSpaceNameCheckVO checkFileName(String zone, String relativePath, String newFileName) {
        String zoneKey = normalizeZone(zone);
        ImageFile current = resolveImageFile(zoneKey, relativePath);
        String nextName = normalizeNewFileName(newFileName);
        ImageSpaceNameCheckVO vo = new ImageSpaceNameCheckVO();

        if (current.fileName().equalsIgnoreCase(nextName)) {
            vo.setAvailable(true);
            vo.setMessage("名称未变更");
            return vo;
        }
        if (!isImageFileName(nextName)) {
            vo.setAvailable(false);
            vo.setMessage("仅支持 JPG、PNG、WebP、GIF 图片");
            return vo;
        }
        Path parent = current.path().getParent();
        Path target = parent.resolve(nextName).normalize();
        if (!target.startsWith(resolveZoneDir(zoneKey))) {
            vo.setAvailable(false);
            vo.setMessage("非法文件名");
            return vo;
        }
        if (Files.exists(target)) {
            vo.setAvailable(false);
            vo.setMessage("该分区下已存在同名图片");
            return vo;
        }
        vo.setAvailable(true);
        vo.setMessage("名称可用");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImageSpaceImageDetailVO renameImage(ImageSpaceRenameRequest request) {
        if (request == null || !StringUtils.hasText(request.getRelativePath())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择图片");
        }
        String zoneKey = normalizeZone(request.getZone());
        ImageFile current = resolveImageFile(zoneKey, request.getRelativePath());
        ImageSpaceNameCheckVO check = checkFileName(zoneKey, current.relativePath(), request.getNewFileName());
        if (!check.isAvailable()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), check.getMessage());
        }

        String nextName = normalizeNewFileName(request.getNewFileName());
        if (current.fileName().equalsIgnoreCase(nextName)) {
            return getImageDetail(zoneKey, current.relativePath());
        }

        Path source = current.path();
        Path target = source.getParent().resolve(nextName).normalize();
        if (!target.startsWith(resolveZoneDir(zoneKey))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
        }
        try {
            Files.move(source, target);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重命名失败");
        }

        String oldKey = current.fileName();
        String newKey = nextName;
        updateReferencesAfterRename(oldKey, newKey);

        String newRelative = resolveZoneDir(zoneKey).relativize(target).toString().replace('\\', '/');
        return getImageDetail(zoneKey, newRelative);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(String zone, String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择图片");
        }
        String zoneKey = normalizeZone(zone);
        ImageFile file = resolveImageFile(zoneKey, relativePath);
        ReferenceIndex index = buildReferenceIndex();
        ReferenceInfo ref = index.byFileName().getOrDefault(file.fileName(), ReferenceInfo.empty());
        if (ref.count() > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片已被业务引用，无法删除");
        }
        try {
            Files.deleteIfExists(file.path());
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "删除图片失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImageSpaceNormalizeResultVO normalizeEcommerceImageNames(boolean dryRun) {
        Path uploadDir = resolveZoneDir(ZONE_ECOMMERCE);
        Map<Long, String> productNames = new LinkedHashMap<>();
        ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                        .select(EcProduct::getId, EcProduct::getName, EcProduct::getImageName))
                .forEach(product -> {
                    if (product.getId() != null) {
                        productNames.put(product.getId(), product.getName());
                    }
                });

        Map<String, String> renamePlans = new LinkedHashMap<>();
        Map<String, String> planSources = new LinkedHashMap<>();

        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .select(EcSku::getId, EcSku::getProductId, EcSku::getSkuCode, EcSku::getSpecName, EcSku::getImageName)
                .isNotNull(EcSku::getImageName));
        for (EcSku sku : skus) {
            if (!StringUtils.hasText(sku.getImageName())) {
                continue;
            }
            String oldName = extractFileName(sku.getImageName());
            if (!isImageFileName(oldName)) {
                continue;
            }
            String spuName = productNames.getOrDefault(sku.getProductId(), "SPU#" + sku.getProductId());
            String skuDisplayName = EcEcommerceImageNameSupport.resolveSkuDisplayName(
                    sku.getSpecName(), sku.getSkuCode(), sku.getId());
            String newName = EcEcommerceImageNameSupport.buildSkuImageFileName(spuName, skuDisplayName, oldName);
            mergeRenamePlan(renamePlans, planSources, oldName, newName, "SKU");
        }

        Set<String> skuImageNames = new HashSet<>(renamePlans.keySet());
        List<EcProduct> products = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                .select(EcProduct::getId, EcProduct::getName, EcProduct::getImageName)
                .isNotNull(EcProduct::getImageName));
        for (EcProduct product : products) {
            if (!StringUtils.hasText(product.getImageName())) {
                continue;
            }
            String oldName = extractFileName(product.getImageName());
            if (!isImageFileName(oldName) || skuImageNames.contains(oldName)) {
                continue;
            }
            String newName = EcEcommerceImageNameSupport.buildSpuMainImageFileName(product.getName(), oldName);
            mergeRenamePlan(renamePlans, planSources, oldName, newName, "SPU_MAIN");
        }

        Map<Long, String> platformNames = new LinkedHashMap<>();
        ecPlatformMapper.selectList(new LambdaQueryWrapper<EcPlatform>()
                        .select(EcPlatform::getId, EcPlatform::getName, EcPlatform::getAvatarUrl))
                .forEach(row -> {
                    if (row.getId() != null) {
                        platformNames.put(row.getId(), row.getName());
                    }
                    if (!StringUtils.hasText(row.getAvatarUrl())) {
                        return;
                    }
                    String oldName = extractFileName(row.getAvatarUrl());
                    if (!isImageFileName(oldName)) {
                        return;
                    }
                    String newName = EcEcommerceImageNameSupport.buildPlatformAvatarFileName(row.getName(), oldName);
                    mergeRenamePlan(renamePlans, planSources, oldName, newName, "PLATFORM");
                });

        ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                        .select(EcShop::getPlatformId, EcShop::getName, EcShop::getAvatarUrl)
                        .isNotNull(EcShop::getAvatarUrl))
                .forEach(row -> {
                    if (!StringUtils.hasText(row.getAvatarUrl())) {
                        return;
                    }
                    String oldName = extractFileName(row.getAvatarUrl());
                    if (!isImageFileName(oldName)) {
                        return;
                    }
                    String platformName = platformNames.getOrDefault(row.getPlatformId(), "平台#" + row.getPlatformId());
                    String newName = EcEcommerceImageNameSupport.buildShopAvatarFileName(
                            platformName, row.getName(), oldName);
                    mergeRenamePlan(renamePlans, planSources, oldName, newName, "SHOP");
                });

        ecExpressStationMapper.selectList(new LambdaQueryWrapper<EcExpressStation>()
                        .select(EcExpressStation::getName, EcExpressStation::getAvatarUrl)
                        .isNotNull(EcExpressStation::getAvatarUrl))
                .forEach(row -> {
                    if (!StringUtils.hasText(row.getAvatarUrl())) {
                        return;
                    }
                    String oldName = extractFileName(row.getAvatarUrl());
                    if (!isImageFileName(oldName)) {
                        return;
                    }
                    String newName = EcEcommerceImageNameSupport.buildExpressAvatarFileName(row.getName(), oldName);
                    mergeRenamePlan(renamePlans, planSources, oldName, newName, "EXPRESS");
                });

        ecCartonMapper.selectList(new LambdaQueryWrapper<EcCarton>()
                        .select(EcCarton::getName, EcCarton::getPreviewImage)
                        .isNotNull(EcCarton::getPreviewImage))
                .forEach(row -> {
                    if (!StringUtils.hasText(row.getPreviewImage())) {
                        return;
                    }
                    String oldName = extractFileName(row.getPreviewImage());
                    if (!isImageFileName(oldName)) {
                        return;
                    }
                    String newName = EcEcommerceImageNameSupport.buildCartonPreviewFileName(row.getName(), oldName);
                    mergeRenamePlan(renamePlans, planSources, oldName, newName, "CARTON");
                });

        ReferenceIndex referenceIndex = buildReferenceIndex();
        Set<String> referencedFileNames = new HashSet<>(referenceIndex.byFileName().keySet());
        Set<String> plannedOldNames = new HashSet<>(renamePlans.keySet());
        DateTimeFormatter orphanDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        Map<String, Integer> orphanDayCounters = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(uploadDir)) {
            List<Path> orphanFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> isImageFileName(path.getFileName().toString()))
                    .filter(path -> !referencedFileNames.contains(path.getFileName().toString()))
                    .filter(path -> !plannedOldNames.contains(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            for (Path orphan : orphanFiles) {
                String oldName = orphan.getFileName().toString();
                LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(orphan).toInstant(),
                        ZoneId.systemDefault()
                );
                String dateKey = modifiedAt.format(orphanDateFmt);
                int sequence = orphanDayCounters.merge(dateKey, 1, Integer::sum);
                String newName = EcEcommerceImageNameSupport.buildOrphanFileName(dateKey, sequence, oldName);
                mergeRenamePlan(renamePlans, planSources, oldName, newName, "ORPHAN");
            }
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "扫描未分类图片失败");
        }

        Set<String> reservedNames = new HashSet<>();
        try (Stream<Path> stream = Files.list(uploadDir)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .forEach(reservedNames::add);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "读取电商图片目录失败");
        }

        ImageSpaceNormalizeResultVO result = new ImageSpaceNormalizeResultVO();
        result.setDryRun(dryRun);

        for (Map.Entry<String, String> entry : renamePlans.entrySet()) {
            String oldName = entry.getKey();
            String source = planSources.getOrDefault(oldName, "SKU");
            ImageSpaceNormalizeItemVO item = new ImageSpaceNormalizeItemVO();
            item.setOldName(oldName);
            item.setSource(source);

            if (oldName.equalsIgnoreCase(entry.getValue())) {
                item.setNewName(oldName);
                item.setStatus("SKIPPED");
                item.setMessage("已符合命名规则");
                result.getItems().add(item);
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }

            String newName = EcEcommerceImageNameSupport.allocateUniqueFileName(entry.getValue(), reservedNames);
            item.setNewName(newName);
            result.setPlanned(result.getPlanned() + 1);

            Path sourcePath = uploadDir.resolve(oldName).normalize();
            if (!sourcePath.startsWith(uploadDir) || !Files.isRegularFile(sourcePath)) {
                item.setStatus(dryRun ? "PLANNED" : "FAILED");
                item.setMessage("本地文件不存在，仅更新数据库引用");
                if (!dryRun) {
                    updateReferencesAfterRename(oldName, newName);
                    item.setStatus("RENAMED");
                    item.setMessage("数据库引用已更新（文件不存在）");
                    result.setRenamed(result.getRenamed() + 1);
                }
                result.getItems().add(item);
                continue;
            }

            if (dryRun) {
                item.setStatus("PLANNED");
                item.setMessage("待重命名");
                result.getItems().add(item);
                continue;
            }

            try {
                Path targetPath = uploadDir.resolve(newName).normalize();
                if (!targetPath.startsWith(uploadDir)) {
                    throw new IOException("非法目标文件名");
                }
                Files.move(sourcePath, targetPath);
                updateReferencesAfterRename(oldName, newName);
                item.setStatus("RENAMED");
                item.setMessage("重命名成功");
                result.setRenamed(result.getRenamed() + 1);
            } catch (IOException ex) {
                item.setStatus("FAILED");
                item.setMessage("重命名失败: " + ex.getMessage());
                result.setFailed(result.getFailed() + 1);
            }
            result.getItems().add(item);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String renameEcommerceFileName(String oldFileName, String newFileName) {
        if (!StringUtils.hasText(oldFileName) || !StringUtils.hasText(newFileName)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片名称无效");
        }
        String oldKey = extractFileName(oldFileName);
        String desired = normalizeNewFileName(newFileName);
        if (oldKey.equalsIgnoreCase(desired)) {
            return oldKey;
        }
        if (!isImageFileName(desired)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 JPG、PNG、WebP、GIF 图片");
        }

        Path uploadDir = resolveZoneDir(ZONE_ECOMMERCE);
        Set<String> reservedNames = loadReservedFileNames(uploadDir);
        String nextName = EcEcommerceImageNameSupport.allocateUniqueFileName(desired, reservedNames);

        Path source = uploadDir.resolve(oldKey).normalize();
        if (!source.startsWith(uploadDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
        }
        if (!Files.isRegularFile(source)) {
            updateReferencesAfterRename(oldKey, nextName);
            return nextName;
        }

        Path target = uploadDir.resolve(nextName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
        }
        try {
            Files.move(source, target);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "重命名失败");
        }
        updateReferencesAfterRename(oldKey, nextName);
        return nextName;
    }

    private Set<String> loadReservedFileNames(Path uploadDir) {
        Set<String> reservedNames = new HashSet<>();
        if (!Files.isDirectory(uploadDir)) {
            return reservedNames;
        }
        try (Stream<Path> stream = Files.list(uploadDir)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .forEach(reservedNames::add);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "读取电商图片目录失败");
        }
        return reservedNames;
    }

    private void mergeRenamePlan(
            Map<String, String> renamePlans,
            Map<String, String> planSources,
            String oldName,
            String newName,
            String source
    ) {
        String existing = renamePlans.get(oldName);
        if (existing == null) {
            renamePlans.put(oldName, newName);
            planSources.put(oldName, source);
            return;
        }
        if (!existing.equalsIgnoreCase(newName)) {
            log.warn("图片 {} 存在冲突命名方案: {} vs {}", oldName, existing, newName);
        }
    }

    private ImageSpaceCategoryNodeVO categoryNode(String id, String label, Long spuId) {
        ImageSpaceCategoryNodeVO node = new ImageSpaceCategoryNodeVO();
        node.setId(id);
        node.setLabel(label);
        node.setSpuId(spuId);
        return node;
    }

    private String normalizeZone(String zone) {
        if (!StringUtils.hasText(zone)) {
            return ZONE_ECOMMERCE;
        }
        String key = zone.trim().toUpperCase(Locale.ROOT);
        if (!ZONE_ECOMMERCE.equals(key) && !ZONE_NOTEBOOK.equals(key)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的分区");
        }
        return key;
    }

    private Path resolveZoneDir(String zoneKey) {
        return switch (zoneKey) {
            case ZONE_ECOMMERCE -> StoragePathSupport.resolveUploadBasePath(ecommerceUploadPath);
            case ZONE_NOTEBOOK -> StoragePathSupport.resolveUploadBasePath(notebookImagesPath);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的分区");
        };
    }

    private List<ImageFile> listZoneImageFiles(String zoneKey) {
        Path dir = resolveZoneDir(zoneKey);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<ImageFile> files = new ArrayList<>();
        boolean excludeImports = ZONE_ECOMMERCE.equals(zoneKey);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                if (excludeImports && path.toString().contains(dir.resolve("imports").toString())) {
                    return;
                }
                String fileName = path.getFileName().toString();
                if (!isImageFileName(fileName)) {
                    return;
                }
                try {
                    String relativePath = dir.relativize(path).toString().replace('\\', '/');
                    LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            ZoneId.systemDefault()
                    );
                    files.add(new ImageFile(zoneKey, path, fileName, relativePath, Files.size(path), modifiedAt));
                } catch (IOException ignored) {
                    // skip
                }
            });
        } catch (IOException ex) {
            log.warn("读取图片目录失败: {}", ex.getMessage());
        }
        files.sort(Comparator.comparing(ImageFile::modifiedAt).reversed());
        return files;
    }

    private ImageFile resolveImageFile(String zoneKey, String relativePath) {
        Path dir = resolveZoneDir(zoneKey);
        Path path = dir.resolve(relativePath.trim()).normalize();
        if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "图片不存在");
        }
        try {
            String fileName = path.getFileName().toString();
            LocalDateTime modifiedAt = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    ZoneId.systemDefault()
            );
            return new ImageFile(
                    zoneKey,
                    path,
                    fileName,
                    dir.relativize(path).toString().replace('\\', '/'),
                    Files.size(path),
                    modifiedAt
            );
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "读取图片失败");
        }
    }

    private boolean matchesKeyword(ImageFile file, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return file.fileName().toLowerCase(Locale.ROOT).contains(keyword)
                || file.relativePath().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesCategory(String zoneKey, String categoryId, ImageFile file, SpuIndex spuIndex) {
        if (CAT_ALL.equals(categoryId)) {
            return true;
        }
        if (ZONE_NOTEBOOK.equals(zoneKey)) {
            return true;
        }
        if (CAT_UNCATEGORIZED.equals(categoryId)) {
            return !spuIndex.spuLinkedFileNames().contains(file.fileName());
        }
        if (categoryId.startsWith(SPU_PREFIX)) {
            long spuId;
            try {
                spuId = Long.parseLong(categoryId.substring(SPU_PREFIX.length()));
            } catch (NumberFormatException ex) {
                return false;
            }
            Set<String> names = spuIndex.filesBySpuId().getOrDefault(spuId, Set.of());
            return names.contains(file.fileName());
        }
        return true;
    }

    private ImageSpaceImageItemVO toListItem(ImageFile file, ReferenceIndex index, SpuIndex spuIndex) {
        ImageSpaceImageItemVO vo = new ImageSpaceImageItemVO();
        vo.setZone(file.zone());
        vo.setFileName(file.fileName());
        vo.setRelativePath(file.relativePath());
        vo.setSizeBytes(file.sizeBytes());
        vo.setModifiedAt(file.modifiedAt().format(TIME_FMT));
        ReferenceInfo ref = index.byFileName().getOrDefault(file.fileName(), ReferenceInfo.empty());
        vo.setReferenceCount(ref.count());
        vo.setLinkedSpuNames(new ArrayList<>(spuIndex.spuNamesByFileName().getOrDefault(file.fileName(), Set.of())));
        return vo;
    }

    private ImageSpaceImageDetailVO toDetail(ImageFile file, ReferenceIndex index, SpuIndex spuIndex) {
        ImageSpaceImageDetailVO vo = new ImageSpaceImageDetailVO();
        vo.setZone(file.zone());
        vo.setFileName(file.fileName());
        vo.setRelativePath(file.relativePath());
        vo.setSizeBytes(file.sizeBytes());
        vo.setModifiedAt(file.modifiedAt().format(TIME_FMT));
        ReferenceInfo ref = index.byFileName().getOrDefault(file.fileName(), ReferenceInfo.empty());
        vo.setReferenceCount(ref.count());
        vo.setLinkedSpuNames(new ArrayList<>(spuIndex.spuNamesByFileName().getOrDefault(file.fileName(), Set.of())));
        vo.setReferenceHints(new ArrayList<>(ref.hints()));
        return vo;
    }

    private String normalizeNewFileName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请输入图片名称");
        }
        String trimmed = value.trim().replace('\\', '/');
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private boolean isImageFileName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private SpuIndex buildSpuIndex() {
        Map<Long, String> productNames = new LinkedHashMap<>();
        Map<Long, Set<String>> filesBySpuId = new HashMap<>();
        Map<String, Set<String>> spuNamesByFileName = new HashMap<>();
        Set<String> spuLinkedFileNames = new HashSet<>();

        List<EcProduct> products = ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                .select(EcProduct::getId, EcProduct::getName, EcProduct::getImageName));
        for (EcProduct product : products) {
            if (product.getId() == null) {
                continue;
            }
            productNames.put(product.getId(), product.getName());
            if (StringUtils.hasText(product.getImageName())) {
                linkSpuFile(filesBySpuId, spuNamesByFileName, spuLinkedFileNames,
                        product.getId(), product.getName(), product.getImageName().trim());
            }
        }

        List<EcSku> skus = ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                .select(EcSku::getProductId, EcSku::getImageName)
                .isNotNull(EcSku::getImageName));
        for (EcSku sku : skus) {
            if (sku.getProductId() == null || !StringUtils.hasText(sku.getImageName())) {
                continue;
            }
            String spuName = productNames.getOrDefault(sku.getProductId(), "SPU#" + sku.getProductId());
            linkSpuFile(filesBySpuId, spuNamesByFileName, spuLinkedFileNames,
                    sku.getProductId(), spuName, sku.getImageName().trim());
        }

        return new SpuIndex(filesBySpuId, spuNamesByFileName, spuLinkedFileNames);
    }

    private void linkSpuFile(
            Map<Long, Set<String>> filesBySpuId,
            Map<String, Set<String>> spuNamesByFileName,
            Set<String> spuLinkedFileNames,
            Long spuId,
            String spuName,
            String fileName
    ) {
        filesBySpuId.computeIfAbsent(spuId, key -> new LinkedHashSet<>()).add(fileName);
        spuNamesByFileName.computeIfAbsent(fileName, key -> new LinkedHashSet<>()).add(spuName);
        spuLinkedFileNames.add(fileName);
    }

    private ReferenceIndex buildReferenceIndex() {
        Map<String, ReferenceInfo> map = new HashMap<>();

        ecProductMapper.selectList(new LambdaQueryWrapper<EcProduct>()
                        .select(EcProduct::getName, EcProduct::getImageName)
                        .isNotNull(EcProduct::getImageName))
                .forEach(row -> addRef(map, row.getImageName(), "商品主图 · " + row.getName()));

        ecSkuMapper.selectList(new LambdaQueryWrapper<EcSku>()
                        .select(EcSku::getSkuCode, EcSku::getImageName)
                        .isNotNull(EcSku::getImageName))
                .forEach(row -> addRef(map, row.getImageName(), "SKU 图片 · " + row.getSkuCode()));

        ecCartonMapper.selectList(new LambdaQueryWrapper<EcCarton>()
                        .select(EcCarton::getName, EcCarton::getPreviewImage)
                        .isNotNull(EcCarton::getPreviewImage))
                .forEach(row -> addRef(map, row.getPreviewImage(), "纸箱预览 · " + row.getName()));

        ecPlatformMapper.selectList(new LambdaQueryWrapper<EcPlatform>()
                        .select(EcPlatform::getName, EcPlatform::getAvatarUrl)
                        .isNotNull(EcPlatform::getAvatarUrl))
                .forEach(row -> addRef(map, row.getAvatarUrl(), "平台头像 · " + row.getName()));

        ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                        .select(EcShop::getName, EcShop::getAvatarUrl)
                        .isNotNull(EcShop::getAvatarUrl))
                .forEach(row -> addRef(map, row.getAvatarUrl(), "店铺头像 · " + row.getName()));

        ecExpressStationMapper.selectList(new LambdaQueryWrapper<EcExpressStation>()
                        .select(EcExpressStation::getName, EcExpressStation::getAvatarUrl)
                        .isNotNull(EcExpressStation::getAvatarUrl))
                .forEach(row -> addRef(map, row.getAvatarUrl(), "快递头像 · " + row.getName()));

        Path notesDir = StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot()).resolve("notes");
        if (Files.isDirectory(notesDir)) {
            try (Stream<Path> stream = Files.list(notesDir)) {
                for (Path file : stream.filter(path -> path.toString().endsWith(".html")).toList()) {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        Matcher matcher = NOTEBOOK_IMAGE_REF_PATTERN.matcher(content);
                        while (matcher.find()) {
                            addRef(map, matcher.group(1), "笔记正文 · " + file.getFileName());
                        }
                    } catch (IOException ex) {
                        log.debug("扫描笔记图片引用失败: {}", file);
                    }
                }
            } catch (IOException ex) {
                log.warn("读取笔记正文目录失败: {}", ex.getMessage());
            }
        }

        return new ReferenceIndex(map);
    }

    private void addRef(Map<String, ReferenceInfo> map, String rawValue, String hint) {
        String fileName = extractFileName(rawValue);
        if (!StringUtils.hasText(fileName)) {
            return;
        }
        map.compute(fileName, (key, current) -> {
            ReferenceInfo info = current == null ? ReferenceInfo.empty() : current;
            return info.add(hint);
        });
    }

    private String extractFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private void updateReferencesAfterRename(String oldName, String newName) {
        ecProductMapper.update(null, new LambdaUpdateWrapper<EcProduct>()
                .set(EcProduct::getImageName, newName)
                .eq(EcProduct::getImageName, oldName));

        ecSkuMapper.update(null, new LambdaUpdateWrapper<EcSku>()
                .set(EcSku::getImageName, newName)
                .eq(EcSku::getImageName, oldName));

        ecCartonMapper.update(null, new LambdaUpdateWrapper<EcCarton>()
                .set(EcCarton::getPreviewImage, newName)
                .eq(EcCarton::getPreviewImage, oldName));

        updatePlatformAvatars(oldName, newName);
        updateShopAvatars(oldName, newName);
        updateExpressAvatars(oldName, newName);

        replaceNotebookContentImageRefs(oldName, newName);
    }

    private void updatePlatformAvatars(String oldName, String newName) {
        ecPlatformMapper.selectList(new LambdaQueryWrapper<EcPlatform>()
                        .select(EcPlatform::getId, EcPlatform::getAvatarUrl)
                        .isNotNull(EcPlatform::getAvatarUrl))
                .forEach(row -> {
                    if (oldName.equals(extractFileName(row.getAvatarUrl()))) {
                        EcPlatform patch = new EcPlatform();
                        patch.setId(row.getId());
                        patch.setAvatarUrl(newName);
                        ecPlatformMapper.updateById(patch);
                    }
                });
    }

    private void updateShopAvatars(String oldName, String newName) {
        ecShopMapper.selectList(new LambdaQueryWrapper<EcShop>()
                        .select(EcShop::getId, EcShop::getAvatarUrl)
                        .isNotNull(EcShop::getAvatarUrl))
                .forEach(row -> {
                    if (oldName.equals(extractFileName(row.getAvatarUrl()))) {
                        EcShop patch = new EcShop();
                        patch.setId(row.getId());
                        patch.setAvatarUrl(newName);
                        ecShopMapper.updateById(patch);
                    }
                });
    }

    private void updateExpressAvatars(String oldName, String newName) {
        ecExpressStationMapper.selectList(new LambdaQueryWrapper<EcExpressStation>()
                        .select(EcExpressStation::getId, EcExpressStation::getAvatarUrl)
                        .isNotNull(EcExpressStation::getAvatarUrl))
                .forEach(row -> {
                    if (oldName.equals(extractFileName(row.getAvatarUrl()))) {
                        EcExpressStation patch = new EcExpressStation();
                        patch.setId(row.getId());
                        patch.setAvatarUrl(newName);
                        ecExpressStationMapper.updateById(patch);
                    }
                });
    }

    private void replaceNotebookContentImageRefs(String oldName, String newName) {
        Path notesDir = StoragePathSupport.resolveUploadBasePath(noteStorageProperties.getLocalRoot()).resolve("notes");
        if (!Files.isDirectory(notesDir)) {
            return;
        }
        String oldFragment = "notebook/images/" + oldName;
        String newFragment = "notebook/images/" + newName;
        try (Stream<Path> stream = Files.list(notesDir)) {
            for (Path file : stream.filter(path -> path.toString().endsWith(".html")).toList()) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    if (!content.contains(oldName)) {
                        continue;
                    }
                    String replaced = content
                            .replace(oldFragment, newFragment)
                            .replace("notebook/images\\" + oldName, newFragment);
                    if (!replaced.equals(content)) {
                        Files.writeString(file, replaced, StandardCharsets.UTF_8);
                    }
                } catch (IOException ex) {
                    log.warn("更新笔记正文图片引用失败 {}: {}", file, ex.getMessage());
                }
            }
        } catch (IOException ex) {
            log.warn("扫描笔记正文失败: {}", ex.getMessage());
        }
    }

    private record ImageFile(
            String zone,
            Path path,
            String fileName,
            String relativePath,
            long sizeBytes,
            LocalDateTime modifiedAt
    ) {
    }

    private record ReferenceInfo(int count, List<String> hints) {
        static ReferenceInfo empty() {
            return new ReferenceInfo(0, new ArrayList<>());
        }

        ReferenceInfo add(String hint) {
            List<String> nextHints = new ArrayList<>(hints);
            if (!nextHints.contains(hint)) {
                nextHints.add(hint);
            }
            return new ReferenceInfo(count + 1, nextHints);
        }
    }

    private record ReferenceIndex(Map<String, ReferenceInfo> byFileName) {
    }

    private record SpuIndex(
            Map<Long, Set<String>> filesBySpuId,
            Map<String, Set<String>> spuNamesByFileName,
            Set<String> spuLinkedFileNames
    ) {
    }
}
