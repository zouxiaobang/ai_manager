package com.ai.manager.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class DeployStatusService {

    private final Instant applicationStartedAt = Instant.now();
    private final String backendJarPath;
    private final String frontendIndexPath;
    private final String deployMarkerPath;

    public DeployStatusService(
            @Value("${ai-manager.deploy.backend-jar-path:/opt/ai-manager/backend/admin-server.jar}") String backendJarPath,
            @Value("${ai-manager.deploy.frontend-index-path:/var/www/ai-manager/index.html}") String frontendIndexPath,
            @Value("${ai-manager.deploy.marker-path:}") String deployMarkerPath) {
        this.backendJarPath = backendJarPath;
        this.frontendIndexPath = frontendIndexPath;
        this.deployMarkerPath = deployMarkerPath.isBlank()
                ? defaultMarkerPath(backendJarPath)
                : deployMarkerPath;
    }

    public Instant applicationStartedAt() {
        return applicationStartedAt;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void touchDeployMarker() {
        try {
            Path marker = Path.of(deployMarkerPath);
            Path parent = marker.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(marker, applicationStartedAt.toString());
        } catch (Exception ignored) {
            // 本地开发或目录不可写时忽略
        }
    }

    public Optional<Instant> resolveLastDeployAt() {
        return candidateDeployTimes().stream().max(Instant::compareTo);
    }

    private List<Instant> candidateDeployTimes() {
        List<Instant> candidates = new ArrayList<>();
        candidates.add(applicationStartedAt);

        readLastModified(backendJarPath).ifPresent(candidates::add);
        readLastModified(frontendIndexPath).ifPresent(candidates::add);
        readLastModified(deployMarkerPath).ifPresent(candidates::add);
        resolveRunningJarMtime().ifPresent(candidates::add);

        for (String devJar : devJarCandidates()) {
            readLastModified(devJar).ifPresent(candidates::add);
        }

        return candidates;
    }

    private static String defaultMarkerPath(String backendJarPath) {
        Path parent = Path.of(backendJarPath).getParent();
        if (parent == null) {
            return ".ai-manager-last-deploy";
        }
        return parent.resolve(".last-deploy-at").toString();
    }

    private Optional<Instant> resolveRunningJarMtime() {
        try {
            URL location = DeployStatusService.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return Optional.empty();
            }
            Path path = Paths.get(location.toURI());
            return readLastModified(path);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private List<String> devJarCandidates() {
        String userDir = System.getProperty("user.dir", ".");
        Path cwd = Path.of(userDir).toAbsolutePath().normalize();
        return Stream.of(
                        cwd.resolve("admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"),
                        cwd.resolve("admin-backend/admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"),
                        cwd.resolve("target/admin-server-1.0.0-SNAPSHOT.jar"),
                        cwd.resolve("../admin-backend/admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"))
                .map(Path::toString)
                .toList();
    }

    private Optional<Instant> readLastModified(String pathText) {
        if (pathText == null || pathText.isBlank()) {
            return Optional.empty();
        }
        try {
            Path path = Path.of(pathText).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                return Optional.empty();
            }
            return Optional.of(Files.getLastModifiedTime(path).toInstant());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Instant> readLastModified(Path path) {
        if (path == null) {
            return Optional.empty();
        }
        return readLastModified(path.toString());
    }
}
