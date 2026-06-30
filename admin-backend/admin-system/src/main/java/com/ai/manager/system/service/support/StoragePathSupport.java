package com.ai.manager.system.service.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public final class StoragePathSupport {

    private StoragePathSupport() {
    }

    public static Path resolveUploadBasePath(String configuredPath) {
        Path configured = Paths.get(configuredPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if ("admin-server".equals(userDir.getFileName().toString())) {
            Path parent = userDir.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent.resolve(configuredPath).normalize();
            }
        }
        return userDir.resolve(configuredPath).normalize();
    }

    public static long directorySize(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException ex) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }

    public static long countRegularFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }
}
