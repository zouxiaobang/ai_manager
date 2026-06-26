package com.ai.manager.system.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeployChecklistService {

    public Map<String, Object> checkLogs() {
        Map<String, Object> result = new HashMap<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) {
            result.put("ok", true);
            result.put("skipped", true);
            result.put("message", "当前非 Linux 环境，跳过 systemd 日志检查");
            return result;
        }

        try {
            Process process = new ProcessBuilder(
                            "journalctl",
                            "-u",
                            "ai-manager-backend",
                            "-n",
                            "50",
                            "--no-pager")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.put("ok", false);
                result.put("skipped", false);
                result.put("message", "读取 journalctl 超时");
                return result;
            }
            if (process.exitValue() != 0) {
                result.put("ok", false);
                result.put("skipped", false);
                result.put("message", "journalctl 执行失败，退出码 " + process.exitValue());
                return result;
            }

            List<String> issues = detectLogIssues(output);
            boolean ok = issues.isEmpty();
            result.put("ok", ok);
            result.put("skipped", false);
            result.put(
                    "message",
                    ok ? "最近 50 行后端日志未发现严重错误" : String.join("；", issues));
            return result;
        } catch (Exception ex) {
            result.put("ok", false);
            result.put("skipped", false);
            result.put("message", "日志检查失败: " + ex.getMessage());
            return result;
        }
    }

    private List<String> detectLogIssues(String output) {
        List<String> issues = new ArrayList<>();
        if (output == null || output.isBlank()) {
            issues.add("日志为空，请确认服务是否已启动");
            return issues;
        }
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("APPLICATION FAILED TO START")) {
                issues.add("检测到应用启动失败");
                break;
            }
            if (trimmed.contains("Communications link failure")
                    || trimmed.contains("Could not open JDBC Connection")) {
                issues.add("检测到 MySQL 连接异常");
            }
            if (trimmed.contains("Unable to connect to Redis")
                    || trimmed.contains("RedisConnectionFailureException")) {
                issues.add("检测到 Redis 连接异常");
            }
            if (trimmed.contains("OutOfMemoryError")) {
                issues.add("检测到内存不足 OOM");
            }
        }
        return issues.stream().distinct().toList();
    }
}
