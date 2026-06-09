package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final StringRedisTemplate stringRedisTemplate;

    public HealthController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "ai-manager-admin");
        try {
            stringRedisTemplate.opsForValue().set("admin:health:ping", "ok", 10, TimeUnit.SECONDS);
            data.put("redis", "UP");
        } catch (Exception ex) {
            data.put("redis", "DOWN");
            data.put("redisError", ex.getMessage());
        }
        return ApiResult.ok(data);
    }
}
