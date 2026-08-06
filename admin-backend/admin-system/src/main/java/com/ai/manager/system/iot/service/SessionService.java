package com.ai.manager.system.iot.service;

import com.ai.manager.system.iot.domain.entity.IotSession;
import com.ai.manager.system.iot.domain.vo.OnlineSessionVO;

import java.util.List;

public interface SessionService {

    /** 在线会话列表（由 WsSessionRegistry 实时数据 + 设备信息组装）。 */
    List<OnlineSessionVO> listOnlineSessions();

    /** 开启一次会话（WS 连接建立时）。 */
    IotSession startSession(Long deviceId, String sessionId);

    /** 结束会话（WS 断开时）。 */
    void endSession(String sessionId);

    /** 会话内唤醒轮次 +1。 */
    void incrementTurn(Long deviceId, String sessionId);

    IotSession findBySessionId(String sessionId);
}
