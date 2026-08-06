package com.ai.manager.system.iot.websocket;

import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WsHandshakeInterceptorTest {

    @Mock
    private IotDeviceMapper iotDeviceMapper;

    private WsHandshakeInterceptor interceptor;

    private WebSocketHandler handler = mock(WebSocketHandler.class);

    @BeforeAll
    static void initMybatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), IotDevice.class);
    }

    @BeforeEach
    void setUp() {
        interceptor = new WsHandshakeInterceptor(iotDeviceMapper);
    }

    private ServerHttpRequest request(HttpHeaders headers) {
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        when(req.getHeaders()).thenReturn(headers);
        return req;
    }

    private ServerHttpResponse response() {
        return mock(ServerHttpResponse.class);
    }

    @Test
    void beforeHandshake_whenMissingHeaders_shouldReject401() {
        ServerHttpResponse resp = response();
        boolean ok = interceptor.beforeHandshake(request(new HttpHeaders()), resp, handler, new HashMap<>());

        assertThat(ok).isFalse();
        verify(resp).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_whenDeviceUnknown_shouldReject() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer tok");
        headers.set("Protocol-Version", "3");
        headers.set("Device-Id", "aabbccdd");
        headers.set("Client-Id", "c1");
        when(iotDeviceMapper.selectOne(any())).thenReturn(null);

        ServerHttpResponse resp = response();
        boolean ok = interceptor.beforeHandshake(request(headers), resp, handler, new HashMap<>());

        assertThat(ok).isFalse();
        verify(resp).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_whenTokenMismatch_shouldReject() {
        IotDevice device = new IotDevice();
        device.setWsToken("other");
        when(iotDeviceMapper.selectOne(any())).thenReturn(device);
        HttpHeaders headers = validHeaders();

        ServerHttpResponse resp = response();
        boolean ok = interceptor.beforeHandshake(request(headers), resp, handler, new HashMap<>());

        assertThat(ok).isFalse();
    }

    @Test
    void beforeHandshake_whenValid_shouldAcceptAndSetAttributes() {
        IotDevice device = new IotDevice();
        device.setWsToken("tok-123");
        when(iotDeviceMapper.selectOne(any())).thenReturn(device);
        HttpHeaders headers = validHeaders();

        Map<String, Object> attrs = new HashMap<>();
        ServerHttpResponse resp = response();
        boolean ok = interceptor.beforeHandshake(request(headers), resp, handler, attrs);

        assertThat(ok).isTrue();
        assertThat(attrs.get(WsHandshakeInterceptor.ATTR_DEVICE_ID)).isEqualTo("aabbccdd");
        assertThat(attrs.get(WsHandshakeInterceptor.ATTR_PROTOCOL_VERSION)).isEqualTo(3);
        assertThat(attrs.get(WsHandshakeInterceptor.ATTR_CLIENT_ID)).isEqualTo("c1");
    }

    private HttpHeaders validHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer tok-123");
        headers.set("Protocol-Version", "3");
        headers.set("Device-Id", "AA:BB:CC:DD");
        headers.set("Client-Id", "c1");
        return headers;
    }
}
