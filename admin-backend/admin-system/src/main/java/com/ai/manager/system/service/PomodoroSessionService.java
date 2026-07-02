package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;

public interface PomodoroSessionService {

    PomodoroSessionVO getActiveSession();

    PomodoroSessionVO syncSession(PomodoroSessionSyncRequest request);

    /** 专注/休息倒计时进行中（RUNNING）时为 true，此时不可修改计划 */
    boolean isPlanEditBlocked();
}
