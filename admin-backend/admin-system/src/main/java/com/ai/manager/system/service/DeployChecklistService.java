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

    private static final String[] JOURNALCTL_ARGS = {
        "-u", "ai-manager-backend", "-n", "50", "--no-pager", "-o", "cat"
    };

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
            JournalRun direct = runJournalctl(false);
            JournalRun journalRun = direct;
            if (!direct.success()) {
                JournalRun withSudo = runJournalctl(true);
                if (withSudo.success()) {
                    journalRun = withSudo;
                }
            }

            if (!journalRun.success()) {
                result.put("ok", false);
                result.put("skipped", false);
                result.put(
                        "message",
                        buildJournalFailureMessage(journalRun.exitCode(), journalRun.output()));
                return result;
            }

            String output = journalRun.output();
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

    private JournalRun runJournalctl(boolean useSudo) throws Exception {
        List<String> command = new ArrayList<>();
        if (useSudo) {
            command.add("/usr/bin/sudo");
            command.add("-n");
        }
        command.add("/usr/bin/journalctl");
        for (String arg : JOURNALCTL_ARGS) {
            command.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new JournalRun(-1, "读取 journalctl 超时");
        }
        return new JournalRun(process.exitValue(), output);
    }

    private String buildJournalFailureMessage(int exitCode, String output) {
        String hint =
                "aimanager 用户无法读取 systemd 日志。请在 114 上执行其一："
                        + "① sudo usermod -aG systemd-journal aimanager 后重启服务；"
                        + "② 更新 sudoers 允许 journalctl（见 deploy/sudoers/ai-manager-deploy.example）后 systemctl restart ai-manager-backend";
        String trimmed = output == null ? "" : output.trim();
        if (trimmed.isEmpty()) {
            return "journalctl 执行失败，退出码 " + exitCode + "。" + hint;
        }
        String firstLine = trimmed.lines().findFirst().orElse(trimmed);
        if (firstLine.length() > 120) {
            firstLine = firstLine.substring(0, 120) + "…";
        }
        return "journalctl 执行失败，退出码 " + exitCode + "（" + firstLine + "）。" + hint;
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

    private record JournalRun(int exitCode, String output) {
        boolean success() {
            return exitCode == 0;
        }
    }
}
