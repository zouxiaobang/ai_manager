package com.ai.manager.system.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.service.deploy.DeployPiSshClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
@Service
public class DeployRunnerService {

    public enum DeployTarget {
        BACKEND,
        FRONTEND
    }

    private enum DeployRunnerMode {
        REMOTE,
        LOCAL
    }

    private final boolean enabled;
    private final String projectRootConfig;
    private final String backendDir;
    private final String webRoot;
    private final String modeConfig;
    private final String runAsUser;
    private final boolean gitPullEnabled;
    private final DeployPiSshClient piSshClient;
    private final AtomicReference<String> runningTarget = new AtomicReference<>();

    public DeployRunnerService(
            @Value("${ai-manager.deploy.runner.enabled:false}") boolean enabled,
            @Value("${ai-manager.deploy.runner.project-root:}") String projectRootConfig,
            @Value("${ai-manager.deploy.backend-dir:/opt/ai-manager/backend}") String backendDir,
            @Value("${ai-manager.deploy.web-root:/var/www/ai-manager}") String webRoot,
            @Value("${ai-manager.deploy.runner.mode:auto}") String modeConfig,
            @Value("${ai-manager.deploy.runner.run-as-user:}") String runAsUser,
            @Value("${ai-manager.deploy.runner.git-pull-enabled:true}") boolean gitPullEnabled,
            DeployPiSshClient piSshClient) {
        this.enabled = enabled;
        this.projectRootConfig = projectRootConfig == null ? "" : projectRootConfig.trim();
        this.backendDir = backendDir == null ? "/opt/ai-manager/backend" : backendDir.trim();
        this.webRoot = webRoot == null ? "/var/www/ai-manager" : webRoot.trim();
        this.modeConfig = modeConfig == null ? "auto" : modeConfig.trim();
        this.runAsUser = runAsUser == null ? "" : runAsUser.trim();
        this.gitPullEnabled = gitPullEnabled;
        this.piSshClient = piSshClient;
    }

    public Map<String, Object> status() {
        DeployRunnerMode mode = resolveMode();
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", enabled);
        data.put("deployMode", mode.name().toLowerCase());
        data.put("running", runningTarget.get() != null);
        data.put("runningTarget", runningTarget.get());
        data.put("platform", System.getProperty("os.name"));
        data.put("runAsUser", runAsUser);
        if (mode == DeployRunnerMode.REMOTE) {
            data.put("sshAuthMode", piSshClient.isPasswordAuthEnabled() ? "password" : "key");
            data.put("sshTarget", piSshClient.targetLabel());
        }
        try {
            Path root = resolveProjectRoot();
            data.put("available", true);
            data.put("projectRoot", root.toString());
        } catch (Exception ex) {
            data.put("available", false);
            data.put("projectRoot", "");
            data.put("message", ex.getMessage());
        }
        return data;
    }

    public Map<String, Object> preflight() {
        DeployRunnerMode mode = resolveMode();
        Map<String, Object> data = new HashMap<>();
        data.put("deployMode", mode.name().toLowerCase());

        if (!enabled) {
            data.put("ready", false);
            data.put("sshReady", false);
            data.put("message", "当前环境未启用一键部署");
            return data;
        }

        if (mode == DeployRunnerMode.LOCAL) {
            boolean ready = runLocalPreflight(data);
            data.put("ready", ready);
            data.put("sshReady", ready);
            return data;
        }

        data.put("sshTarget", piSshClient.targetLabel());
        data.put("sshAuthMode", piSshClient.isPasswordAuthEnabled() ? "password" : "key");
        boolean ready = runRemotePreflight(data);
        data.put("ready", ready);
        data.put("sshReady", ready);
        return data;
    }

    public SseEmitter startStream(String targetRaw) {
        if (!enabled) {
            throw new BusinessException(403, "当前环境未启用一键部署");
        }
        DeployTarget target = parseTarget(targetRaw);
        String targetKey = target.name().toLowerCase();
        if (!runningTarget.compareAndSet(null, targetKey)) {
            throw new BusinessException(409, "已有部署任务正在执行，请稍后再试");
        }

        Path projectRoot = resolveProjectRoot();
        DeployRunnerMode mode = resolveMode();

        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        Runnable releaseLock = () -> runningTarget.compareAndSet(targetKey, null);
        emitter.onCompletion(releaseLock);
        emitter.onTimeout(releaseLock);
        emitter.onError(ex -> releaseLock.run());

        if (mode == DeployRunnerMode.LOCAL) {
            List<String> command = buildLocalScriptCommand(projectRoot, target);
            CompletableFuture.runAsync(() -> runScriptDeploy(emitter, projectRoot, command, releaseLock, true));
        } else if (piSshClient.isPasswordAuthEnabled()) {
            CompletableFuture.runAsync(() -> runPasswordDeploy(emitter, projectRoot, target, releaseLock));
        } else {
            List<String> command = buildRemoteScriptCommand(projectRoot, target);
            CompletableFuture.runAsync(() -> runScriptDeploy(emitter, projectRoot, command, releaseLock, false));
        }
        return emitter;
    }

    private DeployRunnerMode resolveMode() {
        return switch (modeConfig.toLowerCase()) {
            case "local" -> DeployRunnerMode.LOCAL;
            case "remote" -> DeployRunnerMode.REMOTE;
            default -> isWindows() || piSshClient.isPasswordAuthEnabled()
                    ? DeployRunnerMode.REMOTE
                    : DeployRunnerMode.LOCAL;
        };
    }

    private boolean runLocalPreflight(Map<String, Object> data) {
        try {
            Path root = resolveProjectRoot();
            Path backendScript = root.resolve("deploy").resolve("scripts").resolve("deploy-on-pi-backend.sh");
            Path frontendScript = root.resolve("deploy").resolve("scripts").resolve("deploy-on-pi-frontend.sh");
            if (!isDeployScript(backendScript) || !isDeployScript(frontendScript)) {
                data.put("message", "缺少本机部署脚本 deploy-on-pi-*.sh，请先在 114 上 git pull 最新代码");
                return false;
            }
            if (!runAsUser.isBlank() && !canRunAsUser(runAsUser)) {
                data.put(
                        "message",
                        "无法以用户 " + runAsUser + " 执行部署，请配置 sudoers（见 deploy/sudoers/ai-manager-deploy.example）");
                return false;
            }
            data.put("projectRoot", root.toString());
            return true;
        } catch (Exception ex) {
            data.put("message", ex.getMessage());
            return false;
        }
    }

    private boolean runRemotePreflight(Map<String, Object> data) {
        if (piSshClient.isPasswordAuthEnabled()) {
            try {
                piSshClient.testConnection();
                return true;
            } catch (Exception ex) {
                data.put("message", "SSH 密码登录失败: " + ex.getMessage());
                return false;
            }
        }

        try {
            List<String> command = List.of(
                    "ssh",
                    "-o", "BatchMode=yes",
                    "-o", "ConnectTimeout=10",
                    "-o", "StrictHostKeyChecking=accept-new",
                    piSshClient.targetLabel(),
                    "echo", "ssh-ok");
            ProcessBuilder builder = new ProcessBuilder(command);
            applyProcessEnvironment(builder);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            Charset charset = resolveConsoleCharset();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                data.put("message", "SSH 连接超时，请检查 114 是否在线");
                return false;
            }

            boolean ok = process.exitValue() == 0;
            if (!ok) {
                String detail = output.toString().trim();
                data.put(
                        "message",
                        "无法免密 SSH 到 " + piSshClient.targetLabel()
                                + "，请配置 ai-manager.deploy.pi.password 或 SSH 公钥。"
                                + (detail.isEmpty() ? "" : "\n" + detail));
            }
            return ok;
        } catch (Exception ex) {
            data.put("message", "SSH 预检失败: " + ex.getMessage());
            return false;
        }
    }

    private boolean canRunAsUser(String user) {
        try {
            ProcessBuilder builder = new ProcessBuilder("sudo", "-n", "-u", user, "true");
            applyProcessEnvironment(builder);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.getInputStream().readAllBytes();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ex) {
            log.warn("sudo preflight failed for user {}: {}", user, ex.getMessage());
            return false;
        }
    }

    private DeployTarget parseTarget(String targetRaw) {
        if (targetRaw == null) {
            throw new BusinessException(400, "缺少部署目标 target");
        }
        return switch (targetRaw.trim().toLowerCase()) {
            case "backend" -> DeployTarget.BACKEND;
            case "frontend" -> DeployTarget.FRONTEND;
            default -> throw new BusinessException(400, "不支持的部署目标: " + targetRaw);
        };
    }

    private Path resolveProjectRoot() {
        if (!projectRootConfig.isBlank()) {
            Path configured = Path.of(projectRootConfig).toAbsolutePath().normalize();
            if (isDeployScriptsDir(configured.resolve("deploy").resolve("scripts"))) {
                return configured;
            }
            String hint = resolveMode() == DeployRunnerMode.LOCAL && !runAsUser.isBlank()
                    ? "请确认已 git clone 到该路径，且 deploy/scripts 存在（可通过 sudo -u "
                            + runAsUser
                            + " 访问）"
                    : "请确认该路径下存在 deploy/scripts";
            throw new BusinessException(500, "配置的 project-root 无效: " + configured + "。" + hint);
        }

        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd);
        Path parent = cwd.getParent();
        if (parent != null) {
            candidates.add(parent);
        }
        candidates.add(cwd.resolve("..").normalize());
        candidates.add(cwd.resolve("../..").normalize());

        for (Path candidate : candidates) {
            if (isDeployScriptsDir(candidate.resolve("deploy").resolve("scripts"))) {
                return candidate;
            }
        }
        throw new BusinessException(500, "未找到项目根目录（需包含 deploy/scripts）");
    }

    private boolean isDeployScriptsDir(Path scriptsDir) {
        if (resolveMode() == DeployRunnerMode.LOCAL && !runAsUser.isBlank()) {
            return runUserTest("-d", scriptsDir.toString());
        }
        return Files.isDirectory(scriptsDir);
    }

    private boolean isDeployScript(Path script) {
        if (resolveMode() == DeployRunnerMode.LOCAL && !runAsUser.isBlank()) {
            return runUserTest("-f", script.toString());
        }
        return Files.isRegularFile(script);
    }

    private boolean runUserTest(String flag, String path) {
        try {
            ProcessBuilder builder = new ProcessBuilder("sudo", "-n", "-u", runAsUser, "test", flag, path);
            applyProcessEnvironment(builder);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.getInputStream().readAllBytes();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ex) {
            log.warn("runUserTest failed for {} {}: {}", flag, path, ex.getMessage());
            return false;
        }
    }

    private List<String> buildLocalScriptCommand(Path projectRoot, DeployTarget target) {
        Path scriptsDir = projectRoot.resolve("deploy").resolve("scripts");
        Path script = target == DeployTarget.BACKEND
                ? scriptsDir.resolve("deploy-on-pi-backend.sh")
                : scriptsDir.resolve("deploy-on-pi-frontend.sh");
        if (!isDeployScript(script)) {
            throw new BusinessException(500, "本机部署脚本不存在: " + script);
        }
        return wrapRunAsUser("bash", script.toString());
    }

    private List<String> buildRemoteScriptCommand(Path projectRoot, DeployTarget target) {
        Path scriptsDir = projectRoot.resolve("deploy").resolve("scripts");
        if (isWindows()) {
            Path script = target == DeployTarget.BACKEND
                    ? scriptsDir.resolve("deploy-backend.ps1")
                    : scriptsDir.resolve("deploy-frontend.ps1");
            if (!Files.isRegularFile(script)) {
                throw new BusinessException(500, "部署脚本不存在: " + script);
            }
            return List.of(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    script.toString());
        }

        Path script = target == DeployTarget.BACKEND
                ? scriptsDir.resolve("deploy-backend-to-pi.sh")
                : scriptsDir.resolve("deploy-frontend-to-pi.sh");
        if (!Files.isRegularFile(script)) {
            throw new BusinessException(500, "部署脚本不存在: " + script);
        }
        return List.of("bash", script.toString());
    }

    private List<String> wrapRunAsUser(String... command) {
        if (runAsUser.isBlank()) {
            return List.of(command);
        }
        String currentUser = firstNonBlank(System.getenv("USER"), System.getProperty("user.name"));
        if (runAsUser.equalsIgnoreCase(currentUser)) {
            return List.of(command);
        }
        List<String> wrapped = new ArrayList<>();
        wrapped.add("sudo");
        wrapped.add("-n");
        wrapped.add("-u");
        wrapped.add(runAsUser);
        wrapped.addAll(List.of(command));
        return wrapped;
    }

    private void runPasswordDeploy(
            SseEmitter emitter,
            Path projectRoot,
            DeployTarget target,
            Runnable releaseLock) {
        try {
            sendEvent(emitter, "log", "工作目录: " + projectRoot);
            sendEvent(emitter, "log", "SSH 目标: " + piSshClient.targetLabel() + "（密码登录）");
            sendEvent(emitter, "log", "==> 检查 SSH 连接...");
            piSshClient.testConnection();
            sendEvent(emitter, "log", "SSH 检查通过");

            if (target == DeployTarget.BACKEND) {
                deployBackendWithPassword(emitter, projectRoot);
            } else {
                deployFrontendWithPassword(emitter, projectRoot);
            }

            sendEvent(emitter, "done", "{\"success\":true,\"exitCode\":0}");
            emitter.complete();
        } catch (Exception ex) {
            log.error("Password deploy failed", ex);
            try {
                sendEvent(emitter, "log", "错误: " + ex.getMessage());
                sendEvent(emitter, "done", "{\"success\":false,\"exitCode\":-1}");
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ex);
            }
        } finally {
            releaseLock.run();
        }
    }

    private void deployBackendWithPassword(SseEmitter emitter, Path projectRoot) throws Exception {
        sendEvent(emitter, "log", "==> 构建后端...");
        int buildCode = runCommand(
                emitter,
                projectRoot.resolve("admin-backend"),
                commandLine("mvn", "clean", "package", "-DskipTests", "-pl", "admin-server", "-am"));
        if (buildCode != 0) {
            throw new BusinessException(500, "Maven 构建失败，退出码 " + buildCode);
        }

        Path jar = projectRoot
                .resolve("admin-backend")
                .resolve("admin-server")
                .resolve("target")
                .resolve("admin-server-1.0.0-SNAPSHOT.jar");
        if (!Files.isRegularFile(jar)) {
            throw new BusinessException(500, "未找到 JAR: " + jar);
        }

        long jarSizeMb = Math.max(1, Files.size(jar) / (1024 * 1024));
        sendEvent(emitter, "log", "==> 上传 JAR 到 /tmp（约 " + jarSizeMb + " MB），请稍候...");
        piSshClient.uploadAndInstallBackendJar(jar, backendDir);
        sendEvent(emitter, "log", "JAR 已安装到 " + backendDir);

        sendEvent(emitter, "log", "==> 重启服务...");
        String status = piSshClient.execute(
                "sudo systemctl restart ai-manager-backend && sudo systemctl status ai-manager-backend --no-pager");
        if (!status.isBlank()) {
            sendEvent(emitter, "log", status);
        }
        sendEvent(emitter, "log", "完成。健康检查：curl http://" + hostOnly() + "/api/health");
    }

    private void deployFrontendWithPassword(SseEmitter emitter, Path projectRoot) throws Exception {
        Path webDir = projectRoot.resolve("admin-web");
        sendEvent(emitter, "log", "==> 构建前端...");
        int installCode = runCommand(emitter, webDir, commandLine("npm", "install"));
        if (installCode != 0) {
            throw new BusinessException(500, "npm install 失败，退出码 " + installCode);
        }
        int buildCode = runCommand(emitter, webDir, commandLine("npm", "run", "build"));
        if (buildCode != 0) {
            throw new BusinessException(500, "npm run build 失败，退出码 " + buildCode);
        }

        Path distDir = webDir.resolve("dist");
        if (!Files.isDirectory(distDir)) {
            throw new BusinessException(500, "未找到 dist 目录: " + distDir);
        }

        sendEvent(emitter, "log", "==> 上传到 " + piSshClient.targetLabel() + " ...");
        long fileCount;
        try (Stream<Path> walk = Files.walk(distDir)) {
            fileCount = walk.filter(Files::isRegularFile).count();
        }
        sendEvent(emitter, "log", "==> 传输 dist 文件 " + fileCount + " 个（无进度条，请稍候）...");
        piSshClient.uploadDirectory(distDir, "/tmp/ai-manager-new");
        sendEvent(emitter, "log", "静态文件上传完成");

        sendEvent(emitter, "log", "==> 安装到 Nginx 目录...");
        String output = piSshClient.execute(
                "sudo rsync -av --delete /tmp/ai-manager-new/ "
                        + DeployPiSshClient.shellQuote(webRoot)
                        + "/ && sudo chown -R www-data:www-data "
                        + DeployPiSshClient.shellQuote(webRoot));
        if (!output.isBlank()) {
            sendEvent(emitter, "log", output);
        }
        sendEvent(emitter, "log", "完成。访问 http://" + hostOnly() + "/#/home");
    }

    private String hostOnly() {
        String target = piSshClient.targetLabel();
        int index = target.indexOf('@');
        return index >= 0 ? target.substring(index + 1) : target;
    }

    private List<String> commandLine(String... parts) {
        if (!isWindows()) {
            return List.of(parts);
        }
        List<String> command = new ArrayList<>();
        command.add("cmd");
        command.add("/c");
        command.add(String.join(" ", parts));
        return command;
    }

    private int runCommand(SseEmitter emitter, Path workDir, List<String> command) throws Exception {
        sendEvent(emitter, "log", "$ " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        applyProcessEnvironment(builder);
        Process process = builder.start();

        Charset charset = resolveConsoleCharset();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sendEvent(emitter, "log", line);
            }
        }
        return process.waitFor();
    }

    private void runScriptDeploy(
            SseEmitter emitter,
            Path projectRoot,
            List<String> command,
            Runnable releaseLock,
            boolean localMode) {
        try {
            sendEvent(emitter, "log", "执行命令: " + String.join(" ", command));
            sendEvent(emitter, "log", "工作目录: " + projectRoot);
            if (localMode) {
                sendEvent(emitter, "log", "部署模式: 本机（114）");
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(projectRoot.toFile());
            builder.redirectErrorStream(true);
            applyProcessEnvironment(builder);
            applyDeployEnvironment(builder, localMode);
            Process process = builder.start();

            Charset charset = resolveConsoleCharset();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sendEvent(emitter, "log", line);
                }
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            sendEvent(emitter, "done", "{\"success\":" + success + ",\"exitCode\":" + exitCode + "}");
            emitter.complete();
        } catch (Exception ex) {
            log.error("Deploy run failed", ex);
            try {
                sendEvent(emitter, "log", "错误: " + ex.getMessage());
                sendEvent(emitter, "done", "{\"success\":false,\"exitCode\":-1}");
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ex);
            }
        } finally {
            releaseLock.run();
        }
    }

    private void applyDeployEnvironment(ProcessBuilder builder, boolean localMode) {
        if (!localMode) {
            return;
        }
        Map<String, String> env = builder.environment();
        env.put("GIT_PULL", gitPullEnabled ? "true" : "false");
        env.put("BACKEND_DIR", backendDir);
        env.put("WEB_ROOT", webRoot);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) throws Exception {
        emitter.send(SseEmitter.event().name(name).data(data));
    }

    private void applyProcessEnvironment(ProcessBuilder builder) {
        Map<String, String> env = builder.environment();
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            env.putIfAbsent("USERPROFILE", userProfile);
            env.putIfAbsent("HOME", userProfile);
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            env.putIfAbsent("HOME", userHome);
        }
        String path = System.getenv("PATH");
        if (path != null) {
            env.putIfAbsent("PATH", path);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Charset resolveConsoleCharset() {
        if (isWindows()) {
            return Charset.forName("GBK");
        }
        return StandardCharsets.UTF_8;
    }
}
