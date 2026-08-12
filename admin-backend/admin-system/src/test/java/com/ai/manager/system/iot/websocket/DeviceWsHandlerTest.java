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
 * DeviceWsHandler 按段语音流水线接线测试。
 * <p>
 * 端到端录音语义：设备 listen(detect) → 缓冲上行 Opus 帧（累积成分帧流）→
 * listen(stop) → 整段交语音流水线 → tts start / 下行 Opus 帧 / tts stop。
 * 未激活缓冲的音频帧被忽略（防逐帧回显风暴）。
 * </p>
 * <p>
 * 注意：afterConnectionEstablished 会生成随机 sessionId 覆盖握手态属性，因此
 * 各用例先 establish() 再读取真实 sessionId，用于 processTurn 的 stub/verify。
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

    /** establish 后 attrs 中的真实 sessionId（handler 生成的随机 UUID）。 */
    private String sessionId() {
        return (String) attrs.get("sessionId");
    }

    private void listenDetect() throws Exception {
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"listen\",\"state\":\"detect\",\"session_id\":\"s1\"}"));
    }

    private void listenStop() throws Exception {
        handler.handleTextMessage(session, new TextMessage(
                "{\"type\":\"listen\",\"state\":\"stop\",\"session_id\":\"s1\"}"));
    }

    /** 上行一个裸 Opus payload 的 v3 帧。 */
    private void sendOpus(byte[] payload) throws Exception {
        handler.handleBinaryMessage(session,
                new BinaryMessage(BinaryProtocol.encodeV3(BinaryProtocol.TYPE_OPUS, 0, payload)));
    }

    @Test
    void fullTurn_detectBufferFramesStopEchoesWholeSegment() throws Exception {
        establish();
        String sid = sessionId();
        listenDetect();
        // 3 帧上行：payload 3B/2B/1B → 缓冲成 {0,3,01,02,03}{0,2,04,05}{0,1,06}
        sendOpus(new byte[]{0x01, 0x02, 0x03});
        sendOpus(new byte[]{0x04, 0x05});
        sendOpus(new byte[]{0x06});
        // 整段分帧流交语音流水线
        byte[] expectedSegment = new byte[]{0x00, 0x03, 0x01, 0x02, 0x03, 0x00, 0x02, 0x04, 0x05,
                0x00, 0x01, 0x06};
        byte[] downlinkFramed = new byte[]{0x00, 0x02, (byte) 0xAA, (byte) 0xBB};
        when(voicePipelineService.processTurn(eq(expectedSegment), eq(sid)))
                .thenReturn(downlinkFramed);

        listenStop();

        verify(voicePipelineService).processTurn(expectedSegment, sid);
        // 3 条下发：tts start → 二进制 Opus 帧 → tts stop
        assertThat(sent).hasSize(3);
        TextMessage start = (TextMessage) sent.get(0);
        assertThat(start.getPayload()).contains("tts").contains("start").contains(sid);
        BinaryMessage bin = (BinaryMessage) sent.get(1);
        assertThat(bin.getPayload().array())
                .containsExactly(0x00, 0x00, 0x00, 0x02, (byte) 0xAA, (byte) 0xBB);
        TextMessage stop = (TextMessage) sent.get(2);
        assertThat(stop.getPayload()).contains("tts").contains("stop").contains(sid);
    }

    @Test
    void framesBeforeDetect_shouldBeIgnored() throws Exception {
        establish();
        // 未 listen(detect)：上行音频帧不缓冲、不触发流水线（防逐帧回显）
        sendOpus(new byte[]{1, 2, 3});
        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void framesAfterStop_shouldBeIgnoredUntilNextDetect() throws Exception {
        establish();
        String sid = sessionId();
        listenDetect();
        sendOpus(new byte[]{0x01});
        when(voicePipelineService.processTurn(any(byte[].class), eq(sid))).thenReturn(new byte[0]);
        listenStop();  // 结束第一段（下行空 → 无下发）
        assertThat(sent).isEmpty();

        // listen(stop) 后缓冲已关闭：再发帧属于「非缓冲态」，被忽略，不触发第二次 processTurn
        sendOpus(new byte[]{0x02});
        verify(voicePipelineService, never()).processTurn(
                org.mockito.ArgumentMatchers.<byte[]>argThat(a -> a.length == 3 && a[2] == 0x02), eq(sid));
        // 只发生过一次（第一段），即便把入参放宽也不应有第二次调用
        verify(voicePipelineService).processTurn(any(byte[].class), eq(sid));
    }

    @Test
    void listenStop_withoutDetect_shouldDoNothing() throws Exception {
        establish();
        listenStop();  // 无进行中的段
        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void emptySegment_shouldNotRunPipeline() throws Exception {
        establish();
        listenDetect();
        listenStop();  // 缓冲为空（无帧）
        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_nonOpusType_shouldIgnore() throws Exception {
        establish();
        listenDetect();
        byte[] jsonFrame = BinaryProtocol.encodeV3(BinaryProtocol.TYPE_JSON, 0, new byte[]{1});

        handler.handleBinaryMessage(session, new BinaryMessage(jsonFrame));

        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void handleBinaryMessage_malformedFrame_shouldNotThrow() throws Exception {
        establish();
        listenDetect();
        // 声明 size=5 实际只有 1 字节负载 → decodeV3 抛 size mismatch，被 handler 捕获
        byte[] badFrame = new byte[]{0x00, 0x00, 0x00, 0x05, 0x01};

        handler.handleBinaryMessage(session, new BinaryMessage(badFrame));

        verifyNoInteractions(voicePipelineService);
        assertThat(sent).isEmpty();
    }

    @Test
    void listenStop_emptyDownlink_shouldNotSendAnything() throws Exception {
        establish();
        String sid = sessionId();
        listenDetect();
        sendOpus(new byte[]{1});
        when(voicePipelineService.processTurn(any(byte[].class), eq(sid))).thenReturn(new byte[0]);

        listenStop();

        assertThat(sent).isEmpty();
    }

    @Test
    void listenStop_nullDownlink_shouldNotSendAnything() throws Exception {
        establish();
        String sid = sessionId();
        listenDetect();
        sendOpus(new byte[]{1});
        when(voicePipelineService.processTurn(any(byte[].class), eq(sid))).thenReturn(null);

        listenStop();

        assertThat(sent).isEmpty();
    }

    @Test
    void listenStop_sessionClosed_shouldSkipSending() throws Exception {
        establish();
        listenDetect();
        sendOpus(new byte[]{1});
        when(session.isOpen()).thenReturn(false);
        byte[] downlinkFramed = new byte[]{0x00, 0x02, (byte) 0xAA, (byte) 0xBB};
        when(voicePipelineService.processTurn(any(byte[].class), any(String.class)))
                .thenReturn(downlinkFramed);

        listenStop();

        // 会话已关：sendText/sendBinary 内部 isOpen 判假，不真正 sendMessage
        assertThat(sent).isEmpty();
        verify(session, never()).sendMessage(any(WebSocketMessage.class));
    }
}
