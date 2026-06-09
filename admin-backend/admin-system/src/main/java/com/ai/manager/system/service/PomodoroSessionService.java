package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;

public interface PomodoroSessionService {

    PomodoroSessionVO getActiveSession();

    PomodoroSessionVO syncSession(PomodoroSessionSyncRequest request);
}
