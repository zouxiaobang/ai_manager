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

/**
 * 番茄钟会话控制器
 *
 * <p>所属模块：番茄钟模块-会话管理</p>
 * <p>API路径前缀：/api/pomodoro/session</p>
 * <p>功能描述：提供当前番茄钟会话的查询和同步功能</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/pomodoro/session")
@RequiredArgsConstructor
public class PomodoroSessionController {

    private final PomodoroSessionService pomodoroSessionService;

    /**
     * 获取当前活跃的番茄钟会话
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/pomodoro/session</p>
     *
     * @return 当前番茄钟会话信息
     */
    @GetMapping
    public ApiResult<PomodoroSessionVO> getActive() {
        return ApiResult.ok(pomodoroSessionService.getActiveSession());
    }

    /**
     * 同步番茄钟会话
     *
     * <p>HTTP方法：PUT</p>
     * <p>路径：/api/pomodoro/session</p>
     *
     * @param request 会话同步请求参数
     * @return 同步后的番茄钟会话信息
     */
    @PutMapping
    public ApiResult<PomodoroSessionVO> sync(@RequestBody PomodoroSessionSyncRequest request) {
        return ApiResult.ok(pomodoroSessionService.syncSession(request));
    }
}
