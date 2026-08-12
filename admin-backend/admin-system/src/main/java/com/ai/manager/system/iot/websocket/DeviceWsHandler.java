package com.ai.manager.system.iot.websocket;

import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotSession;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.protocol.BinaryFrame;
import com.ai.manager.system.iot.protocol.BinaryProtocol;
import com.ai.manager.system.iot.protocol.ListenMessage;
import com.ai.manager.system.iot.protocol.WireMessages;
import com.ai.manager.system.iot.service.VoicePipelineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    private final VoicePipelineService voicePipelineService;

    /** 设备在线 WebSocket 会话（key=deviceId） */
    private final ConcurrentMap<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();

    /**
     * 语音段缓冲（key=deviceId）。
     * <p>
     * 端到端录音：设备 listen(detect) 开始累积上行 Opus 帧，listen(stop) 结束一段并把
     * 整段交语音流水线回显，避免逐帧回显风暴（真机看门狗根因之一）。
     * </p>
     */
    private final ConcurrentMap<String, TurnBuffer> turnBuffers = new ConcurrentHashMap<>();

    /** 单段上行 Opus 缓冲上限（约 30s @ 20ms/帧）：超限丢弃本段，防内存无限增长 */
    private static final int MAX_TURN_BUFFER_BYTES = 64 * 1024;

    /** 下行 Opus 逐帧发送间隔：对齐设备 20ms/帧的实时消费。突发灌帧会瞬间打满播放队列丢帧（听不清），
     *  也是设备端 WDT 的推手之一。 */
    private static final int FRAME_SEND_INTERVAL_MS = 20;

    private static class TurnBuffer {
        final ByteArrayOutputStream opus = new ByteArrayOutputStream();
        boolean active = false;  // listen(detect) 后、listen(stop) 前为 true

        void reset() {
            opus.reset();
            active = false;
        }
    }

    public DeviceWsHandler(WsSessionRegistry sessionRegistry, IotProperties iotProperties,
                           IotDeviceMapper iotDeviceMapper, IotSessionMapper iotSessionMapper,
                           VoicePipelineService voicePipelineService) {
        this.sessionRegistry = sessionRegistry;
        this.iotProperties = iotProperties;
        this.iotDeviceMapper = iotDeviceMapper;
        this.iotSessionMapper = iotSessionMapper;
        this.voicePipelineService = voicePipelineService;
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
            case "listen" -> onListen(deviceId, sessionId(session), json);
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
            if (frame.getType() != BinaryProtocol.TYPE_OPUS) {
                log.debug("忽略非音频二进制帧 deviceId={}, type={}", deviceId, frame.getType());
                return;
            }
            TurnBuffer buf = turnBuffers.get(deviceId);
            // 非缓冲态（未 listen detect 或已 listen stop）：丢弃上行音频，避免逐帧回显
            if (buf == null || !buf.active) {
                return;
            }
            // 缓冲本帧：包 2 字节大端长度前缀，累积成 OpusAudioCodec 可整体解码的分帧流
            byte[] payload = frame.getPayload();
            if (buf.opus.size() + payload.length + 2 > MAX_TURN_BUFFER_BYTES) {
                log.warn("语音段超上限，丢弃 deviceId={}", deviceId);
                buf.reset();
                return;
            }
            try {
                buf.opus.write((payload.length >> 8) & 0xff);
                buf.opus.write(payload.length & 0xff);
                buf.opus.write(payload);
            } catch (java.io.IOException e) {
                // ByteArrayOutputStream 签名声明 IOException，实际不抛；防御性处理，超限丢弃本段
                log.warn("语音段缓冲写入失败，丢弃 deviceId={}: {}", deviceId, e.getMessage());
                buf.reset();
            }
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
        turnBuffers.remove(deviceId);  // 释放语音段缓冲
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

    private void onListen(String deviceId, String sessionId, String json) {
        String state;
        try {
            ListenMessage msg = WireMessages.parseListen(json);
            state = msg.getState();
        } catch (IllegalArgumentException e) {
            log.warn("listen 消息解析失败 deviceId={}: {}", deviceId, e.getMessage());
            return;
        }
        TurnBuffer buf = turnBuffers.computeIfAbsent(deviceId, k -> new TurnBuffer());
        if ("stop".equals(state)) {
            flushTurn(deviceId, sessionId, buf);
        } else {
            // detect/start/空：开始一段新录音缓冲
            buf.reset();
            buf.active = true;
            log.debug("开始语音段缓冲 deviceId={}, sessionId={}", deviceId, sessionId);
        }
    }

    /** listen(stop)：结束当前缓冲段，整段交语音流水线回显（tts start + 下行 Opus + tts stop）。 */
    private void flushTurn(String deviceId, String sessionId, TurnBuffer buf) {
        if (!buf.active) {
            return;  // 无进行中的段（未 listen detect），忽略
        }
        buf.active = false;
        byte[] segment = buf.opus.toByteArray();
        buf.opus.reset();
        if (segment.length == 0) {
            log.debug("空语音段 deviceId={}", deviceId);
            return;
        }
        // 整段解码→回显→重编码为下行分帧流
        byte[] downlink = voicePipelineService.processTurn(segment, sessionId);
        if (downlink != null && downlink.length > 0) {
            WebSocketSession session = liveSessions.get(deviceId);
            if (session != null) {
                sendTtsStart(deviceId);
                sendOpusFrames(session, downlink, protocolVersion(session));
                sendTtsStop(deviceId);
            }
        }
        log.info("语音段回显 deviceId={}, bytes={}, downlink={}", deviceId, segment.length,
                downlink == null ? 0 : downlink.length);
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

    /**
     * 下行 Opus 音频：把语音流水线产出的「2 字节大端长度前缀 + Opus 包」分帧流，
     * 拆成单帧，每帧封装为协商版本的二进制帧（type=Opus）逐帧下发——设备端按「一帧一消息」解码。
     */
    private void sendOpusFrames(WebSocketSession session, byte[] framedOpus, int version) {
        int off = 0;
        while (off + 2 <= framedOpus.length) {
            int len = ((framedOpus[off] & 0xff) << 8) | (framedOpus[off + 1] & 0xff);
            if (len <= 0 || off + 2 + len > framedOpus.length) {
                log.warn("下行 Opus 分帧流不合法，中止发送: off={}, len={}, total={}", off, len, framedOpus.length);
                return;
            }
            byte[] opusFrame = Arrays.copyOfRange(framedOpus, off + 2, off + 2 + len);
            byte[] wireFrame = switch (version) {
                case 1 -> BinaryProtocol.encodeV1(opusFrame);
                case 2 -> BinaryProtocol.encodeV2(BinaryProtocol.TYPE_OPUS, 0, System.currentTimeMillis(), opusFrame);
                default -> BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, opusFrame);
            };
            sendBinary(session, wireFrame);
            off += 2 + len;
            // 逐帧 20ms 节流对齐设备实时播放（20ms/帧）。突发灌帧会瞬间打满播放队列
            // 导致丢帧（听不清）——真机看门狗的直接推手。中断则中止剩余下发。
            try {
                Thread.sleep(FRAME_SEND_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("下行 Opus 节流被中断，中止剩余帧发送");
                return;
            }
        }
    }

    private void sendBinary(WebSocketSession session, byte[] payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new BinaryMessage(payload));
                }
            }
        } catch (Exception e) {
            log.warn("下发设备二进制帧失败: {}", e.getMessage());
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
