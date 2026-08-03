package com.ai.manager.system.service.support;

import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcExpressStation;
import com.ai.manager.system.domain.entity.EcPlatform;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.entity.EcShop;
import com.ai.manager.system.domain.entity.EcSku;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.entity.SysImportBatch;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcExpressStationMapper;
import com.ai.manager.system.mapper.EcPlatformMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.ai.manager.system.mapper.EcShopMapper;
import com.ai.manager.system.mapper.EcSkuMapper;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.mapper.SysImportBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 存储中心孤儿引用收集
 *
 * <p>从 {@code StorageCenterServiceImpl} 提取：按分区收集「被业务记录引用的文件名」。
 * 电商图片分区读各业务表图片字段；笔记分区扫描正文 HTML 中的图片引用或笔记文件；
 * 导入文件分区读导入批次路径。供孤儿扫描比对哪些文件是孤立文件。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageOrphanReferenceCollector {

    /** 笔记正文 HTML 中内嵌图片的引用模式（notebook/images 下） */
    private static final Pattern NOTEBOOK_IMAGE_REF_PATTERN = Pattern.compile(
            "notebook/images[/\\\\]([^\"'\\s>?#]+)", Pattern.CASE_INSENSITIVE);

    private final EcProductMapper ecProductMapper;
    private final EcSkuMapper ecSkuMapper;
    private final EcCartonMapper ecCartonMapper;
    private final EcPlatformMapper ecPlatformMapper;
    private final EcShopMapper ecShopMapper;
    private final EcExpressStationMapper ecExpressStationMapper;
    private final NbNoteMapper nbNoteMapper;
    private final SysImportBatchMapper sysImportBatchMapper;
    private final NoteStorageProperties noteStorageProperties;

    /** 分区 → 全部被引用文件名集合（按分区调用对应业务表的引用收集） */
    public Set<String> collectReferences(String zoneKey) {
        return switch (zoneKey) {
            case StorageZoneViewAssembler.ZONE_ECOMMERCE_IMAGES -> collectEcommerceImageReferences();
            case StorageZoneViewAssembler.ZONE_NOTEBOOK_IMAGES -> collectNotebookImageReferences();
            case StorageZoneViewAssembler.ZONE_NOTEBOOK_CONTENT -> collectNoteContentReferences();
            case StorageZoneViewAssembler.ZONE_IMPORT_FILES -> collectImportFileReferences();
            default -> Set.of();
        };
    }

    /** 电商图片分区：收集商品/SKU/纸箱图片名 + 平台/店铺/站点头像文件名 */
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

    /** 笔记图片分区：扫描本地 notes 目录下 HTML 正文，提取内嵌图片引用文件名 */
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

    /** 笔记正文分区：所有未删除笔记的 id 对应 html 与 meta.json 文件名 */
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

    /** 导入文件分区：收集导入批次文件路径的文件名 */
    private Set<String> collectImportFileReferences() {
        Set<String> refs = new HashSet<>();
        sysImportBatchMapper.selectList(new LambdaQueryWrapper<SysImportBatch>()
                        .select(SysImportBatch::getFilePath)
                        .isNotNull(SysImportBatch::getFilePath))
                .forEach(batch -> refs.add(extractFileName(batch.getFilePath())));
        refs.remove("");
        return refs;
    }

    /** 从完整路径/URL 提取文件名，空值返回空串 */
    private String extractFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }
}
