package com.ai.manager.system.service.storage;

import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.domain.storage.NoteContentRef;
import com.ai.manager.system.domain.storage.NoteContentSaveResult;
import com.ai.manager.system.service.BaiduPanAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BaiduPanNoteContentStorage implements NoteContentStorage {

    public static final String TYPE = "BAIDU_PAN";

    private volatile boolean rootEnsured;

    private final BaiduPanClient baiduPanClient;
    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanProperties baiduPanProperties;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public NoteContentSaveResult save(NoteContentRef ref, String content) {
        String accessToken = baiduPanAuthService.requireAccessToken();
        String path = resolvePath(ref);
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        try {
            ensureRoot();
            BaiduPanClient.BaiduUploadResponse response = baiduPanClient.upload(accessToken, path, bytes);
            return NoteContentSaveResult.builder()
                    .storagePath(path)
                    .storageFsId(response.getFsId())
                    .contentSize(bytes.length)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("上传笔记到百度网盘失败: " + path, e);
        }
    }

    @Override
    public String load(NoteContentRef ref) {
        String accessToken = baiduPanAuthService.requireAccessToken();
        String path = resolvePath(ref);
        try {
            return baiduPanClient.download(accessToken, path, ref.getStorageFsId());
        } catch (Exception e) {
            throw new IllegalStateException("从百度网盘读取笔记失败: " + path, e);
        }
    }

    @Override
    public void delete(NoteContentRef ref) {
        String accessToken = baiduPanAuthService.requireAccessToken();
        String path = resolvePath(ref);
        try {
            baiduPanClient.delete(accessToken, path);
        } catch (Exception e) {
            throw new IllegalStateException("删除百度网盘笔记失败: " + path, e);
        }
    }

    @Override
    public void ensureRoot() {
        if (rootEnsured) {
            return;
        }
        synchronized (this) {
            if (rootEnsured) {
                return;
            }
            String accessToken = baiduPanAuthService.requireAccessToken();
            try {
                baiduPanClient.ensureDir(accessToken, baiduPanProperties.rootPath());
                baiduPanClient.ensureDir(accessToken, baiduPanProperties.notesDir());
                baiduPanClient.ensureDir(accessToken, baiduPanProperties.trashDir());
                baiduPanClient.ensureDir(accessToken, baiduPanProperties.imagesDir());
                baiduPanClient.ensureDir(accessToken, baiduPanProperties.ecommerceImagesDir());
                rootEnsured = true;
            } catch (Exception e) {
                throw new IllegalStateException("初始化百度网盘目录失败", e);
            }
        }
    }

    public String toStoragePath(Long noteId) {
        return baiduPanProperties.notesDir() + "/" + noteId + ".html";
    }

    private String resolvePath(NoteContentRef ref) {
        if (ref.getStoragePath() != null && !ref.getStoragePath().isBlank()) {
            return ref.getStoragePath();
        }
        return toStoragePath(ref.getNoteId());
    }
}
