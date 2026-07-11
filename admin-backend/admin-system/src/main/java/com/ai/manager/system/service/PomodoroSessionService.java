package com.ai.manager.system.service;

import com.ai.manager.system.domain.dto.PomodoroSessionSyncRequest;
import com.ai.manager.system.domain.vo.PomodoroSessionVO;

/**
 * 番茄钟会话服务接口
 *
 * <p>提供番茄钟会话的活动会话查询、会话同步和计划编辑状态判断等番茄钟会话管理功能。</p>
 */
public interface PomodoroSessionService {

    /**
     * 获取当前活动的番茄钟会话
     *
     * @return 活动会话信息
     */
    PomodoroSessionVO getActiveSession();

    /**
     * 同步番茄钟会话
     *
     * @param request 会话同步请求参数
     * @return 同步后的会话信息
     */
    PomodoroSessionVO syncSession(PomodoroSessionSyncRequest request);

    /**
     * 判断计划编辑是否被阻塞
     *
     * <p>专注/休息倒计时进行中（RUNNING）时为 true，此时不可修改计划</p>
     *
     * @return 计划编辑是否被阻塞
     */
    boolean isPlanEditBlocked();
}
