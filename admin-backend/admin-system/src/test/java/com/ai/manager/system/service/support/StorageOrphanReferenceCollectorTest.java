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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * StorageOrphanReferenceCollector 单元测试
 * mock 各业务 mapper + 临时目录验证笔记正文引用收集。
 */
@ExtendWith(MockitoExtension.class)
class StorageOrphanReferenceCollectorTest {

    @Mock private EcProductMapper ecProductMapper;
    @Mock private EcSkuMapper ecSkuMapper;
    @Mock private EcCartonMapper ecCartonMapper;
    @Mock private EcPlatformMapper ecPlatformMapper;
    @Mock private EcShopMapper ecShopMapper;
    @Mock private EcExpressStationMapper ecExpressStationMapper;
    @Mock private NbNoteMapper nbNoteMapper;
    @Mock private SysImportBatchMapper sysImportBatchMapper;

    @TempDir
    Path tempDir;

    private NoteStorageProperties props;
    private StorageOrphanReferenceCollector collector;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus Lambda 缓存初始化：LambdaQueryWrapper 列解析依赖 TableInfo
        initTable(EcProduct.class);
        initTable(EcSku.class);
        initTable(EcCarton.class);
        initTable(EcPlatform.class);
        initTable(EcShop.class);
        initTable(EcExpressStation.class);
        initTable(NbNote.class);
        initTable(SysImportBatch.class);

        // Properties 非 mock，手动构造 collector
        props = new NoteStorageProperties();
        collector = new StorageOrphanReferenceCollector(
                ecProductMapper, ecSkuMapper, ecCartonMapper, ecPlatformMapper,
                ecShopMapper, ecExpressStationMapper, nbNoteMapper, sysImportBatchMapper, props);
    }

    private static void initTable(Class<?> clazz) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), clazz);
    }

    @Test
    void collectEcommerceImageReferences_shouldTrimImageNamesAndExtractAvatarFileNames() {
        EcProduct product = new EcProduct();
        product.setImageName(" a.png ");
        EcCarton carton = new EcCarton();
        carton.setPreviewImage("c.png");
        EcPlatform platform = new EcPlatform();
        platform.setAvatarUrl("uploads/ecommerce/platforms/logo.png");
        EcShop shop = new EcShop();
        shop.setAvatarUrl("uploads/ecommerce/shops/shop.png");
        when(ecProductMapper.selectList(any())).thenReturn(List.of(product));
        when(ecSkuMapper.selectList(any())).thenReturn(List.of());
        when(ecCartonMapper.selectList(any())).thenReturn(List.of(carton));
        when(ecPlatformMapper.selectList(any())).thenReturn(List.of(platform));
        when(ecShopMapper.selectList(any())).thenReturn(List.of(shop));
        when(ecExpressStationMapper.selectList(any())).thenReturn(List.of());

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_ECOMMERCE_IMAGES);

        assertThat(refs).contains("a.png", "c.png", "logo.png", "shop.png");
    }

    @Test
    void collectEcommerceImageReferences_shouldDropEmptyAfterExtract() {
        EcPlatform platform = new EcPlatform();
        platform.setAvatarUrl("   ");
        EcExpressStation station = new EcExpressStation();
        station.setAvatarUrl(null);
        when(ecProductMapper.selectList(any())).thenReturn(List.of());
        when(ecSkuMapper.selectList(any())).thenReturn(List.of());
        when(ecCartonMapper.selectList(any())).thenReturn(List.of());
        when(ecPlatformMapper.selectList(any())).thenReturn(List.of(platform));
        when(ecShopMapper.selectList(any())).thenReturn(List.of());
        when(ecExpressStationMapper.selectList(any())).thenReturn(List.of(station));

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_ECOMMERCE_IMAGES);

        assertThat(refs).doesNotContain("");
        assertThat(refs).isEmpty();
    }

    @Test
    void collectNoteContentReferences_shouldBuildHtmlAndMetaJsonRefs() {
        NbNote note1 = new NbNote();
        note1.setId(101L);
        NbNote note2 = new NbNote();
        note2.setId(202L);
        when(nbNoteMapper.selectList(any())).thenReturn(List.of(note1, note2));

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_NOTEBOOK_CONTENT);

        assertThat(refs).contains("101.html", "101.meta.json", "202.html", "202.meta.json");
    }

    @Test
    void collectImportFileReferences_shouldExtractFileNameFromPath() {
        SysImportBatch batch = new SysImportBatch();
        batch.setFilePath("uploads/imports/sales-orders/2026-08.xlsx");
        when(sysImportBatchMapper.selectList(any())).thenReturn(List.of(batch));

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_IMPORT_FILES);

        assertThat(refs).contains("2026-08.xlsx");
    }

    @Test
    void collectReferences_shouldReturnEmptyForUnknownZone() {
        assertThat(collector.collectReferences("UNKNOWN")).isEmpty();
    }

    @Test
    void collectNotebookImageReferences_shouldParseHtmlForImageRefs() throws Exception {
        // 临时目录模拟笔记正文目录：<localRoot>/notes/*.html
        Path notesDir = Files.createDirectories(tempDir.resolve("notes"));
        Path noteFile = notesDir.resolve("note1.html");
        Files.writeString(noteFile,
                "<img src=\"notebook/images/photo_1.png\" /><img src=\"notebook/images/dir/photo_2.jpg\">");
        props.setLocalRoot(tempDir.toString());

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_NOTEBOOK_IMAGES);

        assertThat(refs).contains("photo_1.png", "dir/photo_2.jpg");
    }

    @Test
    void collectNotebookImageReferences_shouldSkipWhenNotesDirMissing() {
        props.setLocalRoot(tempDir.toString());

        Set<String> refs = collector.collectReferences(StorageZoneViewAssembler.ZONE_NOTEBOOK_IMAGES);

        assertThat(refs).isEmpty();
    }
}
