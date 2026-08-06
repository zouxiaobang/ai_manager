package com.ai.manager.system.iot.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 设备在线会话注册表（Redis + 内存镜像）。
 * <p>
 * 以 deviceId（MAC 归一化）为键记录在线会话，同时镜像到 Redis 便于多实例/故障恢复；
 * Redis 不可用时降级为纯内存模式（便于单元测试传 null）。
 * </p>
 */
@Slf4j
public class WsSessionRegistry {

    private static final String DEVICE_KEY_PREFIX = "iot:ws:device:";

    private final ConcurrentMap<String, DeviceSessionInfo> inMemory = new ConcurrentHashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;

    private final long sessionTtlSeconds;

    public WsSessionRegistry(RedisTemplate<String, Object> redisTemplate, long sessionTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public boolean isOnline(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return false;
        }
        return inMemory.containsKey(normalize(deviceId));
    }

    public void register(DeviceSessionInfo info) {
        String deviceId = normalize(info.getDeviceId());
        inMemory.put(deviceId, info);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key(deviceId), info.getSessionId(),
                        Duration.ofSeconds(sessionTtlSeconds));
            } catch (Exception e) {
                log.warn("写 Redis 设备在线会话失败，降级内存: deviceId={}", deviceId, e);
            }
        }
    }

    public void unregister(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        String key = normalize(deviceId);
        inMemory.remove(key);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key(key));
            } catch (Exception e) {
                log.warn("删除 Redis 设备在线会话失败: deviceId={}", deviceId, e);
            }
        }
    }

    public DeviceSessionInfo get(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return inMemory.get(normalize(deviceId));
    }

    /** 心跳续期：刷新内存与 Redis 的 TTL。 */
    public void touch(String deviceId) {
        DeviceSessionInfo info = get(deviceId);
        if (info == null) {
            return;
        }
        info.setStartedAt(LocalDateTime.now());
        if (redisTemplate != null) {
            try {
                redisTemplate.expire(key(normalize(deviceId)), Duration.ofSeconds(sessionTtlSeconds));
            } catch (Exception e) {
                log.warn("刷新 Redis 设备在线会话 TTL 失败: deviceId={}", deviceId, e);
            }
        }
    }

    public Collection<DeviceSessionInfo> all() {
        return inMemory.values();
    }

    private String key(String deviceId) {
        return DEVICE_KEY_PREFIX + deviceId;
    }

    /** MAC 归一化：小写、去冒号。 */
    private String normalize(String deviceId) {
        return deviceId == null ? "" : deviceId.toLowerCase().replace(":", "");
    }
}
