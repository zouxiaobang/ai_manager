package com.ai.manager.system.service.storage;

import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class LocalFileNoteContentStorage implements NoteContentStorage {

    public static final String TYPE = "LOCAL";

    private final NoteStorageProperties noteStorageProperties;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public NoteContentSaveResult save(NoteContentRef ref, String content) {
        Path file = resolvePath(ref.getStoragePath(), ref.getNoteId());
        try {
            Files.createDirectories(file.getParent());
            byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
            Files.write(file, bytes);
            return NoteContentSaveResult.builder()
                    .storagePath(toStoragePath(ref.getNoteId()))
                    .storageFsId(null)
                    .contentSize(bytes.length)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("保存笔记正文到本地失败: " + file, e);
        }
    }

    @Override
    public String load(NoteContentRef ref) {
        Path file = resolvePath(ref.getStoragePath(), ref.getNoteId());
        if (!Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取本地笔记正文失败: " + file, e);
        }
    }

    @Override
    public void delete(NoteContentRef ref) {
        Path file = resolvePath(ref.getStoragePath(), ref.getNoteId());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("删除本地笔记正文失败: " + file, e);
        }
    }

    @Override
    public void ensureRoot() {
        try {
            Files.createDirectories(Paths.get(noteStorageProperties.getLocalRoot()));
        } catch (IOException e) {
            throw new IllegalStateException("创建本地笔记存储目录失败", e);
        }
    }

    public String toStoragePath(Long noteId) {
        return "notes/" + noteId + ".html";
    }

    private Path resolvePath(String storagePath, Long noteId) {
        String relative = StringUtils.hasText(storagePath) ? storagePath : toStoragePath(noteId);
        return Paths.get(noteStorageProperties.getLocalRoot()).resolve(relative).normalize();
    }
}
