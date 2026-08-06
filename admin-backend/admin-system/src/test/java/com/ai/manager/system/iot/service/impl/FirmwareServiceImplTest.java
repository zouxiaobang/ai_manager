package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;

import java.io.Serializable;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.entity.IotOtaRecord;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotFirmwareMapper;
import com.ai.manager.system.iot.mapper.IotOtaRecordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Mock
    private IotDeviceMapper iotDeviceMapper;

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
        service = new FirmwareServiceImpl(iotFirmwareMapper, iotOtaRecordMapper, iotDeviceMapper, props);
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
    void forceUpgrade_shouldSetForceAndPublish() {
        when(iotFirmwareMapper.selectById(1L)).thenReturn(entity(1L, "2.2.1", "DRAFT"));

        FirmwareVO vo = service.forceUpgrade(1L);

        assertThat(vo.getStatus()).isEqualTo("PUBLISHED");
        assertThat(vo.getForce()).isEqualTo(1);
        verify(iotFirmwareMapper).updateById(any(IotFirmware.class));
    }

    @Test
    void forceUpgrade_whenMissing_shouldThrow() {
        when(iotFirmwareMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.forceUpgrade(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_whenDraft_shouldSoftDeleteAndRemoveFile() throws Exception {
        Path fwFile = tempDir.resolve("delete.bin");
        Files.write(fwFile, new byte[]{1, 2});
        IotFirmware draft = entity(1L, "2.2.1", "DRAFT");
        draft.setFilePath(fwFile.toString());
        when(iotFirmwareMapper.selectById(1L)).thenReturn(draft);

        service.delete(1L);

        verify(iotFirmwareMapper).deleteById(1L);
        assertThat(Files.exists(fwFile)).isFalse();
    }

    @Test
    void delete_whenPublished_shouldThrow() {
        when(iotFirmwareMapper.selectById(1L)).thenReturn(entity(1L, "2.2.1", "PUBLISHED"));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已发布固件不可删除");
        verify(iotFirmwareMapper, never()).deleteById(any(Serializable.class));
    }

    @Test
    void listFirmwares_shouldPageAndMapVO() {
        Page<IotFirmware> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(entity(1L, "2.2.1", "PUBLISHED")));
        dbPage.setTotal(1);
        when(iotFirmwareMapper.selectPage(any(), any())).thenReturn(dbPage);

        PageResult<FirmwareVO> result = service.listFirmwares(1L, 20L, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getVersion()).isEqualTo("2.2.1");
        assertThat(result.getTotal()).isEqualTo(1);
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
    void listOtaRecords_shouldPageAndEnrich() {
        IotOtaRecord r = new IotOtaRecord();
        r.setId(1L);
        r.setDeviceId(10L);
        r.setFirmwareId(20L);
        r.setState("UPGRADING");
        r.setProgress(30);
        r.setDeleted(0);
        Page<IotOtaRecord> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(r));
        dbPage.setTotal(1);
        when(iotOtaRecordMapper.selectPage(any(), any())).thenReturn(dbPage);
        IotDevice device = new IotDevice();
        device.setId(10L);
        device.setMac("aabbccdd");
        when(iotDeviceMapper.selectById(10L)).thenReturn(device);
        when(iotFirmwareMapper.selectById(20L)).thenReturn(entity(1L, "2.2.1", "PUBLISHED"));

        PageResult<OtaRecordVO> result = service.listOtaRecords(1L, 20L);

        assertThat(result.getRecords()).hasSize(1);
        OtaRecordVO vo = result.getRecords().get(0);
        assertThat(vo.getState()).isEqualTo("UPGRADING");
        assertThat(vo.getProgress()).isEqualTo(30);
        assertThat(vo.getDeviceName()).isEqualTo("aabbccdd");
        assertThat(vo.getFirmwareVersion()).isEqualTo("2.2.1");
    }
}
