package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.dto.DeviceActivateRequest;
import com.ai.manager.system.iot.domain.dto.DeviceActivateResult;
import com.ai.manager.system.iot.domain.dto.OtaCheckRequest;
import com.ai.manager.system.iot.domain.dto.OtaCheckResponse;
import com.ai.manager.system.iot.domain.dto.OtaStatusRequest;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.entity.IotOtaRecord;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotOtaRecordMapper;
import com.ai.manager.system.iot.service.FirmwareService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtaServiceImplTest {

    private static final String SECRET = "test-secret";

    @Mock
    private IotDeviceMapper iotDeviceMapper;

    @Mock
    private IotOtaRecordMapper iotOtaRecordMapper;

    @Mock
    private FirmwareService firmwareService;

    private IotProperties props;

    private OtaServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotDevice.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotFirmware.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotOtaRecord.class);
    }

    @BeforeEach
    void setUp() {
        props = new IotProperties();
        props.setWsUrl("ws://host/ws/device");
        props.setOtaBaseUrl("http://host/api/iot/ota/download");
        props.setProtocolVersion(3);
        props.setActivationSecret(SECRET);
        props.setTimezoneOffsetMinutes(480);
        service = new OtaServiceImpl(iotDeviceMapper, iotOtaRecordMapper, firmwareService, props);
    }

    private IotDevice device(Long id, String mac, String version) {
        IotDevice d = new IotDevice();
        d.setId(id);
        d.setMac(mac);
        d.setFirmwareVersion(version);
        d.setStatus("ACTIVATED");
        d.setWsToken("tok-123");
        d.setOtaState("IDLE");
        d.setDeleted(0);
        return d;
    }

    private IotFirmware firmware(Long id, String version, int force) {
        IotFirmware f = new IotFirmware();
        f.setId(id);
        f.setVersion(version);
        f.setForceUpgrade(force);
        f.setStatus("PUBLISHED");
        f.setDeleted(0);
        return f;
    }

    private OtaCheckRequest checkRequest(String mac, String version) {
        OtaCheckRequest req = new OtaCheckRequest();
        req.setMac(mac);
        req.setUuid("uuid-1");
        req.setClientId("client-1");
        req.setModel("supermini-c3");
        req.setChip("esp32c3");
        req.setFirmwareVersion(version);
        return req;
    }

    @Test
    void check_whenNewDevice_shouldRegisterAndReturnConfig() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(null);
        when(firmwareService.latestPublished()).thenReturn(null);

        OtaCheckResponse resp = service.check(checkRequest("AA:BB:CC:DD", "2.0.0"));

        assertThat(resp.getWebsocket().getUrl()).isEqualTo("ws://host/ws/device");
        assertThat(resp.getWebsocket().getToken()).isNotBlank();
        assertThat(resp.getWebsocket().getVersion()).isEqualTo(3);
        assertThat(resp.getMqtt().getClientId()).startsWith("iot-");
        assertThat(resp.getServerTime().getTimezoneOffset()).isEqualTo(28800);
        assertThat(resp.getFirmware()).isNull();
        verify(iotDeviceMapper).insert(any(IotDevice.class));
    }

    @Test
    void check_whenNewerFirmware_shouldIncludeAndRecordOta() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.1", 0));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", "2.0.0"));

        assertThat(resp.getFirmware()).isNotNull();
        assertThat(resp.getFirmware().getVersion()).isEqualTo("2.2.1");
        assertThat(resp.getFirmware().getUrl()).isEqualTo("http://host/api/iot/ota/download/9");
        assertThat(resp.getFirmware().isForce()).isFalse();
        verify(iotOtaRecordMapper).insert(any(com.ai.manager.system.iot.domain.entity.IotOtaRecord.class));
    }

    @Test
    void check_whenSameVersion_shouldNotIncludeFirmware() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.2.1"));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.1", 0));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", "2.2.1"));

        assertThat(resp.getFirmware()).isNull();
        verify(iotOtaRecordMapper, never()).insert(any(com.ai.manager.system.iot.domain.entity.IotOtaRecord.class));
    }

    @Test
    void check_whenForceFirmwareDifferentVersion_shouldInclude() {
        // force + 设备版本不同（更旧）→ 强制升级
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.2.1"));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.0", 1));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", "2.2.1"));

        assertThat(resp.getFirmware()).isNotNull();
        assertThat(resp.getFirmware().isForce()).isTrue();
        verify(iotOtaRecordMapper).insert(any(com.ai.manager.system.iot.domain.entity.IotOtaRecord.class));
    }

    @Test
    void check_whenForceFirmwareSameVersion_shouldNotInclude() {
        // force + 版本完全相同：设备已运行该发布版本，不再下发，避免每次 check 无限循环重下
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.2.1"));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.1", 1));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", "2.2.1"));

        assertThat(resp.getFirmware()).isNull();
        verify(iotOtaRecordMapper, never()).insert(any(com.ai.manager.system.iot.domain.entity.IotOtaRecord.class));
    }

    @Test
    void check_whenForceFirmwareDeviceNewer_shouldStillInclude() {
        // force 允许降级：设备已领先（2.2.2）但后端 force 下推 2.2.0，仍强制刷回
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.2.2"));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.0", 1));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", "2.2.2"));

        assertThat(resp.getFirmware()).isNotNull();
        assertThat(resp.getFirmware().isForce()).isTrue();
    }

    @Test
    void check_whenForceFirmwareDeviceEmptyVersion_shouldInclude() {
        // 设备没上报版本：force 兜底刷写
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", null));
        when(firmwareService.latestPublished()).thenReturn(firmware(9L, "2.2.1", 1));

        OtaCheckResponse resp = service.check(checkRequest("aabbccdd", null));

        assertThat(resp.getFirmware()).isNotNull();
    }

    @Test
    void activate_withValidSignature_shouldSetActivated() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        String sig = hmac(SECRET, "aabbccdd:nonce-1:1700000000");

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setMac("aabbccdd");
        req.setNonce("nonce-1");
        req.setTimestamp(1700000000L);
        req.setSignature(sig);

        DeviceActivateResult result = service.activate(req);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeviceId()).isEqualTo(1L);
        verify(iotDeviceMapper).updateById(any(IotDevice.class));
    }

    @Test
    void activate_withInvalidSignature_shouldThrow() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setMac("aabbccdd");
        req.setNonce("nonce-1");
        req.setTimestamp(1700000000L);
        req.setSignature("deadbeef");

        assertThatThrownBy(() -> service.activate(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("签名校验失败");
    }

    @Test
    void activate_whenDeviceMissing_shouldThrow() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(null);

        DeviceActivateRequest req = new DeviceActivateRequest();
        req.setMac("aabbccdd");

        assertThatThrownBy(() -> service.activate(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备未注册");
    }

    @Test
    void getDownloadInfo_shouldDelegate() {
        when(firmwareService.downloadInfo(9L)).thenReturn(
                new com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo("2.2.1", "hash", 1, new byte[]{1}));

        assertThat(service.getDownloadInfo(9L).getVersion()).isEqualTo("2.2.1");
        verify(firmwareService).downloadInfo(9L);
    }

    // ---- OTA 状态上报（POST /api/iot/ota/status）----

    @Test
    void reportStatus_whenDownloading_shouldMarkUpgradingWithProgress() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        IotOtaRecord record = otaRecord(7L, 1L, "UPGRADING", 0);
        when(iotOtaRecordMapper.selectOne(any())).thenReturn(record);

        service.reportStatus(statusRequest("aabbccdd", "DOWNLOADING", 42));

        // DOWNLOADING 归并为进行中；进度更新但不落 finished_at
        assertThat(record.getState()).isEqualTo("UPGRADING");
        assertThat(record.getProgress()).isEqualTo(42);
        assertThat(record.getFinishedAt()).isNull();
        verify(iotOtaRecordMapper).updateById(record);
        verify(iotDeviceMapper).updateById(argThat((IotDevice d) -> "UPGRADING".equals(d.getOtaState())));
    }

    @Test
    void reportStatus_whenSuccess_shouldFinishAndSyncDeviceState() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        IotOtaRecord record = otaRecord(7L, 1L, "UPGRADING", 60);
        when(iotOtaRecordMapper.selectOne(any())).thenReturn(record);

        service.reportStatus(statusRequest("aabbccdd", "SUCCESS", 100));

        assertThat(record.getState()).isEqualTo("SUCCESS");
        assertThat(record.getProgress()).isEqualTo(100);
        assertThat(record.getFinishedAt()).isNotNull();
        verify(iotOtaRecordMapper).updateById(record);
        verify(iotDeviceMapper).updateById(argThat((IotDevice d) -> "SUCCESS".equals(d.getOtaState())));
    }

    @Test
    void reportStatus_whenFailed_shouldFinishAsFailed() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        IotOtaRecord record = otaRecord(7L, 1L, "UPGRADING", 35);
        when(iotOtaRecordMapper.selectOne(any())).thenReturn(record);

        service.reportStatus(statusRequest("aabbccdd", "FAILED", 35));

        assertThat(record.getState()).isEqualTo("FAILED");
        assertThat(record.getFinishedAt()).isNotNull();
        verify(iotOtaRecordMapper).updateById(record);
        verify(iotDeviceMapper).updateById(argThat((IotDevice d) -> "FAILED".equals(d.getOtaState())));
    }

    @Test
    void reportStatus_whenDeviceMissing_shouldIgnore() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(null);

        service.reportStatus(statusRequest("deadbeef", "SUCCESS", 100));

        // 陌生 MAC 静默忽略，不建记录、不更新设备
        verify(iotOtaRecordMapper, never()).selectOne(any());
        verify(iotOtaRecordMapper, never()).updateById(any(IotOtaRecord.class));
        verify(iotDeviceMapper, never()).updateById(any(IotDevice.class));
    }

    @Test
    void reportStatus_whenNoRecord_shouldIgnore() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(device(1L, "aabbccdd", "2.0.0"));
        when(iotOtaRecordMapper.selectOne(any())).thenReturn(null);

        service.reportStatus(statusRequest("aabbccdd", "SUCCESS", 100));

        verify(iotOtaRecordMapper, never()).updateById(any(IotOtaRecord.class));
        verify(iotDeviceMapper, never()).updateById(any(IotDevice.class));
    }

    private IotOtaRecord otaRecord(Long id, Long deviceId, String state, int progress) {
        IotOtaRecord r = new IotOtaRecord();
        r.setId(id);
        r.setDeviceId(deviceId);
        r.setState(state);
        r.setProgress(progress);
        r.setDeleted(0);
        return r;
    }

    private OtaStatusRequest statusRequest(String mac, String state, int progress) {
        OtaStatusRequest req = new OtaStatusRequest();
        req.setMac(mac);
        req.setState(state);
        req.setProgress(progress);
        return req;
    }

    private String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
