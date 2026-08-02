package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.entity.PixelDogState;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;
import com.ai.manager.system.service.PixelDogStateService;
import com.ai.manager.system.service.PomodoroSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PomodoroSessionServiceImpl implements PomodoroSessionService {

    private static final String REDIS_KEY = "pomodoro:session:active";
    private static final String DEVICE_SEEN_KEY = "pomodoro:device:last_seen_ms";
    private static final Duration TTL = Duration.ofHours(24);

    private static final Set<String> PHASES = Set.of("IDLE", "WORK", "SHORT_BREAK", "LONG_BREAK");
    private static final Set<String> PENDING_PHASES = Set.of("WORK", "SHORT_BREAK", "LONG_BREAK");
    private static final Set<String> RUN_STATES = Set.of("IDLE", "RUNNING", "PAUSED");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PixelDogStateService pixelDogStateService;

    @Override
    public PomodoroSessionVO getActiveSession() {
        Object raw = redisTemplate.opsForValue().get(REDIS_KEY);
        PomodoroSessionVO session = toSession(raw);
        if (session == null) {
            return null;
        }
        attachDeviceLastSeen(session);
        return adjustRunningRemaining(session);
    }

    @Override
    public PomodoroSessionVO syncSession(PomodoroSessionSyncRequest request) {
        validate(request);
        PomodoroSessionVO existing = toSession(redisTemplate.opsForValue().get(REDIS_KEY));
        String source = normalizeSource(request);
        touchDeviceIfNeeded(source);
        String runState = request.getRunState().trim().toUpperCase();
        boolean takeControl = Boolean.TRUE.equals(request.getTakeControl());
        boolean deviceActive = "DEVICE".equals(source) && isActiveRunState(runState);

        // 副屏开始/暂停时，允许用 RUNNING/PAUSED 覆盖陈旧的 ADMIN 空闲会话（无需显式 takeControl）
        if (!takeControl && deviceActive && existing != null
                && "ADMIN".equals(existing.getController()) && isIdleSession(existing)) {
            takeControl = true;
        }
        // 副屏 IDLE 推送当 ADMIN 会话活跃时不接管，避免副屏重启/心跳误重置 ADMIN 番茄钟
        // 副屏如需重置，应通过前端 ADMIN 页面操作，或携带明确的 takeControl=true 参数

        // 无已有会话时，副屏不能创建新会话，必须由 ADMIN 创建
        if (existing == null && "DEVICE".equals(source)) {
            PomodoroSessionVO empty = new PomodoroSessionVO();
            empty.setPhase("IDLE");
            empty.setRunState("IDLE");
            empty.setRemainingSec(0);
            empty.setPhaseTotalSec(1);
            empty.setSessionWorkRounds(0);
            empty.setController("");
            return empty;
        }

        if (!takeControl && existing != null && StringUtils.hasText(existing.getController())
                && !existing.getController().equals(source)) {
            attachDeviceLastSeen(existing);
            return adjustRunningRemaining(existing);
        }

        PomodoroSessionVO session = buildSession(request, source);
        if (takeControl || existing == null || !StringUtils.hasText(existing.getController())) {
            session.setController(source);
        } else {
            session.setController(existing.getController());
        }
        session.setSyncedAtMs(System.currentTimeMillis());
        redisTemplate.opsForValue().set(REDIS_KEY, session, TTL);
        syncDogFocusStatus(session.getPhase(), session.getRunState());
        attachDeviceLastSeen(session);
        return adjustRunningRemaining(session);
    }

    private void touchDeviceIfNeeded(String source) {
        if (!"DEVICE".equals(source)) {
            return;
        }
        redisTemplate.opsForValue().set(DEVICE_SEEN_KEY, System.currentTimeMillis(), TTL);
    }

    private void attachDeviceLastSeen(PomodoroSessionVO session) {
        if (session == null) {
            return;
        }
        Object seen = redisTemplate.opsForValue().get(DEVICE_SEEN_KEY);
        if (seen instanceof Number number) {
            session.setDeviceLastSeenMs(number.longValue());
        } else if (seen != null) {
            try {
                session.setDeviceLastSeenMs(Long.parseLong(String.valueOf(seen)));
            } catch (NumberFormatException ignored) {
                // ignore malformed value
            }
        }
    }

    private PomodoroSessionVO buildSession(PomodoroSessionSyncRequest request, String source) {
        PomodoroSessionVO session = new PomodoroSessionVO();
        session.setPhase(request.getPhase().trim().toUpperCase());
        session.setRunState(request.getRunState().trim().toUpperCase());
        session.setRemainingSec(Math.max(0, request.getRemainingSec()));
        session.setPhaseTotalSec(Math.max(1, request.getPhaseTotalSec()));
        session.setSessionWorkRounds(request.getSessionWorkRounds() != null
                ? Math.max(0, request.getSessionWorkRounds()) : 0);
        session.setPlanId(request.getPlanId());
        session.setSource(source);
        if (StringUtils.hasText(request.getPendingPhase())) {
            String pending = request.getPendingPhase().trim().toUpperCase();
            if (PENDING_PHASES.contains(pending)) {
                session.setPendingPhase(pending);
            }
        }
        return session;
    }

    private String normalizeSource(PomodoroSessionSyncRequest request) {
        String source = request.getSource();
        return StringUtils.hasText(source) ? source.trim().toUpperCase() : "DEVICE";
    }

    private void validate(PomodoroSessionSyncRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getPhase())
                || !StringUtils.hasText(request.getRunState())
                || request.getRemainingSec() == null
                || request.getPhaseTotalSec() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "番茄钟会话参数不完整");
        }
        String phase = request.getPhase().trim().toUpperCase();
        String runState = request.getRunState().trim().toUpperCase();
        if (!PHASES.contains(phase)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无效的 phase: " + phase);
        }
        if (!RUN_STATES.contains(runState)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "无效的 runState: " + runState);
        }
    }

    private PomodoroSessionVO toSession(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof PomodoroSessionVO session) {
            return session;
        }
        if (raw instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, PomodoroSessionVO.class);
        }
        return null;
    }

    /**
     * GET 时按 syncedAt 推算 RUNNING 的剩余秒数，但不刷新 syncedAtMs，避免前端重复 apply。
     */
    private static boolean isActiveRunState(String runState) {
        return "RUNNING".equals(runState) || "PAUSED".equals(runState);
    }

    private static boolean isIdleSession(PomodoroSessionVO session) {
        // 仅全空闲（IDLE/IDLE）可被副屏无 takeControl 抢占；WORK+IDLE 为阶段起点待开始
        return session != null
                && "IDLE".equals(session.getRunState())
                && "IDLE".equals(session.getPhase());
    }

    private PomodoroSessionVO adjustRunningRemaining(PomodoroSessionVO session) {
        if (session == null || !"RUNNING".equals(session.getRunState())) {
            return session;
        }
        Long syncedAt = session.getSyncedAtMs();
        Integer remaining = session.getRemainingSec();
        if (syncedAt == null || remaining == null) {
            return session;
        }
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - syncedAt) / 1000);
        int adjusted = (int) Math.max(0, remaining - elapsedSec);
        if (adjusted == remaining) {
            return session;
        }
        PomodoroSessionVO copy = new PomodoroSessionVO();
        copy.setPhase(session.getPhase());
        copy.setRunState(session.getRunState());
        copy.setRemainingSec(adjusted);
        copy.setPhaseTotalSec(session.getPhaseTotalSec());
        copy.setSessionWorkRounds(session.getSessionWorkRounds());
        copy.setPlanId(session.getPlanId());
        copy.setSource(session.getSource());
        copy.setController(session.getController() != null ? session.getController() : session.getSource());
        copy.setPendingPhase(session.getPendingPhase());
        copy.setSyncedAtMs(syncedAt);
        copy.setDeviceLastSeenMs(session.getDeviceLastSeenMs());
        return copy;
    }

    /**
     * 根据番茄钟阶段同步像素狗状态：
     * - WORK + RUNNING → DOG_STATUS_FOCUS (7)
     * - IDLE + IDLE    → DOG_STATUS_IDLE (0)
     * - 其他情况不修改狗的状态
     */
    private void syncDogFocusStatus(String phase, String runState) {
        int dogStatus;
        if ("WORK".equals(phase) && "RUNNING".equals(runState)) {
            dogStatus = 7; // DOG_STATUS_FOCUS
        } else if ("IDLE".equals(phase) && "IDLE".equals(runState)) {
            dogStatus = 0; // DOG_STATUS_IDLE
        } else {
            return;
        }
        try {
            PixelDogState patch = new PixelDogState();
            patch.setStatus(dogStatus);
            pixelDogStateService.updateState(patch);
        } catch (Exception e) {
            // 不影响番茄钟主流程
        }
    }

    @Override
    public boolean isPlanEditBlocked() {
        PomodoroSessionVO session = getActiveSession();
        if (session == null) {
            return false;
        }
        String runState = session.getRunState() != null ? session.getRunState().trim().toUpperCase() : "";
        String phase = session.getPhase() != null ? session.getPhase().trim().toUpperCase() : "";
        if (!"RUNNING".equals(runState)) {
            return false;
        }
        return "WORK".equals(phase) || "SHORT_BREAK".equals(phase) || "LONG_BREAK".equals(phase);
    }
}
