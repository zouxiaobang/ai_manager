package com.ai.manager.system.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.time.DisplayTime;
import com.ai.manager.system.service.DeployStatusService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查控制器
 *
 * <p>所属模块：系统模块-健康检查</p>
 * <p>API路径前缀：/api/health</p>
 * <p>功能描述：提供系统健康检查功能，包括应用状态、Redis连接、MySQL连接等检查</p>
 *
 * @author system
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final StringRedisTemplate stringRedisTemplate;
    private final DataSource dataSource;
    private final DeployStatusService deployStatusService;

    public HealthController(
            StringRedisTemplate stringRedisTemplate,
            DataSource dataSource,
            DeployStatusService deployStatusService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.dataSource = dataSource;
        this.deployStatusService = deployStatusService;
    }

    /**
     * 健康检查
     *
     * <p>HTTP方法：GET</p>
     * <p>路径：/api/health</p>
     *
     * @return 健康检查结果，包含应用状态、Redis状态、MySQL状态等
     */
    @GetMapping
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "ai-manager-admin");
        data.put("serverTime", DisplayTime.formatMinute(Instant.now()));
        data.put("serverTimeZone", DisplayTime.ZONE.getId());
        data.put("startedAt", DisplayTime.formatMinute(deployStatusService.applicationStartedAt()));

        checkRedis(data);
        checkMysql(data);
        deployStatusService
                .resolveLastDeployAt()
                .ifPresent(instant -> data.put("lastDeployAt", DisplayTime.formatMinute(instant)));

        boolean appUp = true;
        boolean dataUp = "UP".equals(data.get("mysql")) && "UP".equals(data.get("redis"));
        data.put("appNodeStatus", appUp ? "UP" : "DOWN");
        data.put("dataNodeStatus", dataUp ? "UP" : "DOWN");

        return ApiResult.ok(data);
    }

    private void checkRedis(Map<String, Object> data) {
        try {
            stringRedisTemplate.opsForValue().set("admin:health:ping", "ok", 10, TimeUnit.SECONDS);
            data.put("redis", "UP");
        } catch (Exception ex) {
            data.put("redis", "DOWN");
            data.put("redisError", ex.getMessage());
        }
    }

    private void checkMysql(Map<String, Object> data) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1");
            data.put("mysql", "UP");
        } catch (Exception ex) {
            data.put("mysql", "DOWN");
            data.put("mysqlError", ex.getMessage());
        }
    }
}
