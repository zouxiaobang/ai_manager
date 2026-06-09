package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;
import com.ai.manager.system.service.PomodoroSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PomodoroSessionServiceImpl implements PomodoroSessionService {

    private static final String REDIS_KEY = "pomodoro:session:active";
    private static final Duration TTL = Duration.ofHours(24);

    private static final Set<String> PHASES = Set.of("IDLE", "WORK", "SHORT_BREAK", "LONG_BREAK");
    private static final Set<String> PENDING_PHASES = Set.of("WORK", "SHORT_BREAK", "LONG_BREAK");
    private static final Set<String> RUN_STATES = Set.of("IDLE", "RUNNING", "PAUSED");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PomodoroSessionVO getActiveSession() {
        Object raw = redisTemplate.opsForValue().get(REDIS_KEY);
        PomodoroSessionVO session = toSession(raw);
        if (session == null) {
            return null;
        }
        return adjustRunningRemaining(session);
    }

    @Override
    public PomodoroSessionVO syncSession(PomodoroSessionSyncRequest request) {
        validate(request);
        PomodoroSessionVO existing = toSession(redisTemplate.opsForValue().get(REDIS_KEY));
        String source = normalizeSource(request);
        String runState = request.getRunState().trim().toUpperCase();
        boolean takeControl = Boolean.TRUE.equals(request.getTakeControl());
        boolean deviceActive = "DEVICE".equals(source) && isActiveRunState(runState);
        String phase = request.getPhase().trim().toUpperCase();
        boolean deviceIdleReset = "DEVICE".equals(source)
                && "IDLE".equals(runState)
                && "IDLE".equals(phase);

        // 副屏开始/暂停时，允许用 RUNNING/PAUSED 覆盖陈旧的 ADMIN 空闲会话（无需显式 takeControl）
        if (!takeControl && deviceActive && existing != null
                && "ADMIN".equals(existing.getController()) && isIdleSession(existing)) {
            takeControl = true;
        }
        // 副屏长按重置（IDLE/IDLE）须覆盖本页仍在计时的 ADMIN 会话
        boolean devicePhaseIdleReset = "DEVICE".equals(source)
                && "IDLE".equals(runState)
                && !"IDLE".equals(phase);
        if (!takeControl && deviceIdleReset && existing != null
                && "ADMIN".equals(existing.getController()) && !isIdleSession(existing)) {
            takeControl = true;
        }
        // 副屏阶段内重置（如 SHORT_BREAK+IDLE）须覆盖本页仍在计时的 ADMIN 会话
        if (!takeControl && devicePhaseIdleReset && existing != null
                && "ADMIN".equals(existing.getController()) && !isIdleSession(existing)) {
            takeControl = true;
        }

        if (!takeControl && existing != null && StringUtils.hasText(existing.getController())
                && !existing.getController().equals(source)) {
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
        return adjustRunningRemaining(session);
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
        return session != null && "IDLE".equals(session.getRunState());
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
        return copy;
    }
}
