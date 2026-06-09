package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;
import com.ai.manager.system.service.PomodoroSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pomodoro/session")
@RequiredArgsConstructor
public class PomodoroSessionController {

    private final PomodoroSessionService pomodoroSessionService;

    @GetMapping
    public ApiResult<PomodoroSessionVO> getActive() {
        return ApiResult.ok(pomodoroSessionService.getActiveSession());
    }

    @PutMapping
    public ApiResult<PomodoroSessionVO> sync(@RequestBody PomodoroSessionSyncRequest request) {
        return ApiResult.ok(pomodoroSessionService.syncSession(request));
    }
}
