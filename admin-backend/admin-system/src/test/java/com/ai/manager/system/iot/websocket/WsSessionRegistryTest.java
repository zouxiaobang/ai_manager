package com.ai.manager.system.iot.websocket;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WsSessionRegistry 在线会话注册表测试（纯内存模式）。
 */
class WsSessionRegistryTest {

    private WsSessionRegistry registry = new WsSessionRegistry(null, 300);

    private DeviceSessionInfo info(String deviceId) {
        return new DeviceSessionInfo(deviceId, deviceId, "supermini-c3", "sess-" + deviceId, LocalDateTime.now());
    }

    @Test
    void register_shouldMakeOnline() {
        registry.register(info("AABBCCDD"));
        assertThat(registry.isOnline("aabbccdd")).isTrue();
        assertThat(registry.isOnline("AA:BB:CC:DD")).isTrue();
    }

    @Test
    void unregister_shouldMakeOffline() {
        registry.register(info("AABBCCDD"));
        registry.unregister("aabbccdd");
        assertThat(registry.isOnline("aabbccdd")).isFalse();
    }

    @Test
    void get_shouldReturnMetadata() {
        registry.register(info("AABBCCDD"));
        DeviceSessionInfo got = registry.get("aabbccdd");
        assertThat(got).isNotNull();
        assertThat(got.getSessionId()).isEqualTo("sess-AABBCCDD");
        assertThat(got.getModel()).isEqualTo("supermini-c3");
    }

    @Test
    void get_shouldReturnNullForUnknown() {
        assertThat(registry.get("nope")).isNull();
    }

    @Test
    void all_shouldReturnRegisteredSessions() {
        registry.register(info("AABBCCDD"));
        registry.register(info("EEFF0011"));
        assertThat(registry.all()).hasSize(2);
    }

    @Test
    void touch_shouldKeepOnline() {
        registry.register(info("AABBCCDD"));
        registry.touch("aabbccdd");
        assertThat(registry.isOnline("aabbccdd")).isTrue();
    }

    @Test
    void isOnline_shouldIgnoreBlank() {
        assertThat(registry.isOnline("")).isFalse();
        assertThat(registry.isOnline(null)).isFalse();
    }
}
