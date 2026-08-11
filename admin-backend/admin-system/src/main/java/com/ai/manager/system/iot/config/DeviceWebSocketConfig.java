package com.ai.manager.system.iot.config;

import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotSessionMapper;
import com.ai.manager.system.iot.service.VoicePipelineService;
import com.ai.manager.system.iot.websocket.DeviceWsHandler;
import com.ai.manager.system.iot.websocket.WsHandshakeInterceptor;
import com.ai.manager.system.iot.websocket.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 设备 WebSocket 通道配置（/ws/device）。
 * <p>
 * 这是 ESP32 协议强制的 WebSocket 通道，与后台 UI 的 SSE 推送互不影响。
 * </p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class DeviceWebSocketConfig implements WebSocketConfigurer {

    private final IotProperties iotProperties;

    private final IotDeviceMapper iotDeviceMapper;

    private final IotSessionMapper iotSessionMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final VoicePipelineService voicePipelineService;

    @Bean
    public WsSessionRegistry wsSessionRegistry() {
        return new WsSessionRegistry(redisTemplate, iotProperties.getTokenTtlSeconds());
    }

    @Bean
    public DeviceWsHandler deviceWsHandler(WsSessionRegistry registry) {
        return new DeviceWsHandler(registry, iotProperties, iotDeviceMapper, iotSessionMapper, voicePipelineService);
    }

    @Bean
    public WsHandshakeInterceptor wsHandshakeInterceptor() {
        return new WsHandshakeInterceptor(iotDeviceMapper);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWsHandler(wsSessionRegistry()), "/ws/device")
                .addInterceptors(wsHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
