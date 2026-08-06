package com.ai.manager.system.iot.websocket;

import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotSession;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.protocol.BinaryFrame;
import com.ai.manager.system.iot.protocol.BinaryProtocol;
import com.ai.manager.system.iot.protocol.WireMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 设备 WebSocket 长连接处理器（/ws/device）。
 * <p>
 * 文本帧：hello → server hello（下发 session_id + audio_params）；listen/abort/MCP 结果分发。
 * 二进制帧：按握手协商版本（v1/v2/v3）解码 Opus 负载，路由到语音流水线（骨架）。
 * 下发：stt/llm/tts/system 方法骨架。
 * </p>
 */
@Slf4j
public class DeviceWsHandler extends AbstractWebSocketHandler {

    private final WsSessionRegistry sessionRegistry;

    private final IotProperties iotProperties;

    private final IotDeviceMapper iotDeviceMapper;

    private final IotSessionMapper iotSessionMapper;

    /** 设备在线 WebSocket 会话（key=deviceId） */
    private final ConcurrentMap<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();

    public DeviceWsHandler(WsSessionRegistry sessionRegistry, IotProperties iotProperties,
                           IotDeviceMapper iotDeviceMapper, IotSessionMapper iotSessionMapper) {
        this.sessionRegistry = sessionRegistry;
        this.iotProperties = iotProperties;
        this.iotDeviceMapper = iotDeviceMapper;
        this.iotSessionMapper = iotSessionMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceId = attribute(session, WsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (deviceId == null) {
            closeQuietly(session, "缺设备标识");
            return;
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        session.getAttributes().put("sessionId", sessionId);
        liveSessions.put(deviceId, session);

        IotDevice device = findDevice(deviceId);
        if (device != null) {
            device.setStatus("ONLINE");
            device.setSessionId(sessionId);
            device.setLastSeenAt(LocalDateTime.now());
            iotDeviceMapper.updateById(device);
            persistSession(device.getId(), sessionId);
        }
        DeviceSessionInfo info = new DeviceSessionInfo(deviceId, deviceId,
                device == null ? null : device.getModel(), sessionId, LocalDateTime.now());
        sessionRegistry.register(info);
        log.info("设备上线: deviceId={}, sessionId={}", deviceId, sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String deviceId = attribute(session, WsHandshakeInterceptor.ATTR_DEVICE_ID);
        String json = message.getPayload();
        if (deviceId == null) {
            return;
        }
        sessionRegistry.touch(deviceId);
        String type = WireMessages.parseType(json);
        switch (type) {
            case "hello" -> {
                String sessionId = UUID.randomUUID().toString().replace("-", "");
                session.getAttributes().put("sessionId", sessionId);
                IotDevice device = findDevice(deviceId);
                if (device != null) {
                    device.setSessionId(sessionId);
                    device.setLastSeenAt(LocalDateTime.now());
                    iotDeviceMapper.updateById(device);
                }
                sendText(session, WireMessages.serverHello(16000, 1, 16));
            }
            case "listen" -> onListen(deviceId, sessionId(session));
            case "abort" -> onAbort(deviceId, sessionId(session));
            default -> log.debug("未知/未处理文本消息 type={}, deviceId={}", type, deviceId);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String deviceId = attribute(session, WsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (deviceId == null) {
            return;
        }
        sessionRegistry.touch(deviceId);
        int version = protocolVersion(session);
        byte[] frameBytes = message.getPayload().array();
        try {
            BinaryFrame frame = BinaryProtocol.decode(version, frameBytes);
            // 骨架：将解码后的 Opus 负载交给语音流水线（ASR/TTS），此处仅记录
            log.debug("设备音频帧 deviceId={}, version={}, type={}, payloadLen={}",
                    deviceId, frame.getVersion(), frame.getType(), frame.getPayload().length);
        } catch (IllegalArgumentException e) {
            log.warn("设备音频帧解码失败 deviceId={}, version={}: {}", deviceId, version, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceId = attribute(session, WsHandshakeInterceptor.ATTR_DEVICE_ID);
        if (deviceId == null) {
            return;
        }
        liveSessions.remove(deviceId);
        sessionRegistry.unregister(deviceId);
        IotDevice device = findDevice(deviceId);
        if (device != null) {
            device.setStatus("OFFLINE");
            device.setLastSeenAt(LocalDateTime.now());
            iotDeviceMapper.updateById(device);
        }
        log.info("设备下线: deviceId={}, status={}", deviceId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("设备传输异常 deviceId={}: {}", attribute(session, WsHandshakeInterceptor.ATTR_DEVICE_ID),
                exception.getMessage());
    }

    // ---------- 下发方法骨架 ----------

    /** 下发 ASR 识别文本。 */
    public void sendStt(String deviceId, String text) {
        WebSocketSession session = liveSessions.get(deviceId);
        if (session != null) {
            sendText(session, WireMessages.stt(sessionId(session), text));
        }
    }

    /** 下发 LLM 回答 + 情绪。 */
    public void sendLlm(String deviceId, String emotion, String text) {
        WebSocketSession session = liveSessions.get(deviceId);
        if (session != null) {
            sendText(session, WireMessages.llm(sessionId(session), emotion, text));
        }
    }

    /** 下发 TTS 开始。 */
    public void sendTtsStart(String deviceId) {
        WebSocketSession session = liveSessions.get(deviceId);
        if (session != null) {
            sendText(session, WireMessages.tts(sessionId(session), "start", null));
        }
    }

    /** 下发 TTS 结束。 */
    public void sendTtsStop(String deviceId) {
        WebSocketSession session = liveSessions.get(deviceId);
        if (session != null) {
            sendText(session, WireMessages.tts(sessionId(session), "stop", null));
        }
    }

    /** 下发系统控制（如 reboot）。 */
    public void sendSystem(String deviceId, String command) {
        WebSocketSession session = liveSessions.get(deviceId);
        if (session != null) {
            sendText(session, "{\"type\":\"system\",\"command\":\"" + command + "\"}");
        }
    }

    // ---------- 私有工具 ----------

    private void onListen(String deviceId, String sessionId) {
        log.debug("设备进入监听 deviceId={}, sessionId={}", deviceId, sessionId);
    }

    private void onAbort(String deviceId, String sessionId) {
        log.debug("设备打断 deviceId={}, sessionId={}", deviceId, sessionId);
    }

    private IotDevice findDevice(String deviceId) {
        return iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, deviceId)
                .last("LIMIT 1"));
    }

    private void persistSession(Long deviceId, String sessionId) {
        IotSession session = new IotSession();
        session.setDeviceId(deviceId);
        session.setSessionId(sessionId);
        session.setStartedAt(LocalDateTime.now());
        session.setTurnCount(0);
        iotSessionMapper.insert(session);
    }

    private void sendText(WebSocketSession session, String payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            log.warn("下发设备消息失败: {}", e.getMessage());
        }
    }

    private void closeQuietly(WebSocketSession session, String reason) {
        try {
            session.close(new CloseStatus(CloseStatus.POLICY_VIOLATION.getCode(), reason));
        } catch (Exception ignored) {
            // 关闭失败不处理
        }
    }

    private String sessionId(WebSocketSession session) {
        Object v = session.getAttributes().get("sessionId");
        return v == null ? "" : v.toString();
    }

    private int protocolVersion(WebSocketSession session) {
        Object v = session.getAttributes().get(WsHandshakeInterceptor.ATTR_PROTOCOL_VERSION);
        return v instanceof Integer i ? i : iotProperties.getProtocolVersion();
    }

    private String attribute(WebSocketSession session, String name) {
        Object v = session.getAttributes().get(name);
        return v == null ? null : v.toString();
    }
}
