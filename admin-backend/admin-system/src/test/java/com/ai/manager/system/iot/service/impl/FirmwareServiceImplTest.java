package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.entity.IotOtaRecord;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.mapper.IotFirmwareMapper;
import com.ai.manager.system.iot.mapper.IotOtaRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmwareServiceImplTest {

    @Mock
    private IotFirmwareMapper iotFirmwareMapper;

    @Mock
    private IotOtaRecordMapper iotOtaRecordMapper;

    @TempDir
    Path tempDir;

    private FirmwareServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotFirmware.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotOtaRecord.class);
    }

    @BeforeEach
    void setUp() {
        IotProperties props = new IotProperties();
        props.setOtaDir(tempDir.toString());
        service = new FirmwareServiceImpl(iotFirmwareMapper, iotOtaRecordMapper, props);
    }

    private IotFirmware entity(Long id, String version, String status) {
        IotFirmware f = new IotFirmware();
        f.setId(id);
        f.setVersion(version);
        f.setStatus(status);
        f.setFilePath(tempDir.resolve("f.bin").toString());
        f.setFileHash("hash");
        f.setSize(3L);
        f.setForceUpgrade(0);
        f.setDeleted(0);
        return f;
    }

    @Test
    void upload_shouldWriteFileAndCreateDraft() throws Exception {
        byte[] content = new byte[]{1, 2, 3};

        FirmwareVO vo = service.upload(content, "2.2.1", "修复", 1);

        assertThat(vo.getVersion()).isEqualTo("2.2.1");
        assertThat(vo.getStatus()).isEqualTo("DRAFT");
        assertThat(vo.getSize()).isEqualTo(3L);
        assertThat(vo.getFileHash()).hasSize(64);
        verify(iotFirmwareMapper).insert(any(IotFirmware.class));
        assertThat(Files.list(tempDir)).isNotEmpty();
    }

    @Test
    void upload_whenEmptyContent_shouldThrow() {
        assertThatThrownBy(() -> service.upload(new byte[0], "2.2.1", null, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固件文件不能为空");
    }

    @Test
    void upload_whenBlankVersion_shouldThrow() {
        assertThatThrownBy(() -> service.upload(new byte[]{1}, "  ", null, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固件版本不能为空");
    }

    @Test
    void publish_shouldSetPublished() {
        when(iotFirmwareMapper.selectById(1L)).thenReturn(entity(1L, "2.2.1", "DRAFT"));

        FirmwareVO vo = service.publish(1L);

        assertThat(vo.getStatus()).isEqualTo("PUBLISHED");
        verify(iotFirmwareMapper).updateById(any(IotFirmware.class));
    }

    @Test
    void publish_whenMissing_shouldThrow() {
        when(iotFirmwareMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.publish(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listFirmwares_shouldMapVO() {
        when(iotFirmwareMapper.selectList(any())).thenReturn(List.of(entity(1L, "2.2.1", "PUBLISHED")));

        List<FirmwareVO> result = service.listFirmwares();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo("2.2.1");
    }

    @Test
    void latestPublished_shouldReturnPublishedFirmware() {
        when(iotFirmwareMapper.selectOne(any())).thenReturn(entity(1L, "2.2.1", "PUBLISHED"));

        IotFirmware latest = service.latestPublished();

        assertThat(latest.getVersion()).isEqualTo("2.2.1");
    }

    @Test
    void downloadInfo_shouldReadFileBytes() throws Exception {
        Files.write(tempDir.resolve("f.bin"), new byte[]{9, 8, 7});
        when(iotFirmwareMapper.selectById(1L)).thenReturn(entity(1L, "2.2.1", "PUBLISHED"));

        FirmwareDownloadInfo info = service.downloadInfo(1L);

        assertThat(info.getVersion()).isEqualTo("2.2.1");
        assertThat(info.getSize()).isEqualTo(3L);
        assertThat(info.getContent()).containsExactly(9, 8, 7);
    }

    @Test
    void downloadInfo_whenFileMissing_shouldThrow() {
        when(iotFirmwareMapper.selectById(1L)).thenReturn(entity(1L, "2.2.1", "PUBLISHED"));

        assertThatThrownBy(() -> service.downloadInfo(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("固件文件不存在");
    }

    @Test
    void listOtaRecords_shouldMapVO() {
        IotOtaRecord r = new IotOtaRecord();
        r.setId(1L);
        r.setDeviceId(10L);
        r.setFirmwareId(20L);
        r.setState("UPGRADING");
        r.setProgress(30);
        r.setDeleted(0);
        when(iotOtaRecordMapper.selectList(any())).thenReturn(List.of(r));

        List<OtaRecordVO> result = service.listOtaRecords();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getState()).isEqualTo("UPGRADING");
        assertThat(result.get(0).getProgress()).isEqualTo(30);
        verify(iotFirmwareMapper, never()).selectById(any());
    }
}
