package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotSession;
import com.ai.manager.system.iot.domain.vo.OnlineSessionVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.websocket.DeviceSessionInfo;
import com.ai.manager.system.iot.websocket.WsSessionRegistry;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private IotSessionMapper iotSessionMapper;

    @Mock
    private IotDeviceMapper iotDeviceMapper;

    @Mock
    private WsSessionRegistry wsSessionRegistry;

    private SessionServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotSession.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotDevice.class);
    }

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(iotSessionMapper, iotDeviceMapper, wsSessionRegistry);
    }

    private IotSession session(Long id, String sessionId, int turnCount) {
        IotSession s = new IotSession();
        s.setId(id);
        s.setDeviceId(1L);
        s.setSessionId(sessionId);
        s.setTurnCount(turnCount);
        s.setDeleted(0);
        return s;
    }

    @Test
    void listOnlineSessions_shouldEnrichWithDevice() {
        DeviceSessionInfo info = new DeviceSessionInfo("aabbccdd", "aabbccdd", "supermini-c3", "s1", LocalDateTime.now());
        when(wsSessionRegistry.all()).thenReturn(List.of(info));
        IotDevice device = new IotDevice();
        device.setId(42L);
        device.setMac("aabbccdd");
        device.setDeleted(0);
        when(iotDeviceMapper.selectOne(any())).thenReturn(device);

        List<OnlineSessionVO> result = service.listOnlineSessions();

        assertThat(result).hasSize(1);
        OnlineSessionVO vo = result.get(0);
        assertThat(vo.getDeviceId()).isEqualTo(42L);
        assertThat(vo.getDeviceMac()).isEqualTo("aabbccdd");
        assertThat(vo.getSessionId()).isEqualTo("s1");
    }

    @Test
    void pageSessions_shouldPageAndEnrichOnline() {
        Page<IotSession> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(session(1L, "s1", 2)));
        dbPage.setTotal(1);
        when(iotSessionMapper.selectPage(any(), any())).thenReturn(dbPage);
        IotDevice device = new IotDevice();
        device.setId(1L);
        device.setMac("aabbccdd");
        when(iotDeviceMapper.selectById(1L)).thenReturn(device);

        PageResult<OnlineSessionVO> result = service.pageSessions(1L, 20L, null);

        assertThat(result.getRecords()).hasSize(1);
        OnlineSessionVO vo = result.getRecords().get(0);
        assertThat(vo.getOnline()).isTrue();
        assertThat(vo.getDeviceName()).isEqualTo("aabbccdd");
        assertThat(vo.getTurnCount()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void pageSessions_whenEnded_shouldMarkOffline() {
        IotSession ended = session(1L, "s1", 3);
        ended.setEndedAt(LocalDateTime.now());
        Page<IotSession> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(ended));
        dbPage.setTotal(1);
        when(iotSessionMapper.selectPage(any(), any())).thenReturn(dbPage);

        PageResult<OnlineSessionVO> result = service.pageSessions(1L, 20L, false);

        assertThat(result.getRecords().get(0).getOnline()).isFalse();
        assertThat(result.getRecords().get(0).getEndedAt()).isNotNull();
    }

    @Test
    void startSession_shouldInsert() {
        service.startSession(1L, "s-new");

        verify(iotSessionMapper).insert(any(IotSession.class));
    }

    @Test
    void endSession_shouldSetEndedAt() {
        when(iotSessionMapper.selectOne(any())).thenReturn(session(1L, "s1", 3));

        service.endSession("s1");

        verify(iotSessionMapper).updateById(any(IotSession.class));
    }

    @Test
    void endSession_whenUnknown_shouldNoOp() {
        when(iotSessionMapper.selectOne(any())).thenReturn(null);

        service.endSession("s-x");

        verify(iotSessionMapper, org.mockito.Mockito.never()).updateById(any(IotSession.class));
    }

    @Test
    void incrementTurn_whenExisting_shouldIncrement() {
        when(iotSessionMapper.selectOne(any())).thenReturn(session(1L, "s1", 2));

        service.incrementTurn(1L, "s1");

        verify(iotSessionMapper).updateById(any(IotSession.class));
    }

    @Test
    void incrementTurn_whenMissing_shouldStartNew() {
        when(iotSessionMapper.selectOne(any())).thenReturn(null);

        service.incrementTurn(1L, "s-new");

        verify(iotSessionMapper).insert(any(IotSession.class));
    }

    @Test
    void findBySessionId_shouldReturnSession() {
        when(iotSessionMapper.selectOne(any())).thenReturn(session(1L, "s1", 1));

        IotSession found = service.findBySessionId("s1");

        assertThat(found.getSessionId()).isEqualTo("s1");
    }

    @Test
    void findBySessionId_whenBlank_shouldReturnNull() {
        assertThat(service.findBySessionId(" ")).isNull();
    }
}
