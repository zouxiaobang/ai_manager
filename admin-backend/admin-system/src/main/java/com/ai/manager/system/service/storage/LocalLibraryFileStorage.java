package com.ai.manager.system.service.storage;

import com.ai.manager.system.config.LibraryStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalLibraryFileStorage implements LibraryFileStorage {

    private final LibraryStorageProperties properties;

    @Override
    public String type() {
        return "LOCAL";
    }

    @Override
    public String save(String folder, String fileName, byte[] content, String extension) {
        LocalDate today = LocalDate.now();
        String datePath = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String relativePath = datePath + "/" + uuid + "." + extension;
        Path fullPath = Paths.get(properties.getLocalRoot(), folder, relativePath);
        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + fullPath, e);
        }
        return folder + "/" + relativePath;
    }

    @Override
    public byte[] load(String storagePath) {
        Path fullPath = Paths.get(properties.getLocalRoot(), storagePath);
        try {
            return Files.readAllBytes(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + fullPath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path fullPath = Paths.get(properties.getLocalRoot(), storagePath);
        try {
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + fullPath, e);
        }
    }

    @Override
    @PostConstruct
    public void ensureRoot() {
        try {
            Files.createDirectories(Paths.get(properties.getLocalRoot()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create library storage root: " + properties.getLocalRoot(), e);
        }
    }
}
