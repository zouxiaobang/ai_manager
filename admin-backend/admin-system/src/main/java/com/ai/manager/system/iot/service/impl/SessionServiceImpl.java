package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotSession;
import com.ai.manager.system.iot.domain.vo.OnlineSessionVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.service.SessionService;
import com.ai.manager.system.iot.websocket.DeviceSessionInfo;
import com.ai.manager.system.iot.websocket.WsSessionRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final IotSessionMapper iotSessionMapper;

    private final IotDeviceMapper iotDeviceMapper;

    private final WsSessionRegistry wsSessionRegistry;

    @Override
    public List<OnlineSessionVO> listOnlineSessions() {
        return wsSessionRegistry.all().stream()
                .map(this::toOnlineVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<OnlineSessionVO> pageSessions(Long page, Long pageSize, Boolean online) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<IotSession> entityPage = iotSessionMapper.selectPage(new Page<>(p, ps),
                new LambdaQueryWrapper<IotSession>()
                        .eq(IotSession::getDeleted, 0)
                        .isNull(Boolean.TRUE.equals(online), IotSession::getEndedAt)
                        .isNotNull(Boolean.FALSE.equals(online), IotSession::getEndedAt)
                        .orderByDesc(IotSession::getId));
        List<OnlineSessionVO> vos = entityPage.getRecords().stream()
                .map(this::toPageVO)
                .collect(Collectors.toList());
        return PageUtils.of(vos, entityPage.getTotal(), p, ps);
    }

    private OnlineSessionVO toPageVO(IotSession s) {
        OnlineSessionVO vo = new OnlineSessionVO();
        vo.setId(s.getId());
        vo.setDeviceId(s.getDeviceId());
        vo.setSessionId(s.getSessionId());
        vo.setStartedAt(s.getStartedAt());
        vo.setEndedAt(s.getEndedAt());
        vo.setTurnCount(s.getTurnCount());
        vo.setOnline(s.getEndedAt() == null);
        if (s.getDeviceId() != null) {
            IotDevice device = iotDeviceMapper.selectById(s.getDeviceId());
            if (device != null) {
                vo.setDeviceName(device.getMac());
                vo.setDeviceMac(device.getMac());
                vo.setDeviceModel(device.getModel());
            }
        }
        return vo;
    }

    @Override
    @Transactional
    public IotSession startSession(Long deviceId, String sessionId) {
        IotSession session = new IotSession();
        session.setDeviceId(deviceId);
        session.setSessionId(sessionId);
        session.setStartedAt(LocalDateTime.now());
        session.setTurnCount(0);
        session.setDeleted(0);
        iotSessionMapper.insert(session);
        return session;
    }

    @Override
    @Transactional
    public void endSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        IotSession session = findBySessionId(sessionId);
        if (session != null) {
            session.setEndedAt(LocalDateTime.now());
            iotSessionMapper.updateById(session);
        }
    }

    @Override
    @Transactional
    public void incrementTurn(Long deviceId, String sessionId) {
        IotSession session = findBySessionId(sessionId);
        if (session == null) {
            startSession(deviceId, sessionId);
            return;
        }
        session.setTurnCount((session.getTurnCount() == null ? 0 : session.getTurnCount()) + 1);
        iotSessionMapper.updateById(session);
    }

    @Override
    public IotSession findBySessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        return iotSessionMapper.selectOne(new LambdaQueryWrapper<IotSession>()
                .eq(IotSession::getDeleted, 0)
                .eq(IotSession::getSessionId, sessionId)
                .last("LIMIT 1"));
    }

    private OnlineSessionVO toOnlineVO(DeviceSessionInfo info) {
        OnlineSessionVO vo = new OnlineSessionVO();
        vo.setDeviceMac(info.getMac());
        vo.setDeviceModel(info.getModel());
        vo.setSessionId(info.getSessionId());
        vo.setStartedAt(info.getStartedAt());
        vo.setTurnCount(resolveTurnCount(info));
        if (StringUtils.hasText(info.getDeviceId())) {
            IotDevice device = iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                    .eq(IotDevice::getDeleted, 0)
                    .eq(IotDevice::getMac, info.getDeviceId())
                    .last("LIMIT 1"));
            if (device != null) {
                vo.setDeviceId(device.getId());
            }
        }
        return vo;
    }

    private Integer resolveTurnCount(DeviceSessionInfo info) {
        IotSession session = findBySessionId(info.getSessionId());
        return session == null ? 0 : (session.getTurnCount() == null ? 0 : session.getTurnCount());
    }
}
