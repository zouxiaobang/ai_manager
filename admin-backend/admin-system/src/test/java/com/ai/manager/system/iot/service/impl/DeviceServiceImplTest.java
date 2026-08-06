package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.vo.DeviceVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.websocket.DeviceWsHandler;
import com.ai.manager.system.iot.websocket.WsSessionRegistry;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private IotDeviceMapper iotDeviceMapper;

    @Mock
    private WsSessionRegistry wsSessionRegistry;

    @Mock
    private DeviceWsHandler deviceWsHandler;

    private DeviceServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotDevice.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeviceServiceImpl(iotDeviceMapper, wsSessionRegistry, deviceWsHandler);
    }

    private IotDevice entity(Long id, String mac, String status) {
        IotDevice d = new IotDevice();
        d.setId(id);
        d.setMac(mac);
        d.setUuid("uuid-" + mac);
        d.setStatus(status);
        d.setOtaState("IDLE");
        d.setDeleted(0);
        return d;
    }

    @Test
    void listDevices_shouldMapToVOWithOnlineFlag() {
        when(iotDeviceMapper.selectList(any())).thenReturn(List.of(entity(1L, "aabbccdd", "ONLINE")));
        when(wsSessionRegistry.isOnline("aabbccdd")).thenReturn(true);

        List<DeviceVO> result = service.listDevices();

        assertThat(result).hasSize(1);
        DeviceVO vo = result.get(0);
        assertThat(vo.getMac()).isEqualTo("aabbccdd");
        assertThat(vo.getOnline()).isTrue();
        assertThat(vo.getOtaState()).isEqualTo("IDLE");
    }

    @Test
    void getDevice_shouldReturnVO() {
        when(iotDeviceMapper.selectById(1L)).thenReturn(entity(1L, "aabbccdd", "BOUND"));

        DeviceVO vo = service.getDevice(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getMac()).isEqualTo("aabbccdd");
    }

    @Test
    void getDevice_whenMissing_shouldThrow() {
        when(iotDeviceMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDevice(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("设备不存在");
    }

    @Test
    void bind_whenNewDevice_shouldInsertAndSignToken() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(null);
        DeviceBindRequest req = new DeviceBindRequest();
        req.setMac("AA:BB:CC:DD");
        req.setModel("supermini-c3");

        DeviceVO vo = service.bind(req);

        assertThat(vo.getMac()).isEqualTo("aabbccdd");
        assertThat(vo.getStatus()).isEqualTo("BOUND");
        assertThat(vo.getUuid()).isNotBlank();
        verify(iotDeviceMapper).insert(any(IotDevice.class));
    }

    @Test
    void bind_whenExisting_shouldUpdate() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(entity(1L, "aabbccdd", "BOUND"));
        DeviceBindRequest req = new DeviceBindRequest();
        req.setMac("aabbccdd");
        req.setChip("esp32c3");

        DeviceVO vo = service.bind(req);

        assertThat(vo.getChip()).isEqualTo("esp32c3");
        verify(iotDeviceMapper).updateById(any(IotDevice.class));
        verify(iotDeviceMapper, never()).insert(any(IotDevice.class));
    }

    @Test
    void updateStatus_shouldPersist() {
        when(iotDeviceMapper.selectById(1L)).thenReturn(entity(1L, "aabbccdd", "BOUND"));

        DeviceVO vo = service.updateStatus(1L, "OFFLINE");

        assertThat(vo.getStatus()).isEqualTo("OFFLINE");
        verify(iotDeviceMapper).updateById(any(IotDevice.class));
    }

    @Test
    void updateStatus_whenMissing_shouldThrow() {
        when(iotDeviceMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStatus(99L, "ONLINE"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reboot_whenOnline_shouldSendCommand() {
        when(iotDeviceMapper.selectById(1L)).thenReturn(entity(1L, "aabbccdd", "ONLINE"));
        when(wsSessionRegistry.isOnline("aabbccdd")).thenReturn(true);

        service.reboot(1L);

        verify(deviceWsHandler).sendSystem("aabbccdd", "reboot");
    }

    @Test
    void reboot_whenOffline_shouldSkipSend() {
        when(iotDeviceMapper.selectById(1L)).thenReturn(entity(1L, "aabbccdd", "OFFLINE"));
        when(wsSessionRegistry.isOnline("aabbccdd")).thenReturn(false);

        service.reboot(1L);

        verify(deviceWsHandler, never()).sendSystem(any(), any());
    }

    @Test
    void findByMac_shouldNormalize() {
        when(iotDeviceMapper.selectOne(any())).thenReturn(entity(1L, "aabbccdd", "BOUND"));

        IotDevice found = service.findByMac("AA:BB:CC:DD");

        assertThat(found).isNotNull();
        assertThat(found.getMac()).isEqualTo("aabbccdd");
    }

    @Test
    void findByMac_whenBlank_shouldReturnNull() {
        assertThat(service.findByMac("  ")).isNull();
    }
}
