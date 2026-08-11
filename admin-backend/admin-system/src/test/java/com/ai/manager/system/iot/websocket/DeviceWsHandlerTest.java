package com.ai.manager.system.iot.websocket;

import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.protocol.BinaryProtocol;
import com.ai.manager.system.iot.service.VoicePipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DeviceWsHandler 二进制语音流水线接线测试。
 * <p>
 * 覆盖：设备上行 Opus v3 帧 → 语音流水线 → tts start / 下行 Opus 帧 / tts stop；
 * 非音频帧忽略；非法帧不抛异常；下行分帧流按帧拆分。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceWsHandlerTest {

    @Mock
    private WsSessionRegistry sessionRegistry;

    @Mock
    private IotDeviceMapper iotDeviceMapper;

    @Mock
    private IotSessionMapper iotSessionMapper;

    @Mock
    private VoicePipelineService voicePipelineService;

    @Mock
    private WebSocketSession session;

    private IotProperties properties;
    private DeviceWsHandler handler;
    private Map<String, Object> attrs;
    private List<WebSocketMessage<?>> sent;

    @BeforeEach
    void setUp() throws Exception {
        properties = new IotProperties();
        handler = new DeviceWsHandler(sessionRegistry, properties, iotDeviceMapper, iotSessionMapper,
                voicePipelineService);
        attrs = new HashMap<>();
        attrs.put(WsHandshakeInterceptor.ATTR_DEVICE_ID, "aabbcc");
        attrs.put(WsHandshakeInterceptor.ATTR_PROTOCOL_VERSION, 3);
        attrs.put("sessionId", "s1");
        when(session.getAttributes()).thenReturn(attrs);
        when(session.isOpen()).thenReturn(true);
        sent = new ArrayList<>();
        doAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return null;
        }).when(session).sendMessage(any(WebSocketMessage.class));
    }

    /** 模拟设备上线：注册 liveSessions，供 tts start/stop 下发定位会话。 */
    private void establish() throws Exception {
        handler.afterConnectionEstablished(session);
    }

    @Test
    void handleBinaryMessage_opusFrame_shouldRunPipelineAndSendDownlink() throws Exception {
        establish();
        String sid = (String) attrs.get("sessionId");
        // 上行 v3 Opus 帧（type=0），payload 为单个裸 Opus 包
        byte[] upFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, new byte[]{1, 2, 3});
        // 语音流水线返回「2 字节大端长度前缀 + Opus 包」分帧流（一帧）
        byte[] downlinkFramed = new byte[]{0x00, 0x02, (byte) 0xAA, (byte) 0xBB};
        when(voicePipelineService.processTurn(eq(new byte[]{1, 2, 3}), eq(sid))).thenReturn(downlinkFramed);

        handler.handleBinaryMessage(session, new BinaryMessage(upFrame));

        verify(voicePipelineService).processTurn(new byte[]{1, 2, 3}, sid);
        // 3 条下发：tts start → 二进制 Opus 帧 → tts stop
        assertThat(sent).hasSize(3);
        TextMessage start = (TextMessage) sent.get(0);
        assertThat(start.getPayload()).contains("tts").contains("start");
        BinaryMessage bin = (BinaryMessage) sent.get(1);
        // 下行帧 = encodeV3(TYPE_OPUS, 0, {AA,BB})
        assertThat(bin.getPayload().array()).containsExactly(0x00, 0x00, 0x00, 0x02, (byte) 0xAA, (byte) 0xBB);
        TextMessage stop = (TextMessage) sent.get(2);
        assertThat(stop.getPayload()).contains("stop");
    }

    @Test
    void handleBinaryMessage_multipleFrames_shouldSendOneBinaryPerOpusFrame() throws Exception {
        establish();
        String sid = (String) attrs.get("sessionId");
        byte[] upFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, new byte[]{9});
        // 分帧流含两帧：{0,1,AA} 和 {0,1,BB}
        byte[] downlinkFramed = new byte[]{0x00, 0x01, (byte) 0xAA, 0x00, 0x01, (byte) 0xBB};
        when(voicePipelineService.processTurn(any(byte[].class), eq(sid))).thenReturn(downlinkFramed);

        handler.handleBinaryMessage(session, new BinaryMessage(upFrame));

        List<BinaryMessage> binaries = sent.stream()
                .filter(m -> m instanceof BinaryMessage)
                .map(m -> (BinaryMessage) m)
                .toList();
        assertThat(binaries).hasSize(2);
        assertThat(binaries.get(0).getPayload().array())
                .containsExactly(0x00, 0x00, 0x00, 0x01, (byte) 0xAA);
        assertThat(binaries.get(1).getPayload().array())
                .containsExactly(0x00, 0x00, 0x00, 0x01, (byte) 0xBB);
    }

    @Test
    void handleBinaryMessage_nonOpusType_shouldIgnore() throws Exception {
        byte[] jsonFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_JSON, 0, new byte[]{1});

        handler.handleBinaryMessage(session, new BinaryMessage(jsonFrame));

        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_malformedFrame_shouldNotThrow() throws Exception {
        // 声明 size=5 实际只有 1 字节负载 → 解码抛 size mismatch，被捕获
        byte[] badFrame = new byte[]{0x00, 0x00, 0x00, 0x05, 0x01};

        handler.handleBinaryMessage(session, new BinaryMessage(badFrame));

        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_emptyDownlink_shouldNotSendAnything() throws Exception {
        byte[] upFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, new byte[]{1});
        when(voicePipelineService.processTurn(any(byte[].class), eq("s1"))).thenReturn(new byte[0]);

        handler.handleBinaryMessage(session, new BinaryMessage(upFrame));

        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_nullDownlink_shouldNotSendAnything() throws Exception {
        byte[] upFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, new byte[]{1});
        when(voicePipelineService.processTurn(any(byte[].class), eq("s1"))).thenReturn(null);

        handler.handleBinaryMessage(session, new BinaryMessage(upFrame));

        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_sessionClosed_shouldSkipSending() throws Exception {
        establish();
        when(session.isOpen()).thenReturn(false);
        byte[] upFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, new byte[]{1});
        byte[] downlinkFramed = new byte[]{0x00, 0x02, (byte) 0xAA, (byte) 0xBB};
        when(voicePipelineService.processTurn(any(byte[].class), any(String.class))).thenReturn(downlinkFramed);

        handler.handleBinaryMessage(session, new BinaryMessage(upFrame));

        // 会话已关：sendMessage 不会真正发出（内部同步块判断 isOpen）
        verify(session, never()).sendMessage(any(BinaryMessage.class));
        assertThat(sent).isEmpty();
    }
}
