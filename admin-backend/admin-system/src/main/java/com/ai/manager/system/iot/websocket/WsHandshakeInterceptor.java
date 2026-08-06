package com.ai.manager.system.iot.websocket;

import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权：校验 Authorization Bearer token + Protocol-Version / Device-Id / Client-Id 头。
 * <p>
 * 校验通过后将设备信息写入会话属性，供 DeviceWsHandler 使用。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_DEVICE_ID = "deviceId";
    public static final String ATTR_DEVICE_MAC = "deviceMac";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_PROTOCOL_VERSION = "protocolVersion";

    private static final String BEARER_PREFIX = "Bearer ";

    private final IotDeviceMapper iotDeviceMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String mac = header(request, "Device-Id");
        String clientId = header(request, "Client-Id");
        String protocolVersion = header(request, "Protocol-Version");
        String authorization = header(request, "Authorization");

        if (!StringUtils.hasText(mac) || !StringUtils.hasText(clientId)
                || !StringUtils.hasText(protocolVersion) || !StringUtils.hasText(authorization)) {
            log.warn("WS 握手失败：缺少 Device-Id/Client-Id/Protocol-Version/Authorization 头");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length()).trim()
                : authorization.trim();
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        IotDevice device = iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, normalizeMac(mac))
                .last("LIMIT 1"));
        if (device == null || !token.equals(device.getWsToken())) {
            log.warn("WS 握手失败：设备未注册或 token 不匹配, mac={}", mac);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        int version = parseProtocolVersion(protocolVersion);
        attributes.put(ATTR_DEVICE_ID, normalizeMac(mac));
        attributes.put(ATTR_DEVICE_MAC, normalizeMac(mac));
        attributes.put(ATTR_CLIENT_ID, clientId);
        attributes.put(ATTR_PROTOCOL_VERSION, version);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    private String header(ServerHttpRequest request, String name) {
        return request.getHeaders().getFirst(name);
    }

    private int parseProtocolVersion(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String normalizeMac(String mac) {
        return mac.toLowerCase().replace(":", "").trim();
    }
}
